#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
  #include "devcom/security.h"
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_devcomjavamobile_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
  std::string hello = "Hello from C++";
  printf("%s\n", hello.c_str());
  __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile", "Printing FROM C++");
  return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_devcomjavamobile_MainActivity_generateKeys(JNIEnv *env, jobject thiz) {
  generate_key_pair();
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_devcomjavamobile_network_TunnelService_createFingerprint(JNIEnv *env, jobject thiz) {
  std::string fingerprintstr =  create_fingerprint_forcpp();
  return env->NewStringUTF(fingerprintstr.c_str());
}