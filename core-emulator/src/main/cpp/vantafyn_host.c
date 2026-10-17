// SPDX-License-Identifier: MIT
// Vantafyn Native Retro Core Engine
// Clean-room C11 implementation of the libretro frontend host.

#include "vantafyn_host.h"
#include "libretro.h"

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <android/log.h>
#include <android/native_window.h>
#include <dirent.h>
#include <errno.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#define LOG_TAG "VantafynNativeHost"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define AUDIO_RING_FRAMES 16384 // ~340ms at 48kHz stereo

typedef struct {
    int16_t *buffer;
    size_t capacity; // in sample pairs (frames)
    atomic_size_t read_idx;
    atomic_size_t write_idx;
} audio_ring_t;

#define VF_MAX_OPTIONS 128

typedef struct {
    char key[64];
    char value[64];
} vf_option_t;

struct vf_session {
    void *core_handle;
    
    // Core function pointers
    void (*retro_init)(void);
    void (*retro_deinit)(void);
    unsigned (*retro_api_version)(void);
    void (*retro_get_system_info)(struct retro_system_info *info);
    void (*retro_get_system_av_info)(struct retro_system_av_info *info);
    void (*retro_set_environment)(retro_environment_t);
    void (*retro_set_video_refresh)(retro_video_refresh_t);
    void (*retro_set_audio_sample)(retro_audio_sample_t);
    void (*retro_set_audio_sample_batch)(retro_audio_sample_batch_t);
    void (*retro_set_input_poll)(retro_input_poll_t);
    void (*retro_set_input_state)(retro_input_state_t);
    void (*retro_set_controller_port_device)(unsigned port, unsigned device);
    void (*retro_reset)(void);
    void (*retro_run)(void);
    size_t (*retro_serialize_size)(void);
    bool (*retro_serialize)(void *data, size_t size);
    bool (*retro_unserialize)(const void *data, size_t size);
    bool (*retro_load_game)(const struct retro_game_info *game);
    void (*retro_unload_game)(void);
    void *(*retro_get_memory_data)(unsigned id);
    size_t (*retro_get_memory_size)(unsigned id);

    // Callbacks & User State
    vf_callbacks_t callbacks;
    vf_av_info_t av_info;
    enum retro_pixel_format pixel_format;
    
    char system_directory[PATH_MAX];
    char save_directory[PATH_MAX];

    // ROM buffer persistence (MUST NOT be freed during gameplay)
    void *rom_data;
    size_t rom_size;

    // Threading and execution control
    pthread_t thread;
    atomic_bool is_thread_running;
    atomic_bool is_paused;
    atomic_bool is_active;
    atomic_bool reset_requested;
    int fast_forward_ratio;

    // Window Rendering
    pthread_mutex_t window_mutex;
    ANativeWindow *window;
    ANativeWindow *window_secondary;
    bool swap_dual_screens;

    // Hardware Rendering (OpenGL ES / EGL)
    bool is_hw_render;
    struct retro_hw_render_callback hw_render;
    bool hw_context_reset_done;
    EGLDisplay egl_display;
    EGLConfig egl_config;
    EGLContext egl_context;
    EGLSurface egl_pbuffer_surface;
    EGLSurface egl_window_surface;
    EGLSurface egl_surface_secondary;
    EGLSurface egl_surface;
    ANativeWindow *active_egl_window;
    ANativeWindow *active_egl_window_secondary;
    GLuint hw_fbo;
    GLuint hw_texture;
    GLuint hw_depth_rb;
    int hw_fbo_w;
    int hw_fbo_h;
    
    // Framebuffer conversion cache
    uint32_t *frame_buffer;
    size_t frame_buffer_size;
    int frame_width;
    int frame_height;
    size_t frame_pitch;

    // Audio Ring Buffer
    audio_ring_t audio_ring;

    // Input state
    _Atomic uint32_t input_masks[VF_MAX_PORTS];
    _Atomic int16_t analog_state[VF_MAX_PORTS][2][2];
    _Atomic int16_t touch_x;
    _Atomic int16_t touch_y;
    _Atomic bool touch_pressed;

    // Visual Enhancements & LCD Simulation
    bool gba_color_correction;
    bool gbc_color_correction;
    int gb_palette_mode; // 0 = colorized/auto, 1 = DMG Pea Soup Green, 2 = Pocket B&W
    bool lcd_ghosting;
    uint32_t *prev_frame_buffer;
    size_t prev_frame_buffer_size;

    bool audio_filtering;
    int32_t audio_filter_prev_l;
    int32_t audio_filter_prev_r;

    // Video Filtering & Scaling
    bool crisp_pixels;
    int last_buffer_w;
    int last_buffer_h;
    int last_buffer_secondary_w;
    int last_buffer_secondary_h;
    uint32_t *lut_x;
    int lut_x_capacity;
    int lut_x_src_w;
    int lut_x_dst_w;
    uint32_t *lut_y;
    int lut_y_capacity;
    int lut_y_src_h;
    int lut_y_dst_h;

    // Dynamic Core Options
    vf_option_t options[VF_MAX_OPTIONS];
    int option_count;
    atomic_bool options_updated;
};

// Thread-local or global reference for libretro C callbacks
static vf_session_t *s_current_session = NULL;
static pthread_mutex_t s_session_lock = PTHREAD_MUTEX_INITIALIZER;

// ---------------------------------------------------------------------------
// Audio Ring Buffer Helpers
// ---------------------------------------------------------------------------

static void ring_init(audio_ring_t *ring, size_t capacity) {
    ring->capacity = capacity;
    ring->buffer = (int16_t *)calloc(capacity * 2, sizeof(int16_t));
    atomic_store(&ring->read_idx, 0);
    atomic_store(&ring->write_idx, 0);
}

static void ring_free(audio_ring_t *ring) {
    if (ring->buffer) {
        free(ring->buffer);
        ring->buffer = NULL;
    }
}

static size_t ring_occupancy(const audio_ring_t *ring) {
    if (!ring || !ring->buffer) return 0;
    size_t r = atomic_load(&ring->read_idx);
    size_t w = atomic_load(&ring->write_idx);
    size_t cap = ring->capacity;
    if (w >= r) return w - r;
    return cap - (r - w);
}

static void ring_write_samples(audio_ring_t *ring, const int16_t *data, size_t frames, bool filter, int32_t *prev_l, int32_t *prev_r) {
    if (!ring->buffer || frames == 0) return;
    size_t r = atomic_load(&ring->read_idx);
    size_t w = atomic_load(&ring->write_idx);
    size_t cap = ring->capacity;

    int32_t fl = prev_l ? *prev_l : 0;
    int32_t fr = prev_r ? *prev_r : 0;

    for (size_t i = 0; i < frames; i++) {
        size_t next_w = (w + 1) % cap;
        if (next_w == r) {
            // Buffer overflow - advance read pointer to drop oldest sample
            r = (r + 1) % cap;
            atomic_store(&ring->read_idx, r);
        }
        if (filter) {
            fl = (180 * (int32_t)data[i * 2] + 76 * fl) >> 8;
            fr = (180 * (int32_t)data[i * 2 + 1] + 76 * fr) >> 8;
            ring->buffer[w * 2] = (int16_t)fl;
            ring->buffer[w * 2 + 1] = (int16_t)fr;
        } else {
            ring->buffer[w * 2] = data[i * 2];
            ring->buffer[w * 2 + 1] = data[i * 2 + 1];
        }
        w = next_w;
    }
    if (prev_l) *prev_l = fl;
    if (prev_r) *prev_r = fr;
    atomic_store(&ring->write_idx, w);
}

static size_t ring_read_samples(audio_ring_t *ring, int16_t *out, size_t max_frames) {
    if (!ring->buffer || max_frames == 0) return 0;
    size_t r = atomic_load(&ring->read_idx);
    size_t w = atomic_load(&ring->write_idx);
    size_t cap = ring->capacity;
    size_t frames_read = 0;

    while (r != w && frames_read < max_frames) {
        out[frames_read * 2] = ring->buffer[r * 2];
        out[frames_read * 2 + 1] = ring->buffer[r * 2 + 1];
        r = (r + 1) % cap;
        frames_read++;
    }
    atomic_store(&ring->read_idx, r);
    return frames_read;
}

// ---------------------------------------------------------------------------
// Libretro Host Environment Callbacks
// ---------------------------------------------------------------------------

static void core_log_printf(enum retro_log_level level, const char *fmt, ...) {
    if (level == RETRO_LOG_DEBUG) {
        return; // Suppress extreme debug noise (e.g. Citra camera/audio Y2R service spam)
    }
    va_list va;
    va_start(va, fmt);
    android_LogPriority prio = ANDROID_LOG_INFO;
    switch (level) {
        case RETRO_LOG_DEBUG: prio = ANDROID_LOG_DEBUG; break;
        case RETRO_LOG_INFO: prio = ANDROID_LOG_INFO; break;
        case RETRO_LOG_WARN: prio = ANDROID_LOG_WARN; break;
        case RETRO_LOG_ERROR: prio = ANDROID_LOG_ERROR; break;
        default: prio = ANDROID_LOG_INFO; break;
    }
    __android_log_vprint(prio, "VantafynCore", fmt, va);
    va_end(va);
}

// ---------------------------------------------------------------------------
// Persistent OpenGL ES Program Binary Disk Cache & Deferred Shader Compiler
// ---------------------------------------------------------------------------

#define VF_GL_HASH_BUCKETS 4096

typedef struct vf_shader_node {
    GLuint shader;
    GLenum type;
    uint64_t hash_lo;
    uint64_t hash_hi;
    bool has_source;
    bool compile_requested;
    bool compiled_on_gpu;
    struct vf_shader_node *next;
} vf_shader_node_t;

typedef struct vf_program_node {
    GLuint program;
    GLuint attached_shaders[8];
    int attached_count;
    struct vf_program_node *next;
} vf_program_node_t;

static vf_shader_node_t *s_shader_buckets[VF_GL_HASH_BUCKETS] = {0};
static vf_program_node_t *s_program_buckets[VF_GL_HASH_BUCKETS] = {0};
static pthread_mutex_t s_gl_cache_lock = PTHREAD_MUTEX_INITIALIZER;
static uint64_t s_gl_driver_hash = 0;

static inline void vf_hash_bytes(const char *data, size_t len, uint64_t *h1, uint64_t *h2) {
    uint64_t a = *h1;
    uint64_t b = *h2;
    for (size_t i = 0; i < len; i++) {
        uint8_t c = (uint8_t)data[i];
        a ^= c;
        a *= 0x100000001b3ULL;
        b = ((b << 5) + b) + c;
    }
    *h1 = a;
    *h2 = b;
}

static void GL_APIENTRY vf_glShaderSource(GLuint shader, GLsizei count, const GLchar *const*string, const GLint *length) {
    glShaderSource(shader, count, string, length);
    if (!string || count <= 0) return;

    uint64_t h1 = 0xcbf29ce484222325ULL;
    uint64_t h2 = 5381ULL;
    for (GLsizei i = 0; i < count; i++) {
        if (!string[i]) continue;
        size_t len = (length && length[i] >= 0) ? (size_t)length[i] : strlen(string[i]);
        vf_hash_bytes(string[i], len, &h1, &h2);
    }

    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(shader % VF_GL_HASH_BUCKETS);
    vf_shader_node_t *n = s_shader_buckets[b];
    while (n && n->shader != shader) n = n->next;
    if (!n) {
        n = (vf_shader_node_t *)calloc(1, sizeof(vf_shader_node_t));
        n->shader = shader;
        n->next = s_shader_buckets[b];
        s_shader_buckets[b] = n;
    }
    n->hash_lo = h1;
    n->hash_hi = h2;
    n->has_source = true;
    n->compile_requested = false;
    n->compiled_on_gpu = false;
    pthread_mutex_unlock(&s_gl_cache_lock);
}

static void GL_APIENTRY vf_glCompileShader(GLuint shader) {
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(shader % VF_GL_HASH_BUCKETS);
    vf_shader_node_t *n = s_shader_buckets[b];
    while (n && n->shader != shader) n = n->next;
    if (n && n->has_source) {
        n->compile_requested = true;
        pthread_mutex_unlock(&s_gl_cache_lock);
        return; // Deferred until link time or program cache lookup!
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
    glCompileShader(shader);
}

static void GL_APIENTRY vf_glGetShaderiv(GLuint shader, GLenum pname, GLint *params) {
    if (!params) return;
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(shader % VF_GL_HASH_BUCKETS);
    vf_shader_node_t *n = s_shader_buckets[b];
    while (n && n->shader != shader) n = n->next;
    if (n && n->compile_requested && !n->compiled_on_gpu) {
        if (pname == GL_COMPILE_STATUS) {
            *params = GL_TRUE;
            pthread_mutex_unlock(&s_gl_cache_lock);
            return;
        }
        if (pname == GL_INFO_LOG_LENGTH) {
            *params = 0;
            pthread_mutex_unlock(&s_gl_cache_lock);
            return;
        }
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
    glGetShaderiv(shader, pname, params);
}

static void GL_APIENTRY vf_glGetShaderInfoLog(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog) {
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(shader % VF_GL_HASH_BUCKETS);
    vf_shader_node_t *n = s_shader_buckets[b];
    while (n && n->shader != shader) n = n->next;
    if (n && n->compile_requested && !n->compiled_on_gpu) {
        if (length) *length = 0;
        if (infoLog && bufSize > 0) infoLog[0] = '\0';
        pthread_mutex_unlock(&s_gl_cache_lock);
        return;
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
    glGetShaderInfoLog(shader, bufSize, length, infoLog);
}

static void GL_APIENTRY vf_glDeleteShader(GLuint shader) {
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(shader % VF_GL_HASH_BUCKETS);
    vf_shader_node_t **curr = &s_shader_buckets[b];
    while (*curr) {
        if ((*curr)->shader == shader) {
            vf_shader_node_t *to_free = *curr;
            *curr = to_free->next;
            free(to_free);
            break;
        }
        curr = &(*curr)->next;
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
    glDeleteShader(shader);
}

static void GL_APIENTRY vf_glAttachShader(GLuint program, GLuint shader) {
    glAttachShader(program, shader);
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(program % VF_GL_HASH_BUCKETS);
    vf_program_node_t *p = s_program_buckets[b];
    while (p && p->program != program) p = p->next;
    if (!p) {
        p = (vf_program_node_t *)calloc(1, sizeof(vf_program_node_t));
        p->program = program;
        p->next = s_program_buckets[b];
        s_program_buckets[b] = p;
    }
    if (p->attached_count < 8) {
        p->attached_shaders[p->attached_count++] = shader;
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
}

static void GL_APIENTRY vf_glDetachShader(GLuint program, GLuint shader) {
    glDetachShader(program, shader);
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(program % VF_GL_HASH_BUCKETS);
    vf_program_node_t *p = s_program_buckets[b];
    while (p && p->program != program) p = p->next;
    if (p) {
        for (int i = 0; i < p->attached_count; i++) {
            if (p->attached_shaders[i] == shader) {
                p->attached_shaders[i] = p->attached_shaders[p->attached_count - 1];
                p->attached_count--;
                break;
            }
        }
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
}

static void GL_APIENTRY vf_glDeleteProgram(GLuint program) {
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(program % VF_GL_HASH_BUCKETS);
    vf_program_node_t **curr = &s_program_buckets[b];
    while (*curr) {
        if ((*curr)->program == program) {
            vf_program_node_t *to_free = *curr;
            *curr = to_free->next;
            free(to_free);
            break;
        }
        curr = &(*curr)->next;
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
    glDeleteProgram(program);
}

static void GL_APIENTRY vf_glLinkProgram(GLuint program) {
    pthread_mutex_lock(&s_gl_cache_lock);
    unsigned int b = (unsigned int)(program % VF_GL_HASH_BUCKETS);
    vf_program_node_t *p = s_program_buckets[b];
    while (p && p->program != program) p = p->next;

    bool can_cache = false;
    uint64_t prog_key_lo = 0xcbf29ce484222325ULL;
    uint64_t prog_key_hi = 5381ULL ^ s_gl_driver_hash;
    char cache_path[PATH_MAX] = {0};

    vf_session_t *s = s_current_session;

    if (p && p->attached_count > 0 && s && s->save_directory[0]) {
        can_cache = true;
        for (int i = 0; i < p->attached_count; i++) {
            GLuint sh = p->attached_shaders[i];
            unsigned int sb = (unsigned int)(sh % VF_GL_HASH_BUCKETS);
            vf_shader_node_t *sn = s_shader_buckets[sb];
            while (sn && sn->shader != sh) sn = sn->next;
            if (!sn || !sn->has_source) {
                can_cache = false;
                break;
            }
            prog_key_lo ^= sn->hash_lo;
            prog_key_lo *= 0x100000001b3ULL;
            prog_key_hi ^= sn->hash_hi;
            prog_key_hi = ((prog_key_hi << 5) + prog_key_hi) + (uint64_t)sn->type;
        }
    }

    if (can_cache) {
        snprintf(cache_path, sizeof(cache_path) - 1, "%s/Azahar/shaders/vf_bin_cache/%016llx_%016llx.bin",
                 s->save_directory, (unsigned long long)prog_key_hi, (unsigned long long)prog_key_lo);

        FILE *f = fopen(cache_path, "rb");
        if (f) {
            uint32_t magic = 0, ver = 0, bin_format = 0, bin_size = 0;
            if (fread(&magic, 4, 1, f) == 1 && magic == 0x56465042 && // 'VFPB'
                fread(&ver, 4, 1, f) == 1 && ver == 1 &&
                fread(&bin_format, 4, 1, f) == 1 &&
                fread(&bin_size, 4, 1, f) == 1 && bin_size > 0 && bin_size < 16777216) {
                void *buf = malloc(bin_size);
                if (buf && fread(buf, 1, bin_size, f) == bin_size) {
                    glProgramParameteri(program, GL_PROGRAM_BINARY_RETRIEVABLE_HINT, GL_TRUE);
                    glProgramBinary(program, (GLenum)bin_format, buf, (GLsizei)bin_size);
                    GLint link_ok = GL_FALSE;
                    glGetProgramiv(program, GL_LINK_STATUS, &link_ok);
                    if (link_ok == GL_TRUE) {
                        free(buf);
                        fclose(f);
                        pthread_mutex_unlock(&s_gl_cache_lock);
                        return; // Program binary loaded in <0.1ms without driver compile or link!
                    }
                }
                free(buf);
            }
            fclose(f);
            unlink(cache_path); // Rejected by GPU driver; will recompile
        }
    }

    // Cache miss: compile any deferred attached shaders on the GPU driver now
    if (p) {
        for (int i = 0; i < p->attached_count; i++) {
            GLuint sh = p->attached_shaders[i];
            unsigned int sb = (unsigned int)(sh % VF_GL_HASH_BUCKETS);
            vf_shader_node_t *sn = s_shader_buckets[sb];
            while (sn && sn->shader != sh) sn = sn->next;
            if (sn && !sn->compiled_on_gpu) {
                glCompileShader(sh);
                sn->compiled_on_gpu = true;
            }
        }
    }
    pthread_mutex_unlock(&s_gl_cache_lock);

    glProgramParameteri(program, GL_PROGRAM_BINARY_RETRIEVABLE_HINT, GL_TRUE);
    glLinkProgram(program);

    GLint link_ok = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &link_ok);
    if (link_ok == GL_TRUE && can_cache && cache_path[0]) {
        GLint bin_len = 0;
        glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH, &bin_len);
        if (bin_len > 0 && bin_len < 16777216) {
            void *buf = malloc(bin_len);
            GLsizei actual_len = 0;
            GLenum bin_format = 0;
            if (buf) {
                glGetProgramBinary(program, bin_len, &actual_len, &bin_format, buf);
                if (actual_len > 0) {
                    char tmp_path[PATH_MAX];
                    snprintf(tmp_path, sizeof(tmp_path) - 1, "%s.tmp", cache_path);
                    FILE *wf = fopen(tmp_path, "wb");
                    if (wf) {
                        uint32_t magic = 0x56465042;
                        uint32_t ver = 1;
                        uint32_t u_fmt = (uint32_t)bin_format;
                        uint32_t u_len = (uint32_t)actual_len;
                        fwrite(&magic, 4, 1, wf);
                        fwrite(&ver, 4, 1, wf);
                        fwrite(&u_fmt, 4, 1, wf);
                        fwrite(&u_len, 4, 1, wf);
                        fwrite(buf, 1, actual_len, wf);
                        fclose(wf);
                        rename(tmp_path, cache_path);
                    }
                }
                free(buf);
            }
        }
    }
}

static void vf_gl_cache_cleanup(void) {
    pthread_mutex_lock(&s_gl_cache_lock);
    for (int i = 0; i < VF_GL_HASH_BUCKETS; i++) {
        vf_shader_node_t *s = s_shader_buckets[i];
        while (s) {
            vf_shader_node_t *next = s->next;
            free(s);
            s = next;
        }
        s_shader_buckets[i] = NULL;

        vf_program_node_t *p = s_program_buckets[i];
        while (p) {
            vf_program_node_t *next = p->next;
            free(p);
            p = next;
        }
        s_program_buckets[i] = NULL;
    }
    pthread_mutex_unlock(&s_gl_cache_lock);
}

// ---------------------------------------------------------------------------
// Libretro Hardware Rendering / EGL Support
// ---------------------------------------------------------------------------

static retro_proc_address_t vf_hw_get_proc_address(const char *sym) {
    if (!sym) return NULL;
    if (strcmp(sym, "glShaderSource") == 0) return (retro_proc_address_t)vf_glShaderSource;
    if (strcmp(sym, "glCompileShader") == 0) return (retro_proc_address_t)vf_glCompileShader;
    if (strcmp(sym, "glGetShaderiv") == 0) return (retro_proc_address_t)vf_glGetShaderiv;
    if (strcmp(sym, "glGetShaderInfoLog") == 0) return (retro_proc_address_t)vf_glGetShaderInfoLog;
    if (strcmp(sym, "glDeleteShader") == 0) return (retro_proc_address_t)vf_glDeleteShader;
    if (strcmp(sym, "glAttachShader") == 0) return (retro_proc_address_t)vf_glAttachShader;
    if (strcmp(sym, "glDetachShader") == 0) return (retro_proc_address_t)vf_glDetachShader;
    if (strcmp(sym, "glDeleteProgram") == 0) return (retro_proc_address_t)vf_glDeleteProgram;
    if (strcmp(sym, "glLinkProgram") == 0) return (retro_proc_address_t)vf_glLinkProgram;

    retro_proc_address_t proc = (retro_proc_address_t)eglGetProcAddress(sym);
    if (!proc) {
        proc = (retro_proc_address_t)dlsym(RTLD_DEFAULT, sym);
    }
    return proc;
}

static uintptr_t vf_hw_get_current_framebuffer(void) {
    vf_session_t *s = s_current_session;
    if (s && s->window_secondary && s->hw_fbo) {
        return (uintptr_t)s->hw_fbo;
    }
    return 0;
}

static bool vf_egl_init(vf_session_t *s, struct retro_hw_render_callback *cb) {
    if (!s || !cb) return false;
    LOGI("Initializing EGL for libretro HW rendering (context_type=%d, depth=%d, stencil=%d, v=%u.%u)",
         cb->context_type, cb->depth, cb->stencil, cb->version_major, cb->version_minor);

    s->egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (s->egl_display == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed: 0x%x", eglGetError());
        return false;
    }

    EGLint major = 0, minor = 0;
    if (!eglInitialize(s->egl_display, &major, &minor)) {
        LOGE("eglInitialize failed: 0x%x", eglGetError());
        return false;
    }
    LOGI("EGL initialized successfully: version %d.%d", major, minor);

    // Citra on Android requests GLES 3.0+ or GLES 2.0 fallback
    EGLint gles_version = 3;
    EGLint renderable_bit = EGL_OPENGL_ES3_BIT_KHR;
    if (cb->context_type == RETRO_HW_CONTEXT_OPENGLES2) {
        gles_version = 2;
        renderable_bit = EGL_OPENGL_ES2_BIT;
    }

    const EGLint config_attribs[] = {
        EGL_RENDERABLE_TYPE, renderable_bit,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, cb->depth ? 24 : 0,
        EGL_STENCIL_SIZE, cb->stencil ? 8 : 0,
        EGL_NONE
    };

    EGLint num_configs = 0;
    if (!eglChooseConfig(s->egl_display, config_attribs, &s->egl_config, 1, &num_configs) || num_configs < 1) {
        LOGW("eglChooseConfig with 24-bit depth failed, trying 16-bit depth fallback...");
        const EGLint fallback_attribs[] = {
            EGL_RENDERABLE_TYPE, renderable_bit,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, cb->depth ? 16 : 0,
            EGL_NONE
        };
        if (!eglChooseConfig(s->egl_display, fallback_attribs, &s->egl_config, 1, &num_configs) || num_configs < 1) {
            LOGE("eglChooseConfig completely failed: 0x%x", eglGetError());
            return false;
        }
    }

    const EGLint ctx_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, gles_version,
        EGL_NONE
    };

    s->egl_context = eglCreateContext(s->egl_display, s->egl_config, EGL_NO_CONTEXT, ctx_attribs);
    if (s->egl_context == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed for GLES %d: 0x%x", gles_version, eglGetError());
        return false;
    }

    // Create an offscreen pbuffer surface sized for emulation framebuffer so context/viewport doesn't collapse to 1x1
    EGLint max_pb_w = 2880, max_pb_h = 1920;
    eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_MAX_PBUFFER_WIDTH, &max_pb_w);
    eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_MAX_PBUFFER_HEIGHT, &max_pb_h);
    int target_pb_w = (max_pb_w > 0 && max_pb_w < 2880) ? max_pb_w : 2880;
    int target_pb_h = (max_pb_h > 0 && max_pb_h < 1920) ? max_pb_h : 1920;

    const EGLint pbuffer_attribs[] = {
        EGL_WIDTH, target_pb_w,
        EGL_HEIGHT, target_pb_h,
        EGL_NONE
    };
    s->egl_pbuffer_surface = eglCreatePbufferSurface(s->egl_display, s->egl_config, pbuffer_attribs);
    if (s->egl_pbuffer_surface == EGL_NO_SURFACE) {
        LOGW("eglCreatePbufferSurface failed for %dx%d (err=0x%x), falling back to 1x1", target_pb_w, target_pb_h, eglGetError());
        const EGLint fallback_attribs[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
        s->egl_pbuffer_surface = eglCreatePbufferSurface(s->egl_display, s->egl_config, fallback_attribs);
    }
    if (s->egl_pbuffer_surface == EGL_NO_SURFACE) {
        LOGE("eglCreatePbufferSurface failed: 0x%x", eglGetError());
        return false;
    }

    s->egl_surface = s->egl_pbuffer_surface;

    // Provide callbacks to the core
    cb->get_proc_address = vf_hw_get_proc_address;
    cb->get_current_framebuffer = vf_hw_get_current_framebuffer;

    s->hw_render = *cb;
    s->is_hw_render = true;
    s->hw_context_reset_done = false;

    // Make current on this loader thread so core can query extensions during retro_load_game
    if (!eglMakeCurrent(s->egl_display, s->egl_surface, s->egl_surface, s->egl_context)) {
        LOGE("eglMakeCurrent failed during vf_egl_init: 0x%x", eglGetError());
        return false;
    }

    const char *v = (const char *)glGetString(GL_VERSION);
    const char *r = (const char *)glGetString(GL_RENDERER);
    uint64_t dh = 5381ULL;
    if (v) vf_hash_bytes(v, strlen(v), &dh, &dh);
    if (r) vf_hash_bytes(r, strlen(r), &dh, &dh);
    s_gl_driver_hash = dh;

    // Pre-create initial hw_fbo so get_current_framebuffer() always returns a valid FBO from frame 0
    glGenTextures(1, &s->hw_texture);
    glBindTexture(GL_TEXTURE_2D, s->hw_texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 400, 480, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glGenRenderbuffers(1, &s->hw_depth_rb);
    glBindRenderbuffer(GL_RENDERBUFFER, s->hw_depth_rb);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, 400, 480);

    glGenFramebuffers(1, &s->hw_fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, s->hw_fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, s->hw_texture, 0);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, s->hw_depth_rb);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    s->hw_fbo_w = 400;
    s->hw_fbo_h = 480;

    LOGI("EGL HW render context initialized successfully on loader thread (GLES %s, Vendor: %s, driver_hash=%llx, fbo=%u)",
         v ? v : "unknown", (const char *)glGetString(GL_VENDOR), (unsigned long long)s_gl_driver_hash, s->hw_fbo);
    return true;
}

// ---------------------------------------------------------------------------
// Libretro Virtual File System (VFS) Implementation
// ---------------------------------------------------------------------------

struct retro_vfs_file_handle {
    FILE *fp;
    char path[PATH_MAX];
    unsigned mode;
    bool is_shader_cache;
    bool has_read;
};

struct retro_vfs_dir_handle {
    DIR *dir;
    struct dirent *entry;
    char path[PATH_MAX];
    bool include_hidden;
};

static const char *vf_vfs_get_path(struct retro_vfs_file_handle *stream) {
    if (!stream) return NULL;
    return stream->path;
}

static struct retro_vfs_file_handle *vf_vfs_open(const char *path, unsigned mode, unsigned hints) {
    (void)hints;
    if (!path || !path[0]) return NULL;

    const char *mode_str = "rb";
    if ((mode & RETRO_VFS_FILE_ACCESS_READ_WRITE) == RETRO_VFS_FILE_ACCESS_READ_WRITE) {
        if (mode & RETRO_VFS_FILE_ACCESS_UPDATE_EXISTING) {
            mode_str = "r+b";
        } else {
            mode_str = "w+b";
        }
    } else if (mode & RETRO_VFS_FILE_ACCESS_WRITE) {
        if (mode & RETRO_VFS_FILE_ACCESS_UPDATE_EXISTING) {
            mode_str = "r+b";
        } else {
            mode_str = "wb";
        }
    } else {
        mode_str = "rb";
    }

    FILE *fp = fopen(path, mode_str);
    if (!fp && (mode & (RETRO_VFS_FILE_ACCESS_WRITE | RETRO_VFS_FILE_ACCESS_UPDATE_EXISTING))) {
        char parent[PATH_MAX];
        strncpy(parent, path, sizeof(parent) - 1);
        parent[sizeof(parent) - 1] = '\0';
        char *slash = strrchr(parent, '/');
        if (slash) {
            *slash = '\0';
            // Recursively create parent directories
            for (char *p = parent + 1; *p; p++) {
                if (*p == '/') {
                    *p = '\0';
                    struct stat st;
                    if (stat(parent, &st) != 0) {
                        mkdir(parent, 0755);
                    }
                    *p = '/';
                }
            }
            mkdir(parent, 0755);
            fp = fopen(path, mode_str);
        }
    }
    if (!fp && (mode & RETRO_VFS_FILE_ACCESS_UPDATE_EXISTING)) {
        // If updating an existing file that didn't exist yet, open with write/create
        fp = fopen(path, (mode & RETRO_VFS_FILE_ACCESS_READ) ? "w+b" : "wb");
    }

    if (!fp) {
        LOGE("vf_vfs_open failed for path '%s' (mode=0x%x, mode_str='%s'): %s", path, mode, mode_str, strerror(errno));
        return NULL;
    }

    struct retro_vfs_file_handle *handle = (struct retro_vfs_file_handle *)calloc(1, sizeof(struct retro_vfs_file_handle));
    if (!handle) {
        fclose(fp);
        return NULL;
    }

    handle->fp = fp;
    strncpy(handle->path, path, sizeof(handle->path) - 1);
    handle->mode = mode;
    handle->is_shader_cache = (strstr(path, "/shaders/") != NULL);
    handle->has_read = false;
    LOGI("vf_vfs_open succeeded for path '%s' (mode=0x%x, mode_str='%s', is_shader_cache=%d)",
         path, mode, mode_str, handle->is_shader_cache);
    return handle;
}

static int vf_vfs_close(struct retro_vfs_file_handle *stream) {
    if (!stream) return -1;
    int ret = 0;
    if (stream->fp) {
        ret = fclose(stream->fp);
    }
    free(stream);
    return ret;
}

static int64_t vf_vfs_size(struct retro_vfs_file_handle *stream) {
    if (!stream || !stream->fp) return -1;
    long curr = ftell(stream->fp);
    if (curr < 0) return -1;
    if (fseek(stream->fp, 0, SEEK_END) != 0) return -1;
    long size = ftell(stream->fp);
    fseek(stream->fp, curr, SEEK_SET);
    return (int64_t)size;
}

static int64_t vf_vfs_truncate(struct retro_vfs_file_handle *stream, int64_t length) {
    if (!stream || !stream->fp) return -1;
    int fd = fileno(stream->fp);
    if (fd < 0) return -1;
    return (ftruncate(fd, (off_t)length) == 0) ? 0 : -1;
}

static int64_t vf_vfs_tell(struct retro_vfs_file_handle *stream) {
    if (!stream || !stream->fp) return -1;
    return (int64_t)ftell(stream->fp);
}

static int64_t vf_vfs_seek(struct retro_vfs_file_handle *stream, int64_t offset, int seek_position) {
    if (!stream || !stream->fp) return -1;
    int whence = SEEK_SET;
    switch (seek_position) {
        case RETRO_VFS_SEEK_POSITION_START:   whence = SEEK_SET; break;
        case RETRO_VFS_SEEK_POSITION_CURRENT: whence = SEEK_CUR; break;
        case RETRO_VFS_SEEK_POSITION_END:     whence = SEEK_END; break;
        default: return -1;
    }
    return (fseek(stream->fp, (long)offset, whence) == 0) ? 0 : -1;
}

static int64_t vf_vfs_read(struct retro_vfs_file_handle *stream, void *s, uint64_t len) {
    if (!stream || !stream->fp || !s) return -1;
    if (stream->is_shader_cache && !stream->has_read) {
        // Azahar opens transferable and precompiled shader cache files once with "ab+",
        // writes the initial header if needed, and then reads from the same handle expecting
        // standard C fopen("ab+") initial read position 0.
        fseek(stream->fp, 0, SEEK_SET);
        stream->has_read = true;
    }
    size_t read_bytes = fread(s, 1, (size_t)len, stream->fp);
    if (read_bytes == 0 && ferror(stream->fp)) return -1;
    return (int64_t)read_bytes;
}

static int64_t vf_vfs_write(struct retro_vfs_file_handle *stream, const void *s, uint64_t len) {
    if (!stream || !stream->fp || !s) return -1;
    if (stream->is_shader_cache) {
        // Enforce standard C fopen("ab+") append-at-EOF semantics on all writes.
        fseek(stream->fp, 0, SEEK_END);
    }
    size_t written_bytes = fwrite(s, 1, (size_t)len, stream->fp);
    if (stream->is_shader_cache) {
        fflush(stream->fp);
        if (written_bytes != (size_t)len) {
            LOGE("vf_vfs_write shader cache FAILED for '%s': len=%llu, written=%zu, err=%s",
                 stream->path, (unsigned long long)len, written_bytes, strerror(errno));
        }
    }
    if (written_bytes == 0 && len > 0 && ferror(stream->fp)) return -1;
    return (int64_t)written_bytes;
}

static int vf_vfs_flush(struct retro_vfs_file_handle *stream) {
    if (!stream || !stream->fp) return -1;
    return fflush(stream->fp);
}

static int vf_vfs_remove(const char *path) {
    if (!path) return -1;
    return remove(path);
}

static int vf_vfs_rename(const char *old_path, const char *new_path) {
    if (!old_path || !new_path) return -1;
    return rename(old_path, new_path);
}

static int vf_vfs_stat(const char *path, int32_t *size) {
    if (!path) return 0;
    struct stat st;
    if (stat(path, &st) != 0) return 0;

    int flags = RETRO_VFS_STAT_IS_VALID;
    if (S_ISDIR(st.st_mode)) {
        flags |= RETRO_VFS_STAT_IS_DIRECTORY;
    }
    if (S_ISCHR(st.st_mode)) {
        flags |= RETRO_VFS_STAT_IS_CHARACTER_SPECIAL;
    }
    if (size) {
        *size = (int32_t)st.st_size;
    }
    return flags;
}

static int vf_vfs_stat_64(const char *path, int64_t *size) {
    if (!path) return 0;
    struct stat st;
    if (stat(path, &st) != 0) return 0;

    int flags = RETRO_VFS_STAT_IS_VALID;
    if (S_ISDIR(st.st_mode)) {
        flags |= RETRO_VFS_STAT_IS_DIRECTORY;
    }
    if (S_ISCHR(st.st_mode)) {
        flags |= RETRO_VFS_STAT_IS_CHARACTER_SPECIAL;
    }
    if (size) {
        *size = (int64_t)st.st_size;
    }
    return flags;
}

static int vf_vfs_mkdir(const char *dir) {
    if (!dir || !*dir) return -1;
    char tmp[PATH_MAX];
    strncpy(tmp, dir, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';
    size_t len = strlen(tmp);
    if (len > 0 && tmp[len - 1] == '/') tmp[len - 1] = '\0';

    for (char *p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            struct stat st;
            if (stat(tmp, &st) != 0) {
                mkdir(tmp, 0755);
            }
            *p = '/';
        }
    }
    struct stat st;
    if (stat(tmp, &st) == 0) {
        return S_ISDIR(st.st_mode) ? -2 : -1;
    }
    if (mkdir(tmp, 0755) == 0) return 0;
    if (errno == EEXIST) return -2;
    return -1;
}

static struct retro_vfs_dir_handle *vf_vfs_opendir(const char *dir, bool include_hidden) {
    if (!dir) return NULL;
    DIR *d = opendir(dir);
    if (!d) return NULL;

    struct retro_vfs_dir_handle *handle = (struct retro_vfs_dir_handle *)calloc(1, sizeof(struct retro_vfs_dir_handle));
    if (!handle) {
        closedir(d);
        return NULL;
    }
    handle->dir = d;
    handle->include_hidden = include_hidden;
    strncpy(handle->path, dir, sizeof(handle->path) - 1);
    return handle;
}

static bool vf_vfs_readdir(struct retro_vfs_dir_handle *dirstream) {
    if (!dirstream || !dirstream->dir) return false;
    struct dirent *ent = NULL;
    while ((ent = readdir(dirstream->dir)) != NULL) {
        if (!dirstream->include_hidden && ent->d_name[0] == '.') {
            continue;
        }
        dirstream->entry = ent;
        return true;
    }
    dirstream->entry = NULL;
    return false;
}

static const char *vf_vfs_dirent_get_name(struct retro_vfs_dir_handle *dirstream) {
    if (!dirstream || !dirstream->entry) return NULL;
    return dirstream->entry->d_name;
}

static bool vf_vfs_dirent_is_dir(struct retro_vfs_dir_handle *dirstream) {
    if (!dirstream || !dirstream->entry) return false;
#if defined(DT_DIR)
    if (dirstream->entry->d_type == DT_DIR) return true;
    if (dirstream->entry->d_type != DT_UNKNOWN) return false;
#endif
    char full[PATH_MAX];
    snprintf(full, sizeof(full), "%s/%s", dirstream->path, dirstream->entry->d_name);
    struct stat st;
    if (stat(full, &st) == 0) {
        return S_ISDIR(st.st_mode);
    }
    return false;
}

static int vf_vfs_closedir(struct retro_vfs_dir_handle *dirstream) {
    if (!dirstream) return -1;
    int ret = 0;
    if (dirstream->dir) {
        ret = closedir(dirstream->dir);
    }
    free(dirstream);
    return ret;
}

static bool core_environment(unsigned cmd, void *data) {
    vf_session_t *s = s_current_session;
    if (!s) return false;

    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            const enum retro_pixel_format *fmt = (const enum retro_pixel_format *)data;
            if (*fmt == RETRO_PIXEL_FORMAT_XRGB8888 ||
                *fmt == RETRO_PIXEL_FORMAT_RGB565 ||
                *fmt == RETRO_PIXEL_FORMAT_0RGB1555) {
                s->pixel_format = *fmt;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            *(const char **)data = s->system_directory;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            *(const char **)data = s->save_directory;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE: {
            *(bool *)data = true;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_ROTATION: {
            s->av_info.rotation = *(const unsigned *)data;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_GEOMETRY: {
            const struct retro_game_geometry *geom = (const struct retro_game_geometry *)data;
            LOGI("Core updated geometry: %ux%u (aspect: %.3f)", geom->base_width, geom->base_height, geom->aspect_ratio);
            s->av_info.width = (int)geom->base_width;
            s->av_info.height = (int)geom->base_height;
            s->av_info.aspect_ratio = (geom->aspect_ratio > 0.0) 
                ? geom->aspect_ratio 
                : ((double)geom->base_width / (double)geom->base_height);
            pthread_mutex_lock(&s->window_mutex);
            s->last_buffer_w = -1;
            s->last_buffer_h = -1;
            pthread_mutex_unlock(&s->window_mutex);
            if (s->callbacks.on_geometry_changed) {
                s->callbacks.on_geometry_changed(s->callbacks.user_data, s->av_info.width, s->av_info.height, s->av_info.aspect_ratio);
            }
            return true;
        }
        case RETRO_ENVIRONMENT_SHUTDOWN: {
            if (s->callbacks.on_core_shutdown) {
                s->callbacks.on_core_shutdown(s->callbacks.user_data);
            }
            return true;
        }
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            struct retro_log_callback *cb = (struct retro_log_callback *)data;
            if (cb) {
                cb->log = core_log_printf;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_SET_VARIABLES: {
            const struct retro_variable *vars = (const struct retro_variable *)data;
            if (vars) {
                for (int i = 0; vars[i].key != NULL; i++) {
                    LOGI("Core registered variable: key='%s', value='%s'", vars[i].key, vars[i].value ? vars[i].value : "");
                }
            }
            return true;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *var = (struct retro_variable *)data;
            if (!var || !var->key) return false;
            for (int i = 0; i < s->option_count; i++) {
                if (strcmp(var->key, s->options[i].key) == 0) {
                    var->value = s->options[i].value;
                    return true;
                }
            }
            // Essential defaults for melonDS standalone parity and direct booting
            if (strcmp(var->key, "melonds_screen_layout") == 0) {
                var->value = "Left/Right";
                return true;
            }
            if (strcmp(var->key, "melonds_boot_directly") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "melonds_console_mode") == 0) {
                var->value = "DS";
                return true;
            }
            if (strcmp(var->key, "melonds_threaded_renderer") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "melonds_jit_enable") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "melonds_touch_mode") == 0) {
                var->value = "Touch";
                return true;
            }
            // Essential defaults for Citra / Azahar 3DS core
            if (strcmp(var->key, "citra_layout_option") == 0 || strcmp(var->key, "azahar_layout_option") == 0) {
                for (int i = 0; i < s->option_count; i++) {
                    if (strcmp(s->options[i].key, "citra_layout_option") == 0 || strcmp(s->options[i].key, "azahar_layout_option") == 0) {
                        if (strstr(s->options[i].value, "side") != NULL || strstr(s->options[i].value, "Side") != NULL) {
                            var->value = "side_by_side";
                            return true;
                        } else if (strstr(s->options[i].value, "single") != NULL || strstr(s->options[i].value, "Single") != NULL) {
                            var->value = "single_screen";
                            return true;
                        } else if (strstr(s->options[i].value, "large") != NULL || strstr(s->options[i].value, "Large") != NULL) {
                            var->value = "large_screen";
                            return true;
                        }
                    }
                }
                var->value = "default";
                return true;
            }
            if (strcmp(var->key, "citra_screen_layout") == 0 || strcmp(var->key, "azahar_screen_layout") == 0) {
                for (int i = 0; i < s->option_count; i++) {
                    if (strcmp(s->options[i].key, "citra_layout_option") == 0 || strcmp(s->options[i].key, "azahar_layout_option") == 0) {
                        if (strstr(s->options[i].value, "side") != NULL || strstr(s->options[i].value, "Side") != NULL) {
                            var->value = "left_right";
                            return true;
                        } else if (strstr(s->options[i].value, "single") != NULL || strstr(s->options[i].value, "Single") != NULL) {
                            var->value = "top_only";
                            return true;
                        }
                    }
                }
                var->value = "top_bottom";
                return true;
            }
            if (strcmp(var->key, "citra_swap_screen") == 0 || strcmp(var->key, "azahar_swap_screen") == 0) {
                var->value = "Top";
                return true;
            }
            if (strcmp(var->key, "citra_use_cpu_jit") == 0 || strcmp(var->key, "azahar_use_cpu_jit") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_is_new_3ds") == 0 || strcmp(var->key, "azahar_is_new_3ds") == 0) {
                var->value = "New 3DS";
                return true;
            }
            if (strcmp(var->key, "citra_cpu_clock_percentage") == 0 || strcmp(var->key, "azahar_cpu_clock_percentage") == 0) {
                var->value = "100";
                return true;
            }
            if (strcmp(var->key, "citra_audio_emulation") == 0 || strcmp(var->key, "azahar_audio_emulation") == 0) {
                var->value = "hle";
                return true;
            }
            if (strcmp(var->key, "citra_resolution_factor") == 0 || strcmp(var->key, "azahar_resolution_factor") == 0) {
                for (int i = 0; i < s->option_count; i++) {
                    if (strcmp(s->options[i].key, "citra_resolution_factor") == 0 || strcmp(s->options[i].key, "azahar_resolution_factor") == 0) {
                        if (strstr(s->options[i].value, "2") != NULL) {
                            var->value = "2";
                            return true;
                        } else if (strstr(s->options[i].value, "3") != NULL) {
                            var->value = "3";
                            return true;
                        } else if (strstr(s->options[i].value, "4") != NULL) {
                            var->value = "4";
                            return true;
                        } else {
                            var->value = "1";
                            return true;
                        }
                    }
                }
                var->value = "1";
                return true;
            }
            if (strcmp(var->key, "citra_graphics_api") == 0 || strcmp(var->key, "azahar_graphics_api") == 0) {
                var->value = "OpenGL";
                return true;
            }
            if (strcmp(var->key, "citra_use_hw_renderer") == 0 || strcmp(var->key, "azahar_use_hw_renderer") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_software_renderer") == 0 || strcmp(var->key, "azahar_use_software_renderer") == 0) {
                var->value = "disabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_hw_shaders") == 0 || strcmp(var->key, "azahar_use_hw_shaders") == 0 ||
                strcmp(var->key, "citra_use_hw_shader") == 0 || strcmp(var->key, "azahar_use_hw_shader") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_shader_jit") == 0 || strcmp(var->key, "azahar_use_shader_jit") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_libretro_save_path") == 0 || strcmp(var->key, "azahar_use_libretro_save_path") == 0) {
                var->value = "LibRetro Default";
                return true;
            }
            if (strcmp(var->key, "citra_use_hw_shader_cache") == 0 || strcmp(var->key, "azahar_use_hw_shader_cache") == 0 ||
                strcmp(var->key, "citra_use_disk_shader_cache") == 0 || strcmp(var->key, "azahar_use_disk_shader_cache") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_async_shader_compilation") == 0 || strcmp(var->key, "azahar_async_shader_compilation") == 0 ||
                strcmp(var->key, "citra_async_shaders") == 0 || strcmp(var->key, "azahar_async_shaders") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_separable_shaders") == 0 || strcmp(var->key, "azahar_use_separable_shaders") == 0 ||
                strcmp(var->key, "citra_separable_shaders") == 0 || strcmp(var->key, "azahar_separable_shaders") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_custom_textures") == 0 || strcmp(var->key, "azahar_use_custom_textures") == 0 ||
                strcmp(var->key, "citra_custom_textures") == 0 || strcmp(var->key, "azahar_custom_textures") == 0) {
                for (int i = 0; i < s->option_count; i++) {
                    if (strcmp(s->options[i].key, "citra_use_custom_textures") == 0 ||
                        strcmp(s->options[i].key, "azahar_use_custom_textures") == 0 ||
                        strcmp(s->options[i].key, "citra_custom_textures") == 0 ||
                        strcmp(s->options[i].key, "azahar_custom_textures") == 0) {
                        var->value = s->options[i].value;
                        return true;
                    }
                }
                var->value = "disabled";
                return true;
            }
            if (strcmp(var->key, "citra_preload_textures") == 0 || strcmp(var->key, "azahar_preload_textures") == 0) {
                for (int i = 0; i < s->option_count; i++) {
                    if (strcmp(s->options[i].key, "citra_preload_textures") == 0 ||
                        strcmp(s->options[i].key, "azahar_preload_textures") == 0) {
                        var->value = s->options[i].value;
                        return true;
                    }
                }
                var->value = "disabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_acc_geo_shaders") == 0 || strcmp(var->key, "azahar_use_acc_geo_shaders") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_use_acc_mul") == 0 || strcmp(var->key, "azahar_use_acc_mul") == 0 ||
                strcmp(var->key, "citra_shaders_accurate_mul") == 0 || strcmp(var->key, "azahar_shaders_accurate_mul") == 0) {
                var->value = "disabled";
                return true;
            }
            if (strcmp(var->key, "citra_limit_framerate") == 0 || strcmp(var->key, "azahar_limit_framerate") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_dup_30hz") == 0 || strcmp(var->key, "azahar_dup_30hz") == 0 ||
                strcmp(var->key, "citra_duplicate_frames") == 0 || strcmp(var->key, "azahar_duplicate_frames") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "gpsp_bios") == 0) {
                var->value = "auto";
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: {
            bool updated = atomic_exchange(&s->options_updated, false);
            *(bool *)data = updated;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS: {
            if (data) *(bool *)data = true;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
            return true;
        }
        case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER: {
            if (data) {
                *(enum retro_hw_context_type *)data = RETRO_HW_CONTEXT_OPENGLES3;
                LOGI("Core queried RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER -> returning RETRO_HW_CONTEXT_OPENGLES3");
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VFS_INTERFACE: {
            struct retro_vfs_interface_info *info = (struct retro_vfs_interface_info *)data;
            if (!info) return false;
            static struct retro_vfs_interface s_vfs_interface = {
                .get_path = vf_vfs_get_path,
                .open = vf_vfs_open,
                .close = vf_vfs_close,
                .size = vf_vfs_size,
                .tell = vf_vfs_tell,
                .seek = vf_vfs_seek,
                .read = vf_vfs_read,
                .write = vf_vfs_write,
                .flush = vf_vfs_flush,
                .remove = vf_vfs_remove,
                .rename = vf_vfs_rename,
                .truncate = vf_vfs_truncate,
                .stat = vf_vfs_stat,
                .mkdir = vf_vfs_mkdir,
                .opendir = vf_vfs_opendir,
                .readdir = vf_vfs_readdir,
                .dirent_get_name = vf_vfs_dirent_get_name,
                .dirent_is_dir = vf_vfs_dirent_is_dir,
                .closedir = vf_vfs_closedir,
                .stat_64 = vf_vfs_stat_64
            };
            info->iface = &s_vfs_interface;
            // The libretro spec requires the frontend to set required_interface_version to its actual API version.
            // libretro-common's filestream_vfs_init requires required_interface_version >= 2 to register callbacks.
            LOGI("Core requested RETRO_ENVIRONMENT_GET_VFS_INTERFACE (requested_version=%u) -> providing VFS interface version 3",
                 info->required_interface_version);
            info->required_interface_version = 3;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE: {
            LOGI("Core requested RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE (unsupported)");
            return false;
        }
        case RETRO_ENVIRONMENT_SET_HW_RENDER: {
            struct retro_hw_render_callback *cb = (struct retro_hw_render_callback *)data;
            if (!cb) return false;
            LOGI("Core requested RETRO_ENVIRONMENT_SET_HW_RENDER (context_type=%d)", cb->context_type);
            if (cb->context_type == RETRO_HW_CONTEXT_VULKAN) {
                LOGW("Core requested Vulkan context, but Vantafyn host provides OpenGL ES 3.0. Rejecting Vulkan so core falls back to GLES.");
                return false;
            }
            return vf_egl_init(s, cb);
        }
        default:
            return false;
    }
}

static void blit_frame_nearest(
    const uint32_t *src, int src_w, int src_h, size_t src_stride,
    uint32_t *dst, int dst_w, int dst_h, size_t dst_stride,
    uint32_t **p_lut_x, int *p_lut_x_cap, int *p_lut_x_src, int *p_lut_x_dst,
    uint32_t **p_lut_y, int *p_lut_y_cap, int *p_lut_y_src, int *p_lut_y_dst
) {
    if (!src || !dst || src_w <= 0 || src_h <= 0 || dst_w <= 0 || dst_h <= 0) return;

    if (src_w == dst_w && src_h == dst_h) {
        for (int y = 0; y < dst_h; y++) {
            memcpy(dst + y * dst_stride, src + y * src_stride, dst_w * sizeof(uint32_t));
        }
        return;
    }

    // Reallocate and compute X LUT if needed
    if (*p_lut_x_cap < dst_w) {
        free(*p_lut_x);
        *p_lut_x = (uint32_t *)malloc(dst_w * sizeof(uint32_t));
        *p_lut_x_cap = dst_w;
        *p_lut_x_src = 0;
        *p_lut_x_dst = 0;
    }
    if (*p_lut_x && (*p_lut_x_src != src_w || *p_lut_x_dst != dst_w)) {
        uint32_t *lx = *p_lut_x;
        for (int dx = 0; dx < dst_w; dx++) {
            lx[dx] = (uint32_t)((dx * (uint64_t)src_w) / dst_w);
        }
        *p_lut_x_src = src_w;
        *p_lut_x_dst = dst_w;
    }

    // Reallocate and compute Y LUT if needed
    if (*p_lut_y_cap < dst_h) {
        free(*p_lut_y);
        *p_lut_y = (uint32_t *)malloc(dst_h * sizeof(uint32_t));
        *p_lut_y_cap = dst_h;
        *p_lut_y_src = 0;
        *p_lut_y_dst = 0;
    }
    if (*p_lut_y && (*p_lut_y_src != src_h || *p_lut_y_dst != dst_h)) {
        uint32_t *ly = *p_lut_y;
        for (int dy = 0; dy < dst_h; dy++) {
            ly[dy] = (uint32_t)((dy * (uint64_t)src_h) / dst_h);
        }
        *p_lut_y_src = src_h;
        *p_lut_y_dst = dst_h;
    }

    const uint32_t *lx = *p_lut_x;
    const uint32_t *ly = *p_lut_y;
    if (!lx || !ly) return;

    for (int dy = 0; dy < dst_h; dy++) {
        const uint32_t *src_row = src + ly[dy] * src_stride;
        uint32_t *dst_row = dst + dy * dst_stride;
        for (int dx = 0; dx < dst_w; dx++) {
            dst_row[dx] = src_row[lx[dx]];
        }
    }
}

static void core_video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    vf_session_t *s = s_current_session;
    if (!s) return;

    // Fast-path for Hardware-Accelerated (OpenGL ES) cores
    if (s->is_hw_render || data == RETRO_HW_FRAME_BUFFER_VALID) {
        pthread_mutex_lock(&s->window_mutex);
        if (s->window_secondary && (height >= 384 || width >= 512 || height >= 480 || width >= 720)) {
            // Dual physical displays mode in Hardware Rendering (3DS Citra / Azahar)
            ANativeWindow *win_top = s->swap_dual_screens ? s->window : s->window_secondary;
            ANativeWindow *win_bottom = s->swap_dual_screens ? s->window_secondary : s->window;

            unsigned top_w, top_h, bot_w, bot_h;
            int top_src_x0, top_src_y0, top_src_x1, top_src_y1;
            int bot_src_x0, bot_src_y0, bot_src_x1, bot_src_y1;

            if (height >= width) {
                // Vertical stacked layout (400x480 at 1x, etc.)
                top_w = width;
                top_h = height / 2;
                bot_w = width * 320 / 400;
                bot_h = height - top_h;
                int margin_w = (int)((width - bot_w) / 2);
                top_src_x0 = 0; top_src_y0 = (int)top_h; top_src_x1 = (int)width; top_src_y1 = (int)height;
                bot_src_x0 = margin_w; bot_src_y0 = 0; bot_src_x1 = margin_w + (int)bot_w; bot_src_y1 = (int)top_h;
            } else {
                // Horizontal side-by-side layout (720x240 at 1x, 1440x480 at 2x, etc.)
                top_w = (width == 1440) ? 800 : (width == 720 ? 400 : width * 400 / 720);
                top_h = height;
                bot_w = width - top_w;
                bot_h = height;
                top_src_x0 = 0; top_src_y0 = 0; top_src_x1 = (int)top_w; top_src_y1 = (int)height;
                bot_src_x0 = (int)top_w; bot_src_y0 = 0; bot_src_x1 = (int)width; bot_src_y1 = (int)height;
            }

            EGLint format = 0;
            if (s->egl_display && s->egl_config) {
                eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_NATIVE_VISUAL_ID, &format);
            }

            int target_main_w = (s->window == win_top) ? (int)top_w : (int)bot_w;
            int target_main_h = (s->window == win_top) ? (int)top_h : (int)bot_h;
            if (s->last_buffer_w != target_main_w || s->last_buffer_h != target_main_h || s->egl_window_surface == EGL_NO_SURFACE) {
                eglMakeCurrent(s->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                if (s->egl_window_surface != EGL_NO_SURFACE) {
                    eglDestroySurface(s->egl_display, s->egl_window_surface);
                    s->egl_window_surface = EGL_NO_SURFACE;
                }
                ANativeWindow_setBuffersGeometry(s->window, (int32_t)target_main_w, (int32_t)target_main_h, format);
                s->last_buffer_w = target_main_w;
                s->last_buffer_h = target_main_h;
                s->egl_window_surface = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window, NULL);
                s->active_egl_window = s->window;
                LOGI("EGL HW render updated main window geometry to %dx%d", target_main_w, target_main_h);
            }

            int target_sec_w = (s->window_secondary == win_top) ? (int)top_w : (int)bot_w;
            int target_sec_h = (s->window_secondary == win_top) ? (int)top_h : (int)bot_h;
            if (s->last_buffer_secondary_w != target_sec_w || s->last_buffer_secondary_h != target_sec_h || s->egl_surface_secondary == EGL_NO_SURFACE) {
                eglMakeCurrent(s->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                if (s->egl_surface_secondary != EGL_NO_SURFACE) {
                    eglDestroySurface(s->egl_display, s->egl_surface_secondary);
                    s->egl_surface_secondary = EGL_NO_SURFACE;
                }
                ANativeWindow_setBuffersGeometry(s->window_secondary, (int32_t)target_sec_w, (int32_t)target_sec_h, format);
                s->last_buffer_secondary_w = target_sec_w;
                s->last_buffer_secondary_h = target_sec_h;
                s->egl_surface_secondary = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window_secondary, NULL);
                s->active_egl_window_secondary = s->window_secondary;
                LOGI("EGL HW render updated secondary window geometry to %dx%d", target_sec_w, target_sec_h);
            }

            // Ensure EGL context is current on worker thread before any GL operations
            eglMakeCurrent(s->egl_display, s->egl_pbuffer_surface, s->egl_pbuffer_surface, s->egl_context);

            if (s->hw_fbo == 0 || s->hw_fbo_w != (int)width || s->hw_fbo_h != (int)height) {
                if (s->hw_fbo) {
                    glDeleteFramebuffers(1, &s->hw_fbo);
                    glDeleteTextures(1, &s->hw_texture);
                    glDeleteRenderbuffers(1, &s->hw_depth_rb);
                    s->hw_fbo = 0;
                }
                glGenTextures(1, &s->hw_texture);
                glBindTexture(GL_TEXTURE_2D, s->hw_texture);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

                glGenRenderbuffers(1, &s->hw_depth_rb);
                glBindRenderbuffer(GL_RENDERBUFFER, s->hw_depth_rb);
                glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);

                glGenFramebuffers(1, &s->hw_fbo);
                glBindFramebuffer(GL_FRAMEBUFFER, s->hw_fbo);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, s->hw_texture, 0);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, s->hw_depth_rb);
                s->hw_fbo_w = (int)width;
                s->hw_fbo_h = (int)height;
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                LOGI("Created EGL HW dual-display FBO %u (%dx%d)", s->hw_fbo, width, height);
            }

            EGLSurface surf_top = (win_top == s->window) ? s->egl_window_surface : s->egl_surface_secondary;
            EGLSurface surf_bottom = (win_bottom == s->window) ? s->egl_window_surface : s->egl_surface_secondary;

            // Diagnostic pixel sampling from s->hw_fbo every 60 frames to verify Citra framebuffer content
            static int s_frame_diag_cnt = 0;
            if (++s_frame_diag_cnt % 60 == 0) {
                uint32_t top_pixel = 0, bot_pixel = 0;
                glBindFramebuffer(GL_READ_FRAMEBUFFER, s->hw_fbo);
                glReadPixels(top_src_x0 + (top_src_x1 - top_src_x0) / 2,
                             top_src_y0 + (top_src_y1 - top_src_y0) / 2,
                             1, 1, GL_RGBA, GL_UNSIGNED_BYTE, &top_pixel);
                glReadPixels(bot_src_x0 + (bot_src_x1 - bot_src_x0) / 2,
                             bot_src_y0 + (bot_src_y1 - bot_src_y0) / 2,
                             1, 1, GL_RGBA, GL_UNSIGNED_BYTE, &bot_pixel);
                GLenum err = glGetError();
                LOGI("HW Frame diag #%d (%dx%d): top_px=0x%08X, bot_px=0x%08X, glErr=0x%x, surf_top=%p, surf_bot=%p",
                     s_frame_diag_cnt, width, height, top_pixel, bot_pixel, err, surf_top, surf_bottom);
            }

            if (surf_top != EGL_NO_SURFACE) {
                eglMakeCurrent(s->egl_display, surf_top, surf_top, s->egl_context);
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                glBindFramebuffer(GL_READ_FRAMEBUFFER, s->hw_fbo);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glBlitFramebuffer(top_src_x0, top_src_y0, top_src_x1, top_src_y1,
                                  0, 0, (int)top_w, (int)top_h,
                                  GL_COLOR_BUFFER_BIT, GL_NEAREST);
                eglSwapBuffers(s->egl_display, surf_top);
            }

            if (surf_bottom != EGL_NO_SURFACE) {
                eglMakeCurrent(s->egl_display, surf_bottom, surf_bottom, s->egl_context);
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                glBindFramebuffer(GL_READ_FRAMEBUFFER, s->hw_fbo);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glBlitFramebuffer(bot_src_x0, bot_src_y0, bot_src_x1, bot_src_y1,
                                  0, 0, (int)bot_w, (int)bot_h,
                                  GL_COLOR_BUFFER_BIT, GL_NEAREST);
                eglSwapBuffers(s->egl_display, surf_bottom);
            }

            // Restore context to pbuffer surface and bind hw_fbo ready for next frame
            eglMakeCurrent(s->egl_display, s->egl_pbuffer_surface, s->egl_pbuffer_surface, s->egl_context);
            glBindFramebuffer(GL_FRAMEBUFFER, s->hw_fbo);
            glViewport(0, 0, s->hw_fbo_w, s->hw_fbo_h);
            glDisable(GL_SCISSOR_TEST);
            glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        } else {
            // Single display mode: render full composite directly to s->window
            if (s->window && width > 0 && height > 0) {
                if (s->last_buffer_w != (int)width || s->last_buffer_h != (int)height) {
                    EGLint format = 0;
                    if (s->egl_display && s->egl_config) {
                        eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_NATIVE_VISUAL_ID, &format);
                    }
                    eglMakeCurrent(s->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                    if (s->egl_window_surface != EGL_NO_SURFACE) {
                        eglDestroySurface(s->egl_display, s->egl_window_surface);
                        s->egl_window_surface = EGL_NO_SURFACE;
                    }
                    ANativeWindow_setBuffersGeometry(s->window, (int32_t)width, (int32_t)height, format);
                    s->last_buffer_w = (int)width;
                    s->last_buffer_h = (int)height;
                    s->egl_window_surface = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window, NULL);
                    s->egl_surface = (s->egl_window_surface != EGL_NO_SURFACE) ? s->egl_window_surface : s->egl_pbuffer_surface;
                    eglMakeCurrent(s->egl_display, s->egl_surface, s->egl_surface, s->egl_context);
                    LOGI("EGL HW render updated window buffer geometry to %ux%u", width, height);
                }
            }
            if (s->egl_display && s->egl_surface && s->egl_surface != s->egl_pbuffer_surface) {
                if (width > 0 && height > 0) {
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                    glEnable(GL_SCISSOR_TEST);
                    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

                    if (height >= width) {
                        int margin_w = (int)(width * 40 / 400);
                        int half_h = (int)(height / 2);
                        if (margin_w > 0 && half_h > 0) {
                            glScissor(0, 0, margin_w, half_h);
                            glClear(GL_COLOR_BUFFER_BIT);
                            glScissor(width - margin_w, 0, margin_w, half_h);
                            glClear(GL_COLOR_BUFFER_BIT);

                            if (!s->hw_render.bottom_left_origin) {
                                glScissor(0, half_h, margin_w, half_h);
                                glClear(GL_COLOR_BUFFER_BIT);
                                glScissor(width - margin_w, half_h, margin_w, half_h);
                                glClear(GL_COLOR_BUFFER_BIT);
                            }
                        }
                    }
                    glDisable(GL_SCISSOR_TEST);
                }

                eglSwapBuffers(s->egl_display, s->egl_surface);
            }
        }
        pthread_mutex_unlock(&s->window_mutex);

        if (s->callbacks.on_frame_rendered) {
            s->callbacks.on_frame_rendered(s->callbacks.user_data);
        }
        return;
    }

    if (!data || width == 0 || height == 0) return;

    pthread_mutex_lock(&s->window_mutex);
    if (!s->window) {
        pthread_mutex_unlock(&s->window_mutex);
        return;
    }

    // Allocate / resize conversion buffer if needed
    size_t needed_pixels = (size_t)width * (size_t)height;
    if (!s->frame_buffer || s->frame_buffer_size < needed_pixels) {
        free(s->frame_buffer);
        s->frame_buffer = (uint32_t *)calloc(needed_pixels, sizeof(uint32_t));
        s->frame_buffer_size = needed_pixels;
    }

    // Convert core frame format to Android RGBA_8888 (with optional GBA/GBC LCD color correction & DMG palette)
    bool cc = s->gba_color_correction || s->gbc_color_correction;
    int pal = s->gb_palette_mode;

    bool is_3ds_stacked = (height >= width && width >= 400);
    unsigned margin_w = is_3ds_stacked ? (width * 40 / 400) : 0;
    unsigned half_h = height / 2;

    if (s->pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        const uint32_t *src = (const uint32_t *)data;
        size_t src_stride = pitch / sizeof(uint32_t);
        for (unsigned y = 0; y < height; y++) {
            const uint32_t *line = src + y * src_stride;
            uint32_t *dst_line = s->frame_buffer + y * width;
            bool is_bottom_row = is_3ds_stacked && (y >= half_h);
            for (unsigned x = 0; x < width; x++) {
                if (is_bottom_row && (x < margin_w || x >= width - margin_w)) {
                    dst_line[x] = 0xFF000000;
                    continue;
                }
                uint32_t pixel = line[x];
                uint32_t r = (pixel >> 16) & 0xFF;
                uint32_t g = (pixel >> 8) & 0xFF;
                uint32_t b = pixel & 0xFF;
                if (pal == 1) { // DMG Pea Soup
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 15; g = 56; b = 15; }
                        else if (lum < 128) { r = 48; g = 98; b = 48; }
                        else if (lum < 192) { r = 139; g = 172; b = 15; }
                        else { r = 155; g = 188; b = 15; }
                    }
                } else if (pal == 2) { // Pocket B&W
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 0; g = 0; b = 0; }
                        else if (lum < 128) { r = 85; g = 85; b = 85; }
                        else if (lum < 192) { r = 170; g = 170; b = 170; }
                        else { r = 255; g = 255; b = 255; }
                    }
                } else if (cc) {
                    uint32_t cr = (210 * r + 31 * g + 15 * b) >> 8;
                    uint32_t cg = (15 * r + 200 * g + 41 * b) >> 8;
                    uint32_t cb = (20 * r + 36 * g + 200 * b) >> 8;
                    r = cr > 255 ? 255 : cr;
                    g = cg > 255 ? 255 : cg;
                    b = cb > 255 ? 255 : cb;
                }
                dst_line[x] = (0xFF << 24) | (b << 16) | (g << 8) | r;
            }
        }
    } else if (s->pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
        const uint16_t *src = (const uint16_t *)data;
        size_t src_stride = pitch / sizeof(uint16_t);
        for (unsigned y = 0; y < height; y++) {
            const uint16_t *line = src + y * src_stride;
            uint32_t *dst_line = s->frame_buffer + y * width;
            bool is_bottom_row = is_3ds_stacked && (y >= half_h);
            for (unsigned x = 0; x < width; x++) {
                if (is_bottom_row && (x < margin_w || x >= width - margin_w)) {
                    dst_line[x] = 0xFF000000;
                    continue;
                }
                uint16_t pixel = line[x];
                uint32_t r = ((pixel >> 11) & 0x1F) * 255 / 31;
                uint32_t g = ((pixel >> 5) & 0x3F) * 255 / 63;
                uint32_t b = (pixel & 0x1F) * 255 / 31;
                if (pal == 1) { // DMG Pea Soup
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 15; g = 56; b = 15; }
                        else if (lum < 128) { r = 48; g = 98; b = 48; }
                        else if (lum < 192) { r = 139; g = 172; b = 15; }
                        else { r = 155; g = 188; b = 15; }
                    }
                } else if (pal == 2) { // Pocket B&W
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 0; g = 0; b = 0; }
                        else if (lum < 128) { r = 85; g = 85; b = 85; }
                        else if (lum < 192) { r = 170; g = 170; b = 170; }
                        else { r = 255; g = 255; b = 255; }
                    }
                } else if (cc) {
                    uint32_t cr = (210 * r + 31 * g + 15 * b) >> 8;
                    uint32_t cg = (15 * r + 200 * g + 41 * b) >> 8;
                    uint32_t cb = (20 * r + 36 * g + 200 * b) >> 8;
                    r = cr > 255 ? 255 : cr;
                    g = cg > 255 ? 255 : cg;
                    b = cb > 255 ? 255 : cb;
                }
                dst_line[x] = (0xFF << 24) | (b << 16) | (g << 8) | r;
            }
        }
    } else if (s->pixel_format == RETRO_PIXEL_FORMAT_0RGB1555) {
        const uint16_t *src = (const uint16_t *)data;
        size_t src_stride = pitch / sizeof(uint16_t);
        for (unsigned y = 0; y < height; y++) {
            const uint16_t *line = src + y * src_stride;
            uint32_t *dst_line = s->frame_buffer + y * width;
            bool is_bottom_row = is_3ds_stacked && (y >= half_h);
            for (unsigned x = 0; x < width; x++) {
                if (is_bottom_row && (x < margin_w || x >= width - margin_w)) {
                    dst_line[x] = 0xFF000000;
                    continue;
                }
                uint16_t pixel = line[x];
                uint32_t r = ((pixel >> 10) & 0x1F) * 255 / 31;
                uint32_t g = ((pixel >> 5) & 0x1F) * 255 / 31;
                uint32_t b = (pixel & 0x1F) * 255 / 31;
                if (pal == 1) { // DMG Pea Soup
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 15; g = 56; b = 15; }
                        else if (lum < 128) { r = 48; g = 98; b = 48; }
                        else if (lum < 192) { r = 139; g = 172; b = 15; }
                        else { r = 155; g = 188; b = 15; }
                    }
                } else if (pal == 2) { // Pocket B&W
                    int d1 = (int)r - (int)g;
                    int d2 = (int)g - (int)b;
                    if (d1 >= -28 && d1 <= 28 && d2 >= -28 && d2 <= 28) {
                        uint32_t lum = (77 * r + 150 * g + 29 * b) >> 8;
                        if (lum < 64) { r = 0; g = 0; b = 0; }
                        else if (lum < 128) { r = 85; g = 85; b = 85; }
                        else if (lum < 192) { r = 170; g = 170; b = 170; }
                        else { r = 255; g = 255; b = 255; }
                    }
                } else if (cc) {
                    uint32_t cr = (210 * r + 31 * g + 15 * b) >> 8;
                    uint32_t cg = (15 * r + 200 * g + 41 * b) >> 8;
                    uint32_t cb = (20 * r + 36 * g + 200 * b) >> 8;
                    r = cr > 255 ? 255 : cr;
                    g = cg > 255 ? 255 : cg;
                    b = cb > 255 ? 255 : cb;
                }
                dst_line[x] = (0xFF << 24) | (b << 16) | (g << 8) | r;
            }
        }
    }

    // Optional LCD Ghosting (authentic STN passive-matrix liquid crystal response decay)
    if (!s->prev_frame_buffer || s->prev_frame_buffer_size < needed_pixels) {
        free(s->prev_frame_buffer);
        s->prev_frame_buffer = (uint32_t *)calloc(needed_pixels, sizeof(uint32_t));
        s->prev_frame_buffer_size = needed_pixels;
    }
    if (s->lcd_ghosting && s->prev_frame_buffer) {
        for (size_t i = 0; i < needed_pixels; i++) {
            uint32_t curr = s->frame_buffer[i];
            uint32_t prev = s->prev_frame_buffer[i];
            uint32_t cr = curr & 0xFF;
            uint32_t cg = (curr >> 8) & 0xFF;
            uint32_t cb = (curr >> 16) & 0xFF;
            uint32_t pr = prev & 0xFF;
            uint32_t pg = (prev >> 8) & 0xFF;
            uint32_t pb = (prev >> 16) & 0xFF;
            uint32_t br = (cr * 65 + pr * 35) / 100;
            uint32_t bg = (cg * 65 + pg * 35) / 100;
            uint32_t bb = (cb * 65 + pb * 35) / 100;
            uint32_t blended = (0xFF << 24) | (bb << 16) | (bg << 8) | br;
            s->frame_buffer[i] = blended;
            s->prev_frame_buffer[i] = blended;
        }
    } else if (s->prev_frame_buffer) {
        memcpy(s->prev_frame_buffer, s->frame_buffer, needed_pixels * sizeof(uint32_t));
    }

    ANativeWindow *win_top = s->swap_dual_screens ? s->window : s->window_secondary;
    ANativeWindow *win_bottom = s->swap_dual_screens ? s->window_secondary : s->window;

    if (s->window_secondary && (height >= 384 || width >= 512 || height >= 480 || width >= 720)) {
        // Dual physical displays mode: slice into twin DS / 3DS screens
        unsigned top_w, top_h, bot_w, bot_h;
        const uint32_t *src_bottom;

        if (height >= width) {
            // Vertical stacked layout (Top/Bottom)
            top_w = width;
            top_h = height / 2;
            bot_w = width;
            bot_h = height - top_h;
            src_bottom = s->frame_buffer + top_h * width;
        } else {
            // Horizontal side-by-side layout (Left/Right)
            if (width == 720 || width == 1440) {
                // 3DS: 400x240 top, 320x240 bottom
                top_w = (width == 1440) ? 800 : 400;
                top_h = height;
                bot_w = width - top_w;
                bot_h = height;
                src_bottom = s->frame_buffer + top_w;
            } else {
                top_w = width / 2;
                top_h = height;
                bot_w = width - top_w;
                bot_h = height;
                src_bottom = s->frame_buffer + top_w;
            }
        }

        if (win_top) {
            int *last_w = (win_top == s->window) ? &s->last_buffer_w : &s->last_buffer_secondary_w;
            int *last_h = (win_top == s->window) ? &s->last_buffer_h : &s->last_buffer_secondary_h;
            if (!s->crisp_pixels) {
                if (*last_w != (int)top_w || *last_h != (int)top_h) {
                    ANativeWindow_setBuffersGeometry(win_top, (int32_t)top_w, (int32_t)top_h, WINDOW_FORMAT_RGBA_8888);
                    *last_w = (int)top_w;
                    *last_h = (int)top_h;
                }
                ANativeWindow_Buffer top_buf;
                if (ANativeWindow_lock(win_top, &top_buf, NULL) == 0) {
                    uint32_t *dst = (uint32_t *)top_buf.bits;
                    for (unsigned y = 0; y < top_h && y < (unsigned)top_buf.height; y++) {
                        memcpy(dst + y * top_buf.stride, s->frame_buffer + y * width, top_w * sizeof(uint32_t));
                    }
                    ANativeWindow_unlockAndPost(win_top);
                }
            } else {
                if (*last_w != 0 || *last_h != 0) {
                    ANativeWindow_setBuffersGeometry(win_top, 0, 0, WINDOW_FORMAT_RGBA_8888);
                    *last_w = 0;
                    *last_h = 0;
                }
                ANativeWindow_Buffer top_buf;
                if (ANativeWindow_lock(win_top, &top_buf, NULL) == 0) {
                    blit_frame_nearest(
                        s->frame_buffer, (int)top_w, (int)top_h, width,
                        (uint32_t *)top_buf.bits, top_buf.width, top_buf.height, top_buf.stride,
                        &s->lut_x, &s->lut_x_capacity, &s->lut_x_src_w, &s->lut_x_dst_w,
                        &s->lut_y, &s->lut_y_capacity, &s->lut_y_src_h, &s->lut_y_dst_h
                    );
                    ANativeWindow_unlockAndPost(win_top);
                }
            }
        }

        if (win_bottom) {
            int *last_w = (win_bottom == s->window) ? &s->last_buffer_w : &s->last_buffer_secondary_w;
            int *last_h = (win_bottom == s->window) ? &s->last_buffer_h : &s->last_buffer_secondary_h;
            if (!s->crisp_pixels) {
                if (*last_w != (int)bot_w || *last_h != (int)bot_h) {
                    ANativeWindow_setBuffersGeometry(win_bottom, (int32_t)bot_w, (int32_t)bot_h, WINDOW_FORMAT_RGBA_8888);
                    *last_w = (int)bot_w;
                    *last_h = (int)bot_h;
                }
                ANativeWindow_Buffer bot_buf;
                if (ANativeWindow_lock(win_bottom, &bot_buf, NULL) == 0) {
                    uint32_t *dst = (uint32_t *)bot_buf.bits;
                    for (unsigned y = 0; y < bot_h && y < (unsigned)bot_buf.height; y++) {
                        memcpy(dst + y * bot_buf.stride, src_bottom + y * width, bot_w * sizeof(uint32_t));
                    }
                    ANativeWindow_unlockAndPost(win_bottom);
                }
            } else {
                if (*last_w != 0 || *last_h != 0) {
                    ANativeWindow_setBuffersGeometry(win_bottom, 0, 0, WINDOW_FORMAT_RGBA_8888);
                    *last_w = 0;
                    *last_h = 0;
                }
                ANativeWindow_Buffer bot_buf;
                if (ANativeWindow_lock(win_bottom, &bot_buf, NULL) == 0) {
                    blit_frame_nearest(
                        src_bottom, (int)bot_w, (int)bot_h, width,
                        (uint32_t *)bot_buf.bits, bot_buf.width, bot_buf.height, bot_buf.stride,
                        &s->lut_x, &s->lut_x_capacity, &s->lut_x_src_w, &s->lut_x_dst_w,
                        &s->lut_y, &s->lut_y_capacity, &s->lut_y_src_h, &s->lut_y_dst_h
                    );
                    ANativeWindow_unlockAndPost(win_bottom);
                }
            }
        }
    } else if (s->window) {
        if (!s->crisp_pixels) {
            // Smooth mode: set buffer geometry to core resolution and let display hardware interpolate
            if (s->last_buffer_w != (int)width || s->last_buffer_h != (int)height) {
                ANativeWindow_setBuffersGeometry(s->window, (int32_t)width, (int32_t)height, WINDOW_FORMAT_RGBA_8888);
                s->last_buffer_w = (int)width;
                s->last_buffer_h = (int)height;
            }
            ANativeWindow_Buffer win_buf;
            if (ANativeWindow_lock(s->window, &win_buf, NULL) == 0) {
                uint32_t *dst = (uint32_t *)win_buf.bits;
                for (unsigned y = 0; y < height && y < (unsigned)win_buf.height; y++) {
                    memcpy(dst + y * win_buf.stride, s->frame_buffer + y * width, width * sizeof(uint32_t));
                }
                ANativeWindow_unlockAndPost(s->window);
            }
        } else {
            // Crisp mode: set buffer geometry to native display size and scale with crisp nearest-neighbor
            if (s->last_buffer_w != 0 || s->last_buffer_h != 0) {
                ANativeWindow_setBuffersGeometry(s->window, 0, 0, WINDOW_FORMAT_RGBA_8888);
                s->last_buffer_w = 0;
                s->last_buffer_h = 0;
            }
            ANativeWindow_Buffer win_buf;
            if (ANativeWindow_lock(s->window, &win_buf, NULL) == 0) {
                blit_frame_nearest(
                    s->frame_buffer, (int)width, (int)height, width,
                    (uint32_t *)win_buf.bits, win_buf.width, win_buf.height, win_buf.stride,
                    &s->lut_x, &s->lut_x_capacity, &s->lut_x_src_w, &s->lut_x_dst_w,
                    &s->lut_y, &s->lut_y_capacity, &s->lut_y_src_h, &s->lut_y_dst_h
                );
                ANativeWindow_unlockAndPost(s->window);
            }
        }
    }

    pthread_mutex_unlock(&s->window_mutex);

    if (s->callbacks.on_frame_rendered) {
        s->callbacks.on_frame_rendered(s->callbacks.user_data);
    }
}

static void core_audio_sample(int16_t left, int16_t right) {
    vf_session_t *s = s_current_session;
    if (!s) return;
    int16_t frame[2] = {left, right};
    ring_write_samples(&s->audio_ring, frame, 1, s->audio_filtering, &s->audio_filter_prev_l, &s->audio_filter_prev_r);
}

static size_t core_audio_sample_batch(const int16_t *data, size_t frames) {
    vf_session_t *s = s_current_session;
    if (!s || !data || frames == 0) return 0;
    ring_write_samples(&s->audio_ring, data, frames, s->audio_filtering, &s->audio_filter_prev_l, &s->audio_filter_prev_r);
    return frames;
}

static void core_input_poll(void) {
    // Input is maintained asynchronously via bitmasks and touch states
}

static int16_t core_input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    vf_session_t *s = s_current_session;
    if (!s || port >= VF_MAX_PORTS) return 0;

    if (device == RETRO_DEVICE_JOYPAD) {
        uint32_t mask = atomic_load(&s->input_masks[port]);

        // Synthesize D-pad from left analog stick if analog stick is tilted
        int16_t lx = atomic_load(&s->analog_state[port][RETRO_DEVICE_INDEX_ANALOG_LEFT][RETRO_DEVICE_ID_ANALOG_X]);
        int16_t ly = atomic_load(&s->analog_state[port][RETRO_DEVICE_INDEX_ANALOG_LEFT][RETRO_DEVICE_ID_ANALOG_Y]);
        if (lx > 16000) mask |= (1 << RETRO_DEVICE_ID_JOYPAD_RIGHT);
        else if (lx < -16000) mask |= (1 << RETRO_DEVICE_ID_JOYPAD_LEFT);
        if (ly > 16000) mask |= (1 << RETRO_DEVICE_ID_JOYPAD_DOWN);
        else if (ly < -16000) mask |= (1 << RETRO_DEVICE_ID_JOYPAD_UP);

        if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
            return (int16_t)(mask & 0xFFFF);
        }
        if (id < 16) {
            return (mask & (1 << id)) ? 1 : 0;
        }
        return 0;
    }

    if (device == RETRO_DEVICE_ANALOG) {
        if (index <= 1 && id <= 1) {
            int16_t val = atomic_load(&s->analog_state[port][index][id]);
            // If analog stick is neutral, synthesize left stick from D-pad
            if (val == 0 && index == RETRO_DEVICE_INDEX_ANALOG_LEFT) {
                uint32_t mask = atomic_load(&s->input_masks[port]);
                if (id == RETRO_DEVICE_ID_ANALOG_X) {
                    if (mask & (1 << RETRO_DEVICE_ID_JOYPAD_RIGHT)) return 0x7fff;
                    if (mask & (1 << RETRO_DEVICE_ID_JOYPAD_LEFT)) return -0x7fff;
                } else if (id == RETRO_DEVICE_ID_ANALOG_Y) {
                    if (mask & (1 << RETRO_DEVICE_ID_JOYPAD_DOWN)) return 0x7fff;
                    if (mask & (1 << RETRO_DEVICE_ID_JOYPAD_UP)) return -0x7fff;
                }
            }
            return val;
        }
        return 0;
    }

    // Pointer / Touchscreen (DS Touch Input)
    if (device == RETRO_DEVICE_POINTER) {
        if (id == RETRO_DEVICE_ID_POINTER_PRESSED) {
            return atomic_load(&s->touch_pressed) ? 1 : 0;
        } else if (id == RETRO_DEVICE_ID_POINTER_X) {
            return atomic_load(&s->touch_x);
        } else if (id == RETRO_DEVICE_ID_POINTER_Y) {
            return atomic_load(&s->touch_y);
        }
    }

    return 0;
}

// ---------------------------------------------------------------------------
// Run Loop Worker Thread
// ---------------------------------------------------------------------------

static void* run_loop_thread(void *arg) {
    vf_session_t *s = (vf_session_t *)arg;
    LOGI("Vantafyn native emulation thread started.");

    // Bind EGL context to this worker thread if HW render is active
    if (s->is_hw_render && s->egl_display && s->egl_context) {
        pthread_mutex_lock(&s->window_mutex);
        EGLSurface initial_surf = s->egl_pbuffer_surface;
        if (s->window) {
            EGLint format = 0;
            eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_NATIVE_VISUAL_ID, &format);
            int target_w = (s->last_buffer_w > 0) ? s->last_buffer_w : ((s->av_info.width > 0) ? s->av_info.width : 400);
            int target_h = (s->last_buffer_h > 0) ? s->last_buffer_h : ((s->av_info.height > 0) ? s->av_info.height : 480);
            ANativeWindow_setBuffersGeometry(s->window, (int32_t)target_w, (int32_t)target_h, format);
            s->last_buffer_w = target_w;
            s->last_buffer_h = target_h;
            s->egl_window_surface = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window, NULL);
            if (s->egl_window_surface != EGL_NO_SURFACE) {
                initial_surf = s->egl_window_surface;
                s->active_egl_window = s->window;
                eglMakeCurrent(s->egl_display, s->egl_window_surface, s->egl_window_surface, s->egl_context);
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
                eglSwapBuffers(s->egl_display, s->egl_window_surface);
                glClear(GL_COLOR_BUFFER_BIT);
            }
        }
        s->egl_surface = initial_surf;
        if (eglMakeCurrent(s->egl_display, s->egl_surface, s->egl_surface, s->egl_context)) {
            LOGI("EGL context successfully bound to emulation worker thread");
            if (s->hw_render.context_reset && !s->hw_context_reset_done) {
                LOGI("Calling core hw_render context_reset()...");
                s->hw_render.context_reset();
                s->hw_context_reset_done = true;
            }
        } else {
            LOGE("Failed to make EGL context current on emulation worker thread: 0x%x", eglGetError());
        }
        pthread_mutex_unlock(&s->window_mutex);
    }

    double target_fps = (s->av_info.fps > 10.0) ? s->av_info.fps : 60.0;
    long frame_period_ns = (long)(1e9 / target_fps);

    struct timespec next_frame_time;
    clock_gettime(CLOCK_MONOTONIC, &next_frame_time);

    while (atomic_load(&s->is_thread_running)) {
        if (atomic_exchange(&s->reset_requested, false)) {
            if (s->is_hw_render && s->egl_display && s->egl_context) {
                EGLSurface cur = (s->egl_surface != EGL_NO_SURFACE) ? s->egl_surface : s->egl_pbuffer_surface;
                if (cur != EGL_NO_SURFACE) {
                    eglMakeCurrent(s->egl_display, cur, cur, s->egl_context);
                }
            }
            if (s->retro_reset) {
                LOGI("Executing retro_reset synchronously on emulation worker thread");
                s->retro_reset();
            }
        }

        // Dynamically track SurfaceView / ANativeWindow changes on the render thread
        if (s->is_hw_render) {
            pthread_mutex_lock(&s->window_mutex);
            if (s->active_egl_window != s->window) {
                eglMakeCurrent(s->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                if (s->egl_window_surface != EGL_NO_SURFACE) {
                    eglDestroySurface(s->egl_display, s->egl_window_surface);
                    s->egl_window_surface = EGL_NO_SURFACE;
                }
                s->active_egl_window = s->window;
                if (s->window) {
                    EGLint format = 0;
                    eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_NATIVE_VISUAL_ID, &format);
                    int target_w = (s->last_buffer_w > 0) ? s->last_buffer_w : ((s->av_info.width > 0) ? s->av_info.width : 400);
                    int target_h = (s->last_buffer_h > 0) ? s->last_buffer_h : ((s->av_info.height > 0) ? s->av_info.height : 480);
                    ANativeWindow_setBuffersGeometry(s->window, (int32_t)target_w, (int32_t)target_h, format);
                    s->last_buffer_w = target_w;
                    s->last_buffer_h = target_h;
                    s->egl_window_surface = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window, NULL);
                    if (s->egl_window_surface == EGL_NO_SURFACE) {
                        LOGE("eglCreateWindowSurface failed during surface switch: 0x%x", eglGetError());
                    } else {
                        eglMakeCurrent(s->egl_display, s->egl_window_surface, s->egl_window_surface, s->egl_context);
                        glDisable(GL_SCISSOR_TEST);
                        glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                        glClear(GL_COLOR_BUFFER_BIT);
                        eglSwapBuffers(s->egl_display, s->egl_window_surface);
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                }
                EGLSurface cur_surf = (s->egl_window_surface != EGL_NO_SURFACE)
                    ? s->egl_window_surface
                    : s->egl_pbuffer_surface;
                s->egl_surface = cur_surf;
                if (eglMakeCurrent(s->egl_display, cur_surf, cur_surf, s->egl_context)) {
                    LOGI("EGL surface switched to %s on emulation worker thread",
                         (s->egl_window_surface != EGL_NO_SURFACE) ? "ANativeWindow" : "pbuffer");
                    if (s->hw_render.context_reset && !s->hw_context_reset_done) {
                        LOGI("Invoking hw_render context_reset on new window surface...");
                        s->hw_render.context_reset();
                        s->hw_context_reset_done = true;
                    }
                } else {
                    LOGE("eglMakeCurrent failed during surface switch: 0x%x", eglGetError());
                }
            }

            if (s->active_egl_window_secondary != s->window_secondary) {
                if (s->egl_surface_secondary != EGL_NO_SURFACE) {
                    eglDestroySurface(s->egl_display, s->egl_surface_secondary);
                    s->egl_surface_secondary = EGL_NO_SURFACE;
                }
                s->active_egl_window_secondary = s->window_secondary;
                if (s->window_secondary) {
                    EGLint format = 0;
                    eglGetConfigAttrib(s->egl_display, s->egl_config, EGL_NATIVE_VISUAL_ID, &format);
                    int target_sec_w = (s->last_buffer_secondary_w > 0) ? s->last_buffer_secondary_w : 400;
                    int target_sec_h = (s->last_buffer_secondary_h > 0) ? s->last_buffer_secondary_h : 240;
                    ANativeWindow_setBuffersGeometry(s->window_secondary, (int32_t)target_sec_w, (int32_t)target_sec_h, format);
                    s->egl_surface_secondary = eglCreateWindowSurface(s->egl_display, s->egl_config, s->window_secondary, NULL);
                }
            }
            pthread_mutex_unlock(&s->window_mutex);
        }

        if (atomic_load(&s->is_paused)) {
            usleep(15000); // 15ms sleep when paused
            clock_gettime(CLOCK_MONOTONIC, &next_frame_time);
            continue;
        }

        // For hardware-rendered cores (OpenGL ES), ensure proper framebuffer and surface binding.
        // On physical dual displays, the core renders into s->hw_fbo with the pbuffer surface active.
        // On single displays, the core renders into the main window surface (FBO 0).
        if (s->is_hw_render && s->egl_display) {
            if (s->window_secondary && s->hw_fbo) {
                eglMakeCurrent(s->egl_display, s->egl_pbuffer_surface, s->egl_pbuffer_surface, s->egl_context);
                glBindFramebuffer(GL_FRAMEBUFFER, s->hw_fbo);
                glViewport(0, 0, s->hw_fbo_w, s->hw_fbo_h);
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
            } else if (s->egl_surface != EGL_NO_SURFACE && s->egl_surface != s->egl_pbuffer_surface) {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDisable(GL_SCISSOR_TEST);
                glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
            }
        }

        // Run emulation frame
        if (s->retro_run) {
            s->retro_run();
        }

        // Timing regulation (unless fast-forwarding)
        int ff = s->fast_forward_ratio;
        long period = (ff > 1) ? (frame_period_ns / ff) : frame_period_ns;

        next_frame_time.tv_nsec += period;
        if (next_frame_time.tv_nsec >= 1000000000L) {
            next_frame_time.tv_sec += next_frame_time.tv_nsec / 1000000000L;
            next_frame_time.tv_nsec %= 1000000000L;
        }

        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);

        long diff_ns = (next_frame_time.tv_sec - now.tv_sec) * 1000000000L + (next_frame_time.tv_nsec - now.tv_nsec);
        if (diff_ns > 1000000L && diff_ns < 1000000000L) { // > 1ms and < 1s
            struct timespec req = {0, diff_ns};
            nanosleep(&req, NULL);
        } else if (diff_ns < -50000000L || diff_ns >= 1000000000L) { // Lagged by more than 50ms or clock jumped, resync
            clock_gettime(CLOCK_MONOTONIC, &next_frame_time);
        }
    }

    if (s->is_hw_render && s->egl_display) {
        eglMakeCurrent(s->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    LOGI("Vantafyn native emulation thread stopped.");
    return NULL;
}

// ---------------------------------------------------------------------------
// Public Session API Implementation
// ---------------------------------------------------------------------------

vf_session_t* vf_session_create(const vf_callbacks_t *callbacks) {
    pthread_mutex_lock(&s_session_lock);
    if (s_current_session != NULL) {
        pthread_mutex_unlock(&s_session_lock);
        LOGE("Session already exists. Libretro only permits one active session.");
        return NULL;
    }

    vf_session_t *session = (vf_session_t *)calloc(1, sizeof(vf_session_t));
    if (callbacks) {
        session->callbacks = *callbacks;
    }
    pthread_mutex_init(&session->window_mutex, NULL);
    ring_init(&session->audio_ring, AUDIO_RING_FRAMES);

    atomic_store(&session->is_thread_running, false);
    atomic_store(&session->is_paused, true);
    atomic_store(&session->is_active, false);
    session->fast_forward_ratio = 1;
    session->pixel_format = RETRO_PIXEL_FORMAT_RGB565;
    session->gba_color_correction = true;
    session->gbc_color_correction = true;
    session->gb_palette_mode = 0;
    session->lcd_ghosting = false;
    session->prev_frame_buffer = NULL;
    session->prev_frame_buffer_size = 0;
    session->audio_filtering = true;
    session->crisp_pixels = true;
    session->last_buffer_w = -1;
    session->last_buffer_h = -1;
    session->last_buffer_secondary_w = -1;
    session->last_buffer_secondary_h = -1;
    session->lut_x = NULL;
    session->lut_x_capacity = 0;
    session->lut_x_src_w = 0;
    session->lut_x_dst_w = 0;
    session->lut_y = NULL;
    session->lut_y_capacity = 0;
    session->lut_y_src_h = 0;
    session->lut_y_dst_h = 0;

    session->is_hw_render = false;
    session->hw_context_reset_done = false;
    session->egl_display = EGL_NO_DISPLAY;
    session->egl_context = EGL_NO_CONTEXT;
    session->egl_surface = EGL_NO_SURFACE;
    session->egl_pbuffer_surface = EGL_NO_SURFACE;
    session->egl_window_surface = EGL_NO_SURFACE;
    session->active_egl_window = NULL;

    s_current_session = session;
    pthread_mutex_unlock(&s_session_lock);
    return session;
}

void vf_session_destroy(vf_session_t *session) {
    if (!session) return;
    vf_session_unload(session);

    pthread_mutex_lock(&s_session_lock);
    ring_free(&session->audio_ring);
    pthread_mutex_lock(&session->window_mutex);
    if (session->window) {
        ANativeWindow_release(session->window);
        session->window = NULL;
    }
    if (session->window_secondary) {
        ANativeWindow_release(session->window_secondary);
        session->window_secondary = NULL;
    }
    pthread_mutex_unlock(&session->window_mutex);
    pthread_mutex_destroy(&session->window_mutex);
    if (session->frame_buffer) {
        free(session->frame_buffer);
        session->frame_buffer = NULL;
    }
    if (session->prev_frame_buffer) {
        free(session->prev_frame_buffer);
        session->prev_frame_buffer = NULL;
        session->prev_frame_buffer_size = 0;
    }
    if (session->lut_x) {
        free(session->lut_x);
        session->lut_x = NULL;
        session->lut_x_capacity = 0;
    }
    if (session->lut_y) {
        free(session->lut_y);
        session->lut_y = NULL;
        session->lut_y_capacity = 0;
    }

    if (s_current_session == session) {
        s_current_session = NULL;
    }
    free(session);
    pthread_mutex_unlock(&s_session_lock);
}

static void vf_close_core_handle(void *handle) {
#if !defined(__ANDROID__)
    if (handle) dlclose(handle);
#else
    (void)handle;
#endif
}

static void vf_patch_azahar_core_file(const char *core_path) {
#if defined(__aarch64__)
    if (!core_path) return;
    if (!strstr(core_path, "azahar") && !strstr(core_path, "citra")) return;

    chmod(core_path, 0755);
    FILE *fp = fopen(core_path, "r+b");
    if (!fp) {
        LOGW("vf_patch_azahar_core_file: could not open '%s' for r+b: %s", core_path, strerror(errno));
        return;
    }

    // In Azahar 2126.1.2 (arm64-v8a), FileUtil::IOFile::WriteImpl has an upstream bug under
    // HAVE_LIBRETRO_VFS:
    //   written = rfwrite(data, elem_size, length, m_file) / elem_size;
    // Because rfwrite() in libretro-common ALREADY divides the byte count by elem_size,
    // dividing a second time by elem_size (udiv x21, x0, x21 = 0x9ad50815) causes every write
    // with elem_size > 1 (such as WriteObject<u32>(NativeVersion) or WriteObject<u64>(id))
    // to return 0 and delete the entire 3DS shader disk cache!
    // Replacing 'udiv x21, x0, x21' (0x9ad50815) with 'mov x21, x0' (0xaa0003f5) fixes it.
    const long known_offset = 0x01afe080L;
    uint32_t pair[2] = {0, 0};
    if (fseek(fp, known_offset, SEEK_SET) == 0 && fread(pair, sizeof(uint32_t), 2, fp) == 2) {
        if (pair[0] == 0x9ad50815U && pair[1] == 0xeb1302bfU) {
            uint32_t patched = 0xaa0003f5U; // mov x21, x0
            if (fseek(fp, known_offset, SEEK_SET) == 0 && fwrite(&patched, sizeof(uint32_t), 1, fp) == 1) {
                fflush(fp);
                fsync(fileno(fp));
                LOGI("Patched Azahar IOFile::WriteImpl double-division bug on disk at 0x%lx", known_offset);
                fclose(fp);
                return;
            }
        } else if (pair[0] == 0xaa0003f5U && pair[1] == 0xeb1302bfU) {
            LOGI("Azahar IOFile::WriteImpl double-division bug already patched on disk at 0x%lx", known_offset);
            fclose(fp);
            return;
        }
    }

    // Fallback scan across the binary for the unique 8-byte sequence {udiv x21, x0, x21; cmp x21, x19}
    if (fseek(fp, 0, SEEK_END) == 0) {
        long file_size = ftell(fp);
        fseek(fp, 0, SEEK_SET);
        const size_t chunk_words = 16384;
        uint32_t *buf = (uint32_t *)malloc(chunk_words * sizeof(uint32_t));
        if (buf) {
            long offset = 0;
            while (offset < file_size) {
                if (fseek(fp, offset, SEEK_SET) != 0) break;
                size_t n = fread(buf, sizeof(uint32_t), chunk_words, fp);
                if (n < 2) break;
                for (size_t i = 0; i + 1 < n; i++) {
                    if (buf[i] == 0x9ad50815U && buf[i + 1] == 0xeb1302bfU) {
                        long patch_pos = offset + (long)(i * sizeof(uint32_t));
                        uint32_t patched = 0xaa0003f5U;
                        if (fseek(fp, patch_pos, SEEK_SET) == 0 && fwrite(&patched, sizeof(uint32_t), 1, fp) == 1) {
                            fflush(fp);
                            fsync(fileno(fp));
                            LOGI("Patched Azahar IOFile::WriteImpl double-division bug via scan at 0x%lx", patch_pos);
                        }
                        free(buf);
                        fclose(fp);
                        return;
                    }
                }
                offset += (long)((n - 1) * sizeof(uint32_t));
            }
            free(buf);
        }
    }

    fclose(fp);
#else
    (void)core_path;
#endif
}

static void vf_patch_azahar_core_memory(void *core_handle) {
#if defined(__aarch64__)
    if (!core_handle) return;
    void *sym = dlsym(core_handle, "_ZN8FileUtil6IOFile9WriteImplEPKvmm");
    if (!sym) return;

    uint32_t *ins = (uint32_t *)((uintptr_t)sym + 0x48);
    LOGI("Azahar IOFile::WriteImpl in memory at %p (+0x48 = 0x%08x, +0x4c = 0x%08x)",
         sym, ins[0], ins[1]);
    if (ins[0] == 0x9ad50815U && ins[1] == 0xeb1302bfU) {
        long page_size = sysconf(_SC_PAGESIZE);
        if (page_size <= 0) page_size = 4096;
        uintptr_t page_start = (uintptr_t)ins & ~((uintptr_t)page_size - 1);
        if (mprotect((void *)page_start, (size_t)page_size, PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
            ins[0] = 0xaa0003f5U; // mov x21, x0
            __builtin___clear_cache((char *)ins, (char *)(ins + 1));
            mprotect((void *)page_start, (size_t)page_size, PROT_READ | PROT_EXEC);
            LOGI("Patched Azahar IOFile::WriteImpl in memory -> 0x%08x", ins[0]);
        } else {
            LOGE("mprotect failed on Azahar IOFile::WriteImpl at %p: %s", (void *)page_start, strerror(errno));
        }
    }
#else
    (void)core_handle;
#endif
}

vf_result_t vf_session_load_game(
    vf_session_t *session,
    const char *core_path,
    const char *rom_path,
    const char *system_dir,
    const char *save_dir,
    vf_av_info_t *out_av_info
) {
    if (!session || !core_path || !rom_path) return VF_ERR_CORE_LOAD;
    LOGI("Loading core '%s' with ROM '%s'", core_path, rom_path);

    strncpy(session->system_directory, system_dir ? system_dir : "", sizeof(session->system_directory) - 1);
    strncpy(session->save_directory, save_dir ? save_dir : "", sizeof(session->save_directory) - 1);

    // Patch Azahar's IOFile::WriteImpl double-division bug on disk before loading the shared library
    vf_patch_azahar_core_file(core_path);

    // Open dynamic core library
    session->core_handle = dlopen(core_path, RTLD_NOW | RTLD_LOCAL);
    if (!session->core_handle) {
        LOGE("dlopen failed on core '%s': %s", core_path, dlerror());
        return VF_ERR_CORE_LOAD;
    }

    vf_patch_azahar_core_memory(session->core_handle);

    // Resolve required libretro symbols
    #define RESOLVE(name) \
        session->name = dlsym(session->core_handle, #name); \
        if (!session->name) { \
            LOGE("Missing required symbol: %s", #name); \
            vf_close_core_handle(session->core_handle); \
            session->core_handle = NULL; \
            return VF_ERR_SYMBOLS_MISSING; \
        }

    RESOLVE(retro_init)
    RESOLVE(retro_deinit)
    RESOLVE(retro_api_version)
    RESOLVE(retro_get_system_info)
    RESOLVE(retro_get_system_av_info)
    RESOLVE(retro_set_environment)
    RESOLVE(retro_set_video_refresh)
    RESOLVE(retro_set_audio_sample)
    RESOLVE(retro_set_audio_sample_batch)
    RESOLVE(retro_set_input_poll)
    RESOLVE(retro_set_input_state)
    RESOLVE(retro_set_controller_port_device)
    RESOLVE(retro_reset)
    RESOLVE(retro_run)
    RESOLVE(retro_serialize_size)
    RESOLVE(retro_serialize)
    RESOLVE(retro_unserialize)
    RESOLVE(retro_load_game)
    RESOLVE(retro_unload_game)
    RESOLVE(retro_get_memory_data)
    RESOLVE(retro_get_memory_size)
    #undef RESOLVE

    // Initialize core hooks
    session->retro_set_environment(core_environment);
    session->retro_set_video_refresh(core_video_refresh);
    session->retro_set_audio_sample(core_audio_sample);
    session->retro_set_audio_sample_batch(core_audio_sample_batch);
    session->retro_set_input_poll(core_input_poll);
    session->retro_set_input_state(core_input_state);

    session->retro_init();

    // Query core system info to check if full file path is required directly
    struct retro_system_info sys_info;
    memset(&sys_info, 0, sizeof(sys_info));
    session->retro_get_system_info(&sys_info);
    bool need_fullpath = sys_info.need_fullpath;

    struct retro_game_info game_info = {
        .path = rom_path,
        .data = NULL,
        .size = 0,
        .meta = NULL
    };

    if (!need_fullpath) {
        // Read ROM file into memory for small cartridge systems (GB, GBC, GBA, NES, SNES)
        FILE *f = fopen(rom_path, "rb");
        if (!f) {
            LOGE("Failed to open ROM file: %s", rom_path);
            session->retro_deinit();
            vf_close_core_handle(session->core_handle);
            session->core_handle = NULL;
            return VF_ERR_ROM_READ;
        }

        fseek(f, 0, SEEK_END);
        long rom_size = ftell(f);
        fseek(f, 0, SEEK_SET);

        void *rom_data = malloc(rom_size);
        if (!rom_data) {
            fclose(f);
            session->retro_deinit();
            vf_close_core_handle(session->core_handle);
            session->core_handle = NULL;
            return VF_ERR_OOM;
        }

        if (fread(rom_data, 1, rom_size, f) != (size_t)rom_size) {
            fclose(f);
            free(rom_data);
            session->retro_deinit();
            vf_close_core_handle(session->core_handle);
            session->core_handle = NULL;
            return VF_ERR_ROM_READ;
        }
        fclose(f);

        session->rom_data = rom_data;
        session->rom_size = (size_t)rom_size;
        game_info.data = session->rom_data;
        game_info.size = session->rom_size;
    } else {
        LOGI("Core specifies need_fullpath=true (%s). Passing ROM path without buffering into RAM: %s", 
             sys_info.library_name ? sys_info.library_name : "core", rom_path);
        session->rom_data = NULL;
        session->rom_size = 0;
    }

    bool loaded = session->retro_load_game(&game_info);

    if (!loaded) {
        LOGE("retro_load_game rejected ROM");
        if (session->is_hw_render && session->egl_display) {
            eglMakeCurrent(session->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            if (session->egl_pbuffer_surface != EGL_NO_SURFACE) {
                eglDestroySurface(session->egl_display, session->egl_pbuffer_surface);
                session->egl_pbuffer_surface = EGL_NO_SURFACE;
            }
            if (session->egl_context != EGL_NO_CONTEXT) {
                eglDestroyContext(session->egl_display, session->egl_context);
                session->egl_context = EGL_NO_CONTEXT;
            }
            eglTerminate(session->egl_display);
            session->egl_display = EGL_NO_DISPLAY;
            session->is_hw_render = false;
        }
        if (session->rom_data) {
            free(session->rom_data);
            session->rom_data = NULL;
            session->rom_size = 0;
        }
        session->retro_deinit();
        vf_close_core_handle(session->core_handle);
        session->core_handle = NULL;
        return VF_ERR_CONTENT_FAILED;
    }

    if (session->is_hw_render && session->egl_display) {
        // Release context from loader thread so emulation worker thread can bind it
        eglMakeCurrent(session->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    // Retrieve AV info
    struct retro_system_av_info av;
    session->retro_get_system_av_info(&av);

    session->av_info.width = (int)av.geometry.base_width;
    session->av_info.height = (int)av.geometry.base_height;
    session->av_info.aspect_ratio = (av.geometry.aspect_ratio > 0.0) 
        ? av.geometry.aspect_ratio 
        : ((double)av.geometry.base_width / (double)av.geometry.base_height);
    session->av_info.fps = av.timing.fps;
    session->av_info.sample_rate = av.timing.sample_rate;

    if (out_av_info) {
        *out_av_info = session->av_info;
    }

    // Set joypad device on port 0 (standard RetroPad buttons)
    session->retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);

    atomic_store(&session->is_active, true);
    LOGI("Game loaded successfully. Resolution: %dx%d, FPS: %.1f, Audio: %.0fHz",
         session->av_info.width, session->av_info.height, session->av_info.fps, session->av_info.sample_rate);

    return VF_OK;
}

void vf_session_unload(vf_session_t *session) {
    if (!session || !session->core_handle) return;

    if (atomic_load(&session->is_thread_running)) {
        atomic_store(&session->is_thread_running, false);
        pthread_join(session->thread, NULL);
    }

    if (session->is_hw_render && session->egl_display && session->egl_context) {
        // Hardware-accelerated cores (OpenGL ES) require an active GL context
        // during retro_unload_game() and retro_deinit() to delete GPU shaders, textures, and FBOs.
        EGLSurface surf = (session->egl_surface != EGL_NO_SURFACE) 
            ? session->egl_surface 
            : session->egl_pbuffer_surface;
        eglMakeCurrent(session->egl_display, surf, surf, session->egl_context);
    }

    if (session->retro_unload_game) session->retro_unload_game();
    if (session->retro_deinit) session->retro_deinit();

    if (session->is_hw_render) {
        if (session->egl_display && session->egl_context) {
            eglMakeCurrent(session->egl_display, session->egl_pbuffer_surface, session->egl_pbuffer_surface, session->egl_context);
            if (session->hw_fbo) {
                glDeleteFramebuffers(1, &session->hw_fbo);
                glDeleteTextures(1, &session->hw_texture);
                glDeleteRenderbuffers(1, &session->hw_depth_rb);
                session->hw_fbo = 0;
                session->hw_texture = 0;
                session->hw_depth_rb = 0;
                session->hw_fbo_w = 0;
                session->hw_fbo_h = 0;
            }
            eglMakeCurrent(session->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            if (session->egl_window_surface != EGL_NO_SURFACE) {
                eglDestroySurface(session->egl_display, session->egl_window_surface);
                session->egl_window_surface = EGL_NO_SURFACE;
            }
            if (session->egl_surface_secondary != EGL_NO_SURFACE) {
                eglDestroySurface(session->egl_display, session->egl_surface_secondary);
                session->egl_surface_secondary = EGL_NO_SURFACE;
            }
            if (session->egl_pbuffer_surface != EGL_NO_SURFACE) {
                eglDestroySurface(session->egl_display, session->egl_pbuffer_surface);
                session->egl_pbuffer_surface = EGL_NO_SURFACE;
            }
            eglDestroyContext(session->egl_display, session->egl_context);
            session->egl_context = EGL_NO_CONTEXT;
            eglTerminate(session->egl_display);
            session->egl_display = EGL_NO_DISPLAY;
        }
        session->is_hw_render = false;
        session->active_egl_window = NULL;
        session->active_egl_window_secondary = NULL;
        session->hw_context_reset_done = false;
        vf_gl_cache_cleanup();
    }

    if (session->rom_data) {
        free(session->rom_data);
        session->rom_data = NULL;
        session->rom_size = 0;
    }

    // On Android, calling dlclose() on heavy C++ cores with JIT/TLS (like Citra)
    // frequently causes fatal SIGSEGV in bionic libc due to unmapped memory and static destructors.
    vf_close_core_handle(session->core_handle);
    session->core_handle = NULL;
    atomic_store(&session->is_active, false);
}

void vf_session_start(vf_session_t *session) {
    if (!session || !session->core_handle) return;
    if (!atomic_load(&session->is_thread_running)) {
        atomic_store(&session->is_thread_running, true);
        atomic_store(&session->is_paused, false);
        pthread_create(&session->thread, NULL, run_loop_thread, session);
    }
}

void vf_session_pause(vf_session_t *session) {
    if (session) atomic_store(&session->is_paused, true);
}

void vf_session_resume(vf_session_t *session) {
    if (session) atomic_store(&session->is_paused, false);
}

void vf_session_reset(vf_session_t *session) {
    if (session) {
        atomic_store(&session->reset_requested, true);
        atomic_store(&session->is_paused, false);
    }
}

bool vf_session_is_running(const vf_session_t *session) {
    return session && atomic_load(&session->is_thread_running) && !atomic_load(&session->is_paused);
}

void vf_session_set_window(vf_session_t *session, void *native_window) {
    if (!session) return;
    pthread_mutex_lock(&session->window_mutex);
    if (session->window) {
        ANativeWindow_release(session->window);
    }
    session->window = (ANativeWindow *)native_window;
    if (session->window) {
        ANativeWindow_acquire(session->window);
    }
    session->last_buffer_w = -1;
    session->last_buffer_h = -1;
    pthread_mutex_unlock(&session->window_mutex);
}

void vf_session_set_secondary_window(vf_session_t *session, void *native_window) {
    if (!session) return;
    pthread_mutex_lock(&session->window_mutex);
    if (session->window_secondary) {
        ANativeWindow_release(session->window_secondary);
    }
    session->window_secondary = (ANativeWindow *)native_window;
    if (session->window_secondary) {
        ANativeWindow_acquire(session->window_secondary);
    }
    session->last_buffer_secondary_w = -1;
    session->last_buffer_secondary_h = -1;
    pthread_mutex_unlock(&session->window_mutex);
}

void vf_session_set_dual_screen_swap(vf_session_t *session, bool swap) {
    if (!session) return;
    pthread_mutex_lock(&session->window_mutex);
    session->swap_dual_screens = swap;
    session->last_buffer_w = -1;
    session->last_buffer_h = -1;
    session->last_buffer_secondary_w = -1;
    session->last_buffer_secondary_h = -1;
    if (session->is_hw_render && session->egl_display) {
        if (session->egl_window_surface != EGL_NO_SURFACE) {
            eglDestroySurface(session->egl_display, session->egl_window_surface);
            session->egl_window_surface = EGL_NO_SURFACE;
        }
        if (session->egl_surface_secondary != EGL_NO_SURFACE) {
            eglDestroySurface(session->egl_display, session->egl_surface_secondary);
            session->egl_surface_secondary = EGL_NO_SURFACE;
        }
    }
    pthread_mutex_unlock(&session->window_mutex);
}

size_t vf_session_read_audio(vf_session_t *session, int16_t *buffer, size_t sample_frames) {
    if (!session) return 0;
    return ring_read_samples(&session->audio_ring, buffer, sample_frames);
}

void vf_session_set_input_mask(vf_session_t *session, int port, uint32_t mask) {
    if (session && port >= 0 && port < VF_MAX_PORTS) {
        atomic_store(&session->input_masks[port], mask);
    }
}

void vf_session_set_analog(vf_session_t *session, int port, int index, int id, int16_t value) {
    if (session && port >= 0 && port < VF_MAX_PORTS && index >= 0 && index <= 1 && id >= 0 && id <= 1) {
        atomic_store(&session->analog_state[port][index][id], value);
    }
}

void vf_session_set_touch_state(vf_session_t *session, int16_t x, int16_t y, bool pressed) {
    if (session) {
        atomic_store(&session->touch_x, x);
        atomic_store(&session->touch_y, y);
        atomic_store(&session->touch_pressed, pressed);
    }
}

bool vf_session_save_sram(vf_session_t *session, const char *path) {
    if (!session || !session->retro_get_memory_data) return false;
    size_t size = session->retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    void *data = session->retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (!data || size == 0) return false;

    FILE *f = fopen(path, "wb");
    if (!f) return false;
    size_t written = fwrite(data, 1, size, f);
    fclose(f);
    return written == size;
}

bool vf_session_load_sram(vf_session_t *session, const char *path) {
    if (!session || !session->retro_get_memory_data) return false;
    size_t size = session->retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    void *data = session->retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (!data || size == 0) return false;

    FILE *f = fopen(path, "rb");
    if (!f) return false;
    size_t read_bytes = fread(data, 1, size, f);
    fclose(f);
    return read_bytes > 0;
}

size_t vf_session_get_state_size(vf_session_t *session) {
    return (session && session->retro_serialize_size) ? session->retro_serialize_size() : 0;
}

bool vf_session_save_state(vf_session_t *session, void *data, size_t size) {
    return (session && session->retro_serialize) ? session->retro_serialize(data, size) : false;
}

bool vf_session_load_state(vf_session_t *session, const void *data, size_t size) {
    return (session && session->retro_unserialize) ? session->retro_unserialize(data, size) : false;
}

void vf_session_set_option(vf_session_t *session, const char *key, const char *value) {
    if (!session || !key || !value) return;
    if (strcmp(key, "gba_color_correction") == 0) {
        session->gba_color_correction = (strcmp(value, "enabled") == 0 || strcmp(value, "true") == 0);
        LOGI("GBA color correction: %s", session->gba_color_correction ? "enabled" : "disabled");
    }
    if (strcmp(key, "gbc_color_correction") == 0) {
        session->gbc_color_correction = (strcmp(value, "enabled") == 0 || strcmp(value, "true") == 0);
        LOGI("GBC color correction: %s", session->gbc_color_correction ? "enabled" : "disabled");
    }
    if (strcmp(key, "gb_palette") == 0) {
        if (strcmp(value, "dmg") == 0) session->gb_palette_mode = 1;
        else if (strcmp(value, "pocket") == 0) session->gb_palette_mode = 2;
        else session->gb_palette_mode = 0;
        LOGI("GB palette mode set to: %d (%s)", session->gb_palette_mode, value);
    }
    if (strcmp(key, "lcd_ghosting") == 0) {
        session->lcd_ghosting = (strcmp(value, "enabled") == 0 || strcmp(value, "true") == 0);
        LOGI("LCD ghosting: %s", session->lcd_ghosting ? "enabled" : "disabled");
    }
    if (strcmp(key, "audio_filtering") == 0) {
        session->audio_filtering = (strcmp(value, "enabled") == 0 || strcmp(value, "true") == 0);
        LOGI("Audio filtering: %s", session->audio_filtering ? "enabled" : "disabled");
    }
    if (strcmp(key, "video_filter") == 0) {
        pthread_mutex_lock(&session->window_mutex);
        session->crisp_pixels = (strcmp(value, "smooth") != 0);
        session->last_buffer_w = -1;
        session->last_buffer_h = -1;
        session->last_buffer_secondary_w = -1;
        session->last_buffer_secondary_h = -1;
        pthread_mutex_unlock(&session->window_mutex);
        LOGI("Video filter set to: %s (crisp=%d)", value, session->crisp_pixels);
    }
    for (int i = 0; i < session->option_count; i++) {
        if (strcmp(session->options[i].key, key) == 0) {
            strncpy(session->options[i].value, value, sizeof(session->options[i].value) - 1);
            session->options[i].value[sizeof(session->options[i].value) - 1] = '\0';
            atomic_store(&session->options_updated, true);
            LOGI("Updated core option: %s = %s", key, value);
            return;
        }
    }
    if (session->option_count < VF_MAX_OPTIONS) {
        int idx = session->option_count;
        strncpy(session->options[idx].key, key, sizeof(session->options[idx].key) - 1);
        session->options[idx].key[sizeof(session->options[idx].key) - 1] = '\0';
        strncpy(session->options[idx].value, value, sizeof(session->options[idx].value) - 1);
        session->options[idx].value[sizeof(session->options[idx].value) - 1] = '\0';
        session->option_count++;
        atomic_store(&session->options_updated, true);
        LOGI("Set core option: %s = %s", key, value);
    }
}

void vf_session_set_fast_forward(vf_session_t *session, int speed_ratio) {
    if (session) {
        session->fast_forward_ratio = (speed_ratio > 1) ? speed_ratio : 1;
    }
}
