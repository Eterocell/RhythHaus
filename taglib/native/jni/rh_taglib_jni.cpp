#include "rh_taglib.h"

#include <cstdlib>
#include <cstring>
#include <jni.h>

namespace {

constexpr int RH_TAGLIB_STATUS_FOUND = 0;
constexpr int RH_TAGLIB_STATUS_UNSUPPORTED = 1;
constexpr int RH_TAGLIB_STATUS_FAILED = 2;

jstring nullable_string(JNIEnv* env, const char* value) {
    return value == nullptr ? nullptr : env->NewStringUTF(value);
}

char* jni_duplicate(const char* value) {
    if (value == nullptr) return nullptr;
    auto size = std::strlen(value) + 1;
    auto* copy = static_cast<char*>(std::malloc(size));
    if (copy != nullptr) {
        std::memcpy(copy, value, size);
    }
    return copy;
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
        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIIIIIII)V"
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
        nullable_string(env, result.metadata.comment),
        static_cast<jint>(result.metadata.year),
        static_cast<jint>(result.metadata.track),
        static_cast<jint>(result.metadata.track_total),
        static_cast<jint>(result.metadata.disc_number),
        static_cast<jint>(result.metadata.disc_total),
        static_cast<jint>(result.metadata.duration_seconds),
        static_cast<jint>(result.metadata.bitrate),
        static_cast<jint>(result.metadata.sample_rate),
        static_cast<jint>(result.metadata.channels)
    );

    rh_taglib_free_result(result);
    return mapped_result;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPropertiesNative(
    JNIEnv* env,
    jobject,
    jstring path
) {
    if (path == nullptr) {
        return nullptr;
    }

    const char* native_path = env->GetStringUTFChars(path, nullptr);
    if (native_path == nullptr) {
        return nullptr;
    }

    RhTagLibProperties props = rh_taglib_read_properties(native_path);
    env->ReleaseStringUTFChars(path, native_path);

    if (props.status != 0) {
        rh_taglib_free_properties(props);
        return nullptr;
    }

    jclass hashmap_class = find_class(env, "java/util/HashMap");
    if (hashmap_class == nullptr) {
        rh_taglib_free_properties(props);
        return nullptr;
    }

    jmethodID init = env->GetMethodID(hashmap_class, "<init>", "()V");
    jmethodID put = env->GetMethodID(
        hashmap_class, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );

    jobject map = env->NewObject(hashmap_class, init);

    for (int i = 0; i < props.property_count; ++i) {
        jstring key = nullable_string(env, props.keys[i]);
        jstring value = nullable_string(env, props.values[i]);
        if (key != nullptr && value != nullptr) {
            env->CallObjectMethod(map, put, key, value);
        }
        if (key != nullptr) env->DeleteLocalRef(key);
        if (value != nullptr) env->DeleteLocalRef(value);
    }

    rh_taglib_free_properties(props);
    return map;
}

extern "C" JNIEXPORT jint JNICALL Java_com_eterocell_rhythhaus_taglib_NativeWriteBridge_writePathNative(
    JNIEnv* env,
    jobject bridgeObj,
    jstring path,
    jobject metaObj
) {
    if (path == nullptr || metaObj == nullptr) {
        return RH_TAGLIB_STATUS_FAILED;
    }

    const char* native_path = env->GetStringUTFChars(path, nullptr);
    if (native_path == nullptr) return RH_TAGLIB_STATUS_FAILED;

    // Find the WriteMeta class
    jclass metaClass = env->GetObjectClass(metaObj);
    if (metaClass == nullptr) {
        env->ReleaseStringUTFChars(path, native_path);
        return RH_TAGLIB_STATUS_FAILED;
    }

    // Build RhTagLibWriteMeta from Java fields
    RhTagLibWriteMeta cMeta{};

    auto getStringField = [&](const char* getterName) -> char* {
        jmethodID method = env->GetMethodID(metaClass, getterName, "()Ljava/lang/String;");
        if (method == nullptr) return nullptr;
        auto jstr = static_cast<jstring>(env->CallObjectMethod(metaObj, method));
        if (jstr == nullptr) return nullptr;
        const char* utf = env->GetStringUTFChars(jstr, nullptr);
        char* copy = utf ? jni_duplicate(utf) : nullptr;
        if (utf) env->ReleaseStringUTFChars(jstr, utf);
        return copy;
    };

    auto getIntField = [&](const char* getterName) -> jint {
        jmethodID method = env->GetMethodID(metaClass, getterName, "()Ljava/lang/Integer;");
        if (method == nullptr) return 0;
        auto intObj = static_cast<jobject>(env->CallObjectMethod(metaObj, method));
        if (intObj == nullptr) return 0;
        jclass intClass = env->FindClass("java/lang/Integer");
        jmethodID intValue = env->GetMethodID(intClass, "intValue", "()I");
        return intValue ? env->CallIntMethod(intObj, intValue) : 0;
    };

    cMeta.title       = getStringField("getTitle");
    cMeta.artist      = getStringField("getArtist");
    cMeta.album       = getStringField("getAlbum");
    cMeta.album_artist = getStringField("getAlbumArtist");
    cMeta.genre       = getStringField("getGenre");
    cMeta.comment     = getStringField("getComment");
    cMeta.year        = getIntField("getYear");
    cMeta.track       = getIntField("getTrackNumber");
    cMeta.track_total = getIntField("getTrackTotal");
    cMeta.disc_number = getIntField("getDiscNumber");
    cMeta.disc_total  = getIntField("getDiscTotal");

    // Read the properties map via getter
    jmethodID propsMethod = env->GetMethodID(metaClass, "getProperties", "()Ljava/util/Map;");
    if (propsMethod != nullptr) {
        auto propsObj = static_cast<jobject>(env->CallObjectMethod(metaObj, propsMethod));
        if (propsObj != nullptr) {
            jclass mapClass = env->GetObjectClass(propsObj);
            jmethodID entrySet = env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");
            auto entrySetObj = static_cast<jobject>(env->CallObjectMethod(propsObj, entrySet));
            if (entrySetObj != nullptr) {
                jclass setClass = env->GetObjectClass(entrySetObj);
                jmethodID toArray = env->GetMethodID(setClass, "toArray", "()[Ljava/lang/Object;");
                auto arrayObj = static_cast<jobjectArray>(env->CallObjectMethod(entrySetObj, toArray));
                if (arrayObj != nullptr) {
                    jint count = env->GetArrayLength(arrayObj);
                    cMeta.property_count = count;
                    cMeta.property_keys = static_cast<char**>(calloc(count, sizeof(char*)));
                    cMeta.property_values = static_cast<char**>(calloc(count, sizeof(char*)));

                    jclass entryClass = env->FindClass("java/util/Map$Entry");
                    jmethodID getKey = env->GetMethodID(entryClass, "getKey", "()Ljava/lang/Object;");
                    jmethodID getValue = env->GetMethodID(entryClass, "getValue", "()Ljava/lang/Object;");

                    for (jint i = 0; i < count; ++i) {
                        auto entry = static_cast<jobject>(env->GetObjectArrayElement(arrayObj, i));
                        auto keyObj = static_cast<jstring>(env->CallObjectMethod(entry, getKey));
                        auto valueObj = static_cast<jstring>(env->CallObjectMethod(entry, getValue));
                        if (keyObj != nullptr) {
                            const char* keyUtf = env->GetStringUTFChars(keyObj, nullptr);
                            cMeta.property_keys[i] = keyUtf ? jni_duplicate(keyUtf) : nullptr;
                            if (keyUtf) env->ReleaseStringUTFChars(keyObj, keyUtf);
                        }
                        if (valueObj != nullptr) {
                            const char* valUtf = env->GetStringUTFChars(valueObj, nullptr);
                            cMeta.property_values[i] = valUtf ? jni_duplicate(valUtf) : nullptr;
                            if (valUtf) env->ReleaseStringUTFChars(valueObj, valUtf);
                        }
                    }
                }
            }
        }
    }

    // Call the native write
    char* errorMsg = nullptr;
    int status = rh_taglib_write_path(native_path, &cMeta, &errorMsg);
    env->ReleaseStringUTFChars(path, native_path);

    // Store error in the bridge object's lastError field
    if (errorMsg != nullptr && bridgeObj != nullptr) {
        jclass bridgeClass = env->GetObjectClass(bridgeObj);
        jfieldID lastErrorField = env->GetFieldID(bridgeClass, "lastError", "Ljava/lang/String;");
        if (lastErrorField != nullptr) {
            jstring errorStr = env->NewStringUTF(errorMsg);
            env->SetObjectField(bridgeObj, lastErrorField, errorStr);
        }
        free(errorMsg);
    }

    rh_taglib_free_write_meta(&cMeta);
    return status;
}
