// SPDX-License-Identifier: MIT
// Vantafyn Native Retro Core Engine
// Clean-room C11 Libretro Host and JNI Bridge for Android

#ifndef VANTAFYN_CORE_EMULATOR_H
#define VANTAFYN_CORE_EMULATOR_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define VF_MAX_PORTS 4

typedef enum {
    VF_OK = 0,
    VF_ERR_ALREADY_RUNNING = -1,
    VF_ERR_CORE_LOAD = -2,
    VF_ERR_SYMBOLS_MISSING = -3,
    VF_ERR_ROM_READ = -4,
    VF_ERR_CONTENT_FAILED = -5,
    VF_ERR_AUDIO_INIT = -6,
    VF_ERR_OOM = -7
} vf_result_t;

typedef struct {
    int width;
    int height;
    double aspect_ratio;
    double fps;
    double sample_rate;
    int rotation; // 0, 1, 2, 3
} vf_av_info_t;

typedef struct {
    void *user_data;
    void (*on_frame_rendered)(void *user_data);
    void (*on_geometry_changed)(void *user_data, int width, int height, double aspect);
    void (*on_core_message)(void *user_data, const char *msg);
    void (*on_core_shutdown)(void *user_data);
    void (*on_fatal_error)(void *user_data, const char *err);
} vf_callbacks_t;

typedef struct vf_session vf_session_t;

// Lifecycle
vf_session_t* vf_session_create(const vf_callbacks_t *callbacks);
void vf_session_destroy(vf_session_t *session);

// Core Loading & Execution
vf_result_t vf_session_load_game(
    vf_session_t *session,
    const char *core_path,
    const char *rom_path,
    const char *system_dir,
    const char *save_dir,
    vf_av_info_t *out_av_info
);

void vf_session_unload(vf_session_t *session);
void vf_session_start(vf_session_t *session);
void vf_session_pause(vf_session_t *session);
void vf_session_resume(vf_session_t *session);
void vf_session_reset(vf_session_t *session);
bool vf_session_is_running(const vf_session_t *session);

// Video Blit Target (ANativeWindow)
void vf_session_set_window(vf_session_t *session, void *native_window);
void vf_session_set_secondary_window(vf_session_t *session, void *native_window);
void vf_session_set_dual_screen_swap(vf_session_t *session, bool swap);

// Audio Pull
size_t vf_session_read_audio(vf_session_t *session, int16_t *buffer, size_t sample_frames);

// Input & Touch State
void vf_session_set_input_mask(vf_session_t *session, int port, uint32_t mask);
void vf_session_set_analog(vf_session_t *session, int port, int index, int id, int16_t value);
void vf_session_set_touch_state(vf_session_t *session, int16_t x, int16_t y, bool pressed);

// Memory & Save Management
bool vf_session_save_sram(vf_session_t *session, const char *path);
bool vf_session_load_sram(vf_session_t *session, const char *path);
size_t vf_session_get_state_size(vf_session_t *session);
bool vf_session_save_state(vf_session_t *session, void *data, size_t size);
bool vf_session_load_state(vf_session_t *session, const void *data, size_t size);

// Core Options
void vf_session_set_option(vf_session_t *session, const char *key, const char *value);

// Core Variables / Fast Forward
void vf_session_set_fast_forward(vf_session_t *session, int speed_ratio);

#ifdef __cplusplus
}
#endif

#endif // VANTAFYN_CORE_EMULATOR_H
