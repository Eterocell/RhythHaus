#include "rh_taglib.h"

#include <jni.h>

namespace {

jstring nullable_string(JNIEnv* env, const char* value) {
    return value == nullptr ? nullptr : env->NewStringUTF(value);
}

jclass find_class(JNIEnv* env, const char* name) {
    jclass clazz = env->FindClass(name);
    return clazz;
}

} // namespace

extern "C" JNIEXPORT jobject JNICALL Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative(
    JNIEnv* env,
    jobject,
    jstring path
) {
    if (path == nullptr) {
        jclass illegal_argument = find_class(env, "java/lang/IllegalArgumentException");
        if (illegal_argument != nullptr) {
            env->ThrowNew(illegal_argument, "path is required");
        }
        return nullptr;
    }

    const char* native_path = env->GetStringUTFChars(path, nullptr);
    if (native_path == nullptr) {
        return nullptr;
    }

    RhTagLibResult result = rh_taglib_read_path(native_path);
    env->ReleaseStringUTFChars(path, native_path);

    jclass result_class = find_class(env, "com/eterocell/rhythhaus/taglib/NativeTagLibReadResult");
    if (result_class == nullptr) {
        rh_taglib_free_result(result);
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
        result_class,
        "<init>",
        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIIII)V"
    );
    if (constructor == nullptr) {
        rh_taglib_free_result(result);
        return nullptr;
    }

    jobject mapped_result = env->NewObject(
        result_class,
        constructor,
        static_cast<jint>(result.status),
        nullable_string(env, result.error_message),
        nullable_string(env, result.metadata.title),
        nullable_string(env, result.metadata.artist),
        nullable_string(env, result.metadata.album),
        nullable_string(env, result.metadata.album_artist),
        nullable_string(env, result.metadata.genre),
        static_cast<jint>(result.metadata.year),
        static_cast<jint>(result.metadata.track),
        static_cast<jint>(result.metadata.duration_seconds),
        static_cast<jint>(result.metadata.bitrate),
        static_cast<jint>(result.metadata.sample_rate),
        static_cast<jint>(result.metadata.channels)
    );

    rh_taglib_free_result(result);
    return mapped_result;
}
