// SPDX-License-Identifier: MIT
// Vantafyn JNI Bridge between Kotlin NativeEmulatorEngine and C11 vantafyn_host

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#include "vantafyn_host.h"

#define JNI_CLASS "dev/vantafyn/core/emulator/NativeEmulatorEngine"
#define LOG_TAG "VantafynJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static vf_session_t *g_session = NULL;
static JavaVM *g_jvm = NULL;
static jobject g_engine_obj = NULL;
static jmethodID g_mid_on_frame_rendered = NULL;
static jmethodID g_mid_on_geometry_changed = NULL;
static jmethodID g_mid_on_core_message = NULL;
static jmethodID g_mid_on_core_shutdown = NULL;
static jmethodID g_mid_on_fatal_error = NULL;

static JNIEnv* get_jni_env(void) {
    if (!g_jvm) return NULL;
    JNIEnv *env = NULL;
    jint res = (*g_jvm)->GetEnv(g_jvm, (void **)&env, JNI_VERSION_1_6);
    if (res == JNI_OK) return env;
    if (res == JNI_EDETACHED) {
        JavaVMAttachArgs args = { .version = JNI_VERSION_1_6, .name = "VantafynNativeDaemon", .group = NULL };
        if ((*g_jvm)->AttachCurrentThreadAsDaemon(g_jvm, &env, &args) == JNI_OK) {
            return env;
        }
    }
    return NULL;
}

static void cb_on_frame_rendered(void *user_data) {
    (void)user_data;
    JNIEnv *env = get_jni_env();
    if (env && g_engine_obj && g_mid_on_frame_rendered) {
        (*env)->CallVoidMethod(env, g_engine_obj, g_mid_on_frame_rendered);
    }
}

static void cb_on_geometry_changed(void *user_data, int width, int height, double aspect) {
    (void)user_data;
    JNIEnv *env = get_jni_env();
    if (env && g_engine_obj && g_mid_on_geometry_changed) {
        (*env)->CallVoidMethod(env, g_engine_obj, g_mid_on_geometry_changed, (jint)width, (jint)height, (jdouble)aspect);
    }
}

static void cb_on_core_message(void *user_data, const char *msg) {
    (void)user_data;
    JNIEnv *env = get_jni_env();
    if (env && g_engine_obj && g_mid_on_core_message && msg) {
        jstring jmsg = (*env)->NewStringUTF(env, msg);
        (*env)->CallVoidMethod(env, g_engine_obj, g_mid_on_core_message, jmsg);
        (*env)->DeleteLocalRef(env, jmsg);
    }
}

static void cb_on_core_shutdown(void *user_data) {
    (void)user_data;
    JNIEnv *env = get_jni_env();
    if (env && g_engine_obj && g_mid_on_core_shutdown) {
        (*env)->CallVoidMethod(env, g_engine_obj, g_mid_on_core_shutdown);
    }
}

static void cb_on_fatal_error(void *user_data, const char *err) {
    (void)user_data;
    JNIEnv *env = get_jni_env();
    if (env && g_engine_obj && g_mid_on_fatal_error && err) {
        jstring jerr = (*env)->NewStringUTF(env, err);
        (*env)->CallVoidMethod(env, g_engine_obj, g_mid_on_fatal_error, jerr);
        (*env)->DeleteLocalRef(env, jerr);
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeInit(JNIEnv *env, jobject thiz) {
    if (g_session) {
        vf_session_destroy(g_session);
        g_session = NULL;
    }

    if (g_engine_obj) {
        (*env)->DeleteGlobalRef(env, g_engine_obj);
    }
    g_engine_obj = (*env)->NewGlobalRef(env, thiz);

    jclass cls = (*env)->GetObjectClass(env, thiz);
    g_mid_on_frame_rendered = (*env)->GetMethodID(env, cls, "onFrameRendered", "()V");
    g_mid_on_geometry_changed = (*env)->GetMethodID(env, cls, "onGeometryChanged", "(IID)V");
    g_mid_on_core_message = (*env)->GetMethodID(env, cls, "onCoreMessage", "(Ljava/lang/String;)V");
    g_mid_on_core_shutdown = (*env)->GetMethodID(env, cls, "onCoreShutdown", "()V");
    g_mid_on_fatal_error = (*env)->GetMethodID(env, cls, "onFatalError", "(Ljava/lang/String;)V");

    vf_callbacks_t cbs = {
        .user_data = NULL,
        .on_frame_rendered = cb_on_frame_rendered,
        .on_geometry_changed = cb_on_geometry_changed,
        .on_core_message = cb_on_core_message,
        .on_core_shutdown = cb_on_core_shutdown,
        .on_fatal_error = cb_on_fatal_error
    };

    g_session = vf_session_create(&cbs);
    return (g_session != NULL) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdoubleArray JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeLoad(
    JNIEnv *env,
    jobject thiz,
    jstring j_core_path,
    jstring j_rom_path,
    jstring j_system_dir,
    jstring j_save_dir
) {
    (void)thiz;
    if (!g_session) return NULL;

    const char *core_path = (*env)->GetStringUTFChars(env, j_core_path, NULL);
    const char *rom_path = (*env)->GetStringUTFChars(env, j_rom_path, NULL);
    const char *system_dir = (*env)->GetStringUTFChars(env, j_system_dir, NULL);
    const char *save_dir = (*env)->GetStringUTFChars(env, j_save_dir, NULL);

    vf_av_info_t av;
    vf_result_t res = vf_session_load_game(g_session, core_path, rom_path, system_dir, save_dir, &av);

    (*env)->ReleaseStringUTFChars(env, j_core_path, core_path);
    (*env)->ReleaseStringUTFChars(env, j_rom_path, rom_path);
    (*env)->ReleaseStringUTFChars(env, j_system_dir, system_dir);
    (*env)->ReleaseStringUTFChars(env, j_save_dir, save_dir);

    if (res != VF_OK) {
        return NULL;
    }

    jdoubleArray out = (*env)->NewDoubleArray(env, 5);
    jdouble vals[5] = {
        (jdouble)av.width,
        (jdouble)av.height,
        av.aspect_ratio,
        av.fps,
        av.sample_rate
    };
    (*env)->SetDoubleArrayRegion(env, out, 0, 5, vals);
    return out;
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeStart(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_session) vf_session_start(g_session);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativePause(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_session) vf_session_pause(g_session);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeResume(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_session) vf_session_resume(g_session);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeReset(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_session) vf_session_reset(g_session);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeStop(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    if (g_session) {
        vf_session_unload(g_session);
    }
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeDestroy(JNIEnv *env, jobject thiz) {
    if (g_session) {
        vf_session_destroy(g_session);
        g_session = NULL;
    }
    if (g_engine_obj) {
        (*env)->DeleteGlobalRef(env, g_engine_obj);
        g_engine_obj = NULL;
    }
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetSurface(JNIEnv *env, jobject thiz, jobject surface) {
    (void)thiz;
    if (!g_session) return;
    ANativeWindow *win = surface ? ANativeWindow_fromSurface(env, surface) : NULL;
    vf_session_set_window(g_session, win);
    if (win) {
        ANativeWindow_release(win); // session acquires its own reference
    }
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetSecondarySurface(JNIEnv *env, jobject thiz, jobject surface) {
    (void)thiz;
    if (!g_session) return;
    ANativeWindow *win = surface ? ANativeWindow_fromSurface(env, surface) : NULL;
    vf_session_set_secondary_window(g_session, win);
    if (win) {
        ANativeWindow_release(win); // session acquires its own reference
    }
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetDualScreenSwap(JNIEnv *env, jobject thiz, jboolean swap) {
    (void)thiz;
    if (g_session) {
        vf_session_set_dual_screen_swap(g_session, swap == JNI_TRUE);
    }
}

JNIEXPORT jint JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeReadAudio(JNIEnv *env, jobject thiz, jshortArray buffer, jint offset, jint length_samples) {
    (void)thiz;
    if (!g_session || !buffer || length_samples <= 0) return 0;
    jshort *ptr = (*env)->GetShortArrayElements(env, buffer, NULL);
    if (!ptr) return 0;

    size_t sample_frames = (size_t)(length_samples / 2);
    size_t frames_read = vf_session_read_audio(g_session, (int16_t *)(ptr + offset), sample_frames);

    (*env)->ReleaseShortArrayElements(env, buffer, ptr, 0);
    return (jint)(frames_read * 2);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetInput(JNIEnv *env, jobject thiz, jint port, jint mask) {
    (void)env; (void)thiz;
    if (g_session) vf_session_set_input_mask(g_session, (int)port, (uint32_t)mask);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetTouch(JNIEnv *env, jobject thiz, jshort x, jshort y, jboolean pressed) {
    (void)env; (void)thiz;
    if (g_session) vf_session_set_touch_state(g_session, (int16_t)x, (int16_t)y, (pressed == JNI_TRUE));
}

JNIEXPORT jboolean JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSaveSram(JNIEnv *env, jobject thiz, jstring j_path) {
    (void)thiz;
    if (!g_session || !j_path) return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, j_path, NULL);
    bool ok = vf_session_save_sram(g_session, path);
    (*env)->ReleaseStringUTFChars(env, j_path, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeLoadSram(JNIEnv *env, jobject thiz, jstring j_path) {
    (void)thiz;
    if (!g_session || !j_path) return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, j_path, NULL);
    bool ok = vf_session_load_sram(g_session, path);
    (*env)->ReleaseStringUTFChars(env, j_path, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSaveState(JNIEnv *env, jobject thiz) {
    (void)thiz;
    if (!g_session) return NULL;
    size_t sz = vf_session_get_state_size(g_session);
    if (sz == 0) return NULL;

    jbyteArray arr = (*env)->NewByteArray(env, (jsize)sz);
    jbyte *ptr = (*env)->GetByteArrayElements(env, arr, NULL);
    bool ok = vf_session_save_state(g_session, ptr, sz);
    (*env)->ReleaseByteArrayElements(env, arr, ptr, 0);

    return ok ? arr : NULL;
}

JNIEXPORT jboolean JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeLoadState(JNIEnv *env, jobject thiz, jbyteArray data) {
    (void)thiz;
    if (!g_session || !data) return JNI_FALSE;
    jsize sz = (*env)->GetArrayLength(env, data);
    if (sz == 0) return JNI_FALSE;

    jbyte *ptr = (*env)->GetByteArrayElements(env, data, NULL);
    bool ok = vf_session_load_state(g_session, ptr, (size_t)sz);
    (*env)->ReleaseByteArrayElements(env, data, ptr, JNI_ABORT);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetFastForward(JNIEnv *env, jobject thiz, jint ratio) {
    (void)env; (void)thiz;
    if (g_session) vf_session_set_fast_forward(g_session, (int)ratio);
}

JNIEXPORT void JNICALL
Java_dev_vantafyn_core_emulator_NativeEmulatorEngine_nativeSetOption(JNIEnv *env, jobject thiz, jstring j_key, jstring j_val) {
    (void)thiz;
    if (!g_session || !j_key || !j_val) return;
    const char *key = (*env)->GetStringUTFChars(env, j_key, NULL);
    const char *val = (*env)->GetStringUTFChars(env, j_val, NULL);
    vf_session_set_option(g_session, key, val);
    (*env)->ReleaseStringUTFChars(env, j_key, key);
    (*env)->ReleaseStringUTFChars(env, j_val, val);
}
