// SPDX-License-Identifier: MIT
// Vantafyn Native Retro Core Engine
// Clean-room C11 implementation of the libretro frontend host.

#include "vantafyn_host.h"
#include "libretro.h"

#include <android/log.h>
#include <android/native_window.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
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

#define VF_MAX_OPTIONS 32

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
    __android_log_vprint(prio, "MelonDSCore", fmt, va);
    va_end(va);
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
            s->av_info.width = (int)geom->base_width;
            s->av_info.height = (int)geom->base_height;
            s->av_info.aspect_ratio = (geom->aspect_ratio > 0.0) 
                ? geom->aspect_ratio 
                : ((double)geom->base_width / (double)geom->base_height);
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
                var->value = "Default Top-Bottom Screen";
                return true;
            }
            if (strcmp(var->key, "citra_screen_layout") == 0 || strcmp(var->key, "azahar_screen_layout") == 0) {
                var->value = "top_bottom";
                return true;
            }
            if (strcmp(var->key, "citra_use_cpu_jit") == 0 || strcmp(var->key, "azahar_use_cpu_jit") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_is_new_3ds") == 0 || strcmp(var->key, "azahar_is_new_3ds") == 0) {
                var->value = "enabled";
                return true;
            }
            if (strcmp(var->key, "citra_resolution_factor") == 0 || strcmp(var->key, "azahar_resolution_factor") == 0) {
                var->value = "1x (400x240)";
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
    if (!s || !data || width == 0 || height == 0) return;

    pthread_mutex_lock(&s->window_mutex);
    if (!s->window) {
        pthread_mutex_unlock(&s->window_mutex);
        return;
    }

    // Allocate / resize conversion buffer if needed
    size_t needed_pixels = (size_t)width * (size_t)height;
    if (!s->frame_buffer || s->frame_buffer_size < needed_pixels) {
        free(s->frame_buffer);
        s->frame_buffer = (uint32_t *)malloc(needed_pixels * sizeof(uint32_t));
        s->frame_buffer_size = needed_pixels;
    }

    // Convert core frame format to Android RGBA_8888 (with optional GBA/GBC LCD color correction & DMG palette)
    bool cc = s->gba_color_correction || s->gbc_color_correction;
    int pal = s->gb_palette_mode;

    if (s->pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        const uint32_t *src = (const uint32_t *)data;
        size_t src_stride = pitch / sizeof(uint32_t);
        for (unsigned y = 0; y < height; y++) {
            const uint32_t *line = src + y * src_stride;
            uint32_t *dst_line = s->frame_buffer + y * width;
            for (unsigned x = 0; x < width; x++) {
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
            for (unsigned x = 0; x < width; x++) {
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
            for (unsigned x = 0; x < width; x++) {
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

    ANativeWindow *win_top = s->swap_dual_screens ? s->window_secondary : s->window;
    ANativeWindow *win_bottom = s->swap_dual_screens ? s->window : s->window_secondary;

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
            if (!s->crisp_pixels) {
                if (s->last_buffer_w != (int)top_w || s->last_buffer_h != (int)top_h) {
                    ANativeWindow_setBuffersGeometry(win_top, (int32_t)top_w, (int32_t)top_h, WINDOW_FORMAT_RGBA_8888);
                    s->last_buffer_w = (int)top_w;
                    s->last_buffer_h = (int)top_h;
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
                if (s->last_buffer_w != 0 || s->last_buffer_h != 0) {
                    ANativeWindow_setBuffersGeometry(win_top, 0, 0, WINDOW_FORMAT_RGBA_8888);
                    s->last_buffer_w = 0;
                    s->last_buffer_h = 0;
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
            if (!s->crisp_pixels) {
                if (s->last_buffer_secondary_w != (int)bot_w || s->last_buffer_secondary_h != (int)bot_h) {
                    ANativeWindow_setBuffersGeometry(win_bottom, (int32_t)bot_w, (int32_t)bot_h, WINDOW_FORMAT_RGBA_8888);
                    s->last_buffer_secondary_w = (int)bot_w;
                    s->last_buffer_secondary_h = (int)bot_h;
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
                if (s->last_buffer_secondary_w != 0 || s->last_buffer_secondary_h != 0) {
                    ANativeWindow_setBuffersGeometry(win_bottom, 0, 0, WINDOW_FORMAT_RGBA_8888);
                    s->last_buffer_secondary_w = 0;
                    s->last_buffer_secondary_h = 0;
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

    double target_fps = (s->av_info.fps > 10.0) ? s->av_info.fps : 60.0;
    long frame_period_ns = (long)(1e9 / target_fps);

    struct timespec next_frame_time;
    clock_gettime(CLOCK_MONOTONIC, &next_frame_time);

    while (atomic_load(&s->is_thread_running)) {
        if (atomic_exchange(&s->reset_requested, false)) {
            if (s->retro_reset) {
                LOGI("Executing retro_reset synchronously on emulation worker thread");
                s->retro_reset();
            }
        }

        if (atomic_load(&s->is_paused)) {
            usleep(15000); // 15ms sleep when paused
            clock_gettime(CLOCK_MONOTONIC, &next_frame_time);
            continue;
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
        if (diff_ns > 1000000L) { // > 1ms
            struct timespec req = {0, diff_ns};
            nanosleep(&req, NULL);
        } else if (diff_ns < -50000000L) { // Lagged by more than 50ms, resync clock
            clock_gettime(CLOCK_MONOTONIC, &next_frame_time);
        }
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

    // Open dynamic core library
    session->core_handle = dlopen(core_path, RTLD_NOW | RTLD_LOCAL);
    if (!session->core_handle) {
        LOGE("dlopen failed on core '%s': %s", core_path, dlerror());
        return VF_ERR_CORE_LOAD;
    }

    // Resolve required libretro symbols
    #define RESOLVE(name) \
        session->name = dlsym(session->core_handle, #name); \
        if (!session->name) { \
            LOGE("Missing required symbol: %s", #name); \
            dlclose(session->core_handle); \
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

    // Read ROM file into memory
    FILE *f = fopen(rom_path, "rb");
    if (!f) {
        LOGE("Failed to open ROM file: %s", rom_path);
        session->retro_deinit();
        dlclose(session->core_handle);
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
        dlclose(session->core_handle);
        session->core_handle = NULL;
        return VF_ERR_OOM;
    }

    if (fread(rom_data, 1, rom_size, f) != (size_t)rom_size) {
        fclose(f);
        free(rom_data);
        session->retro_deinit();
        dlclose(session->core_handle);
        session->core_handle = NULL;
        return VF_ERR_ROM_READ;
    }
    fclose(f);

    session->rom_data = rom_data;
    session->rom_size = (size_t)rom_size;

    struct retro_game_info game_info = {
        .path = rom_path,
        .data = session->rom_data,
        .size = session->rom_size,
        .meta = NULL
    };

    bool loaded = session->retro_load_game(&game_info);

    if (!loaded) {
        LOGE("retro_load_game rejected ROM");
        if (session->rom_data) {
            free(session->rom_data);
            session->rom_data = NULL;
            session->rom_size = 0;
        }
        session->retro_deinit();
        dlclose(session->core_handle);
        session->core_handle = NULL;
        return VF_ERR_CONTENT_FAILED;
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

    if (session->retro_unload_game) session->retro_unload_game();
    if (session->retro_deinit) session->retro_deinit();

    if (session->rom_data) {
        free(session->rom_data);
        session->rom_data = NULL;
        session->rom_size = 0;
    }

    dlclose(session->core_handle);
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
