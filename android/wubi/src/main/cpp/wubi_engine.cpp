#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "WubiEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int g_scheme = 0;
static bool g_errorCorrection = true;

extern "C" {

JNIEXPORT jobjectArray JNICALL
Java_com_quickspeech_wubi_engine_WubiEngine_nativeSearch(
    JNIEnv* env,
    jobject /* this */,
    jstring code
) {
    const char* codeStr = env->GetStringUTFChars(code, nullptr);
    std::string query(codeStr);
    env->ReleaseStringUTFChars(code, codeStr);

    // Placeholder: return empty results
    // TODO: Implement real wubi dictionary lookup
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(0, stringClass, nullptr);

    LOGI("Search query: %s", query.c_str());
    return result;
}

JNIEXPORT void JNICALL
Java_com_quickspeech_wubi_engine_WubiEngine_nativeSetScheme(
    JNIEnv* /* env */,
    jobject /* this */,
    jint schemeCode
) {
    g_scheme = schemeCode;
    LOGI("Scheme set to: %d", schemeCode);
}

JNIEXPORT void JNICALL
Java_com_quickspeech_wubi_engine_WubiEngine_nativeEnableErrorCorrection(
    JNIEnv* /* env */,
    jobject /* this */,
    jboolean enabled
) {
    g_errorCorrection = enabled;
    LOGI("Error correction: %d", enabled);
}

} // extern "C"
