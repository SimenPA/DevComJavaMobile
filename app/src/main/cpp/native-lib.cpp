#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
  #include "devcom/security.h"
  #include "devcom/control_traffic.h"
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

extern "C"
JNIEXPORT jcharArray JNICALL
Java_com_example_devcomjavamobile_network_P2P_control_1packet_1encrypt(JNIEnv *env, jobject thiz, jcharArray packet, jcharArray payload, jstring keyfile) {

  int packetLen = env->GetArrayLength(packet);
  auto *cPacket = new unsigned char[packetLen];
  env->SetCharArrayRegion(packet, 0, packetLen, (jchar *) cPacket);

  int payloadLen = env->GetArrayLength(payload);
  auto *cPayload = new unsigned char[payloadLen];
  env->SetCharArrayRegion(payload, 0, payloadLen, (jchar *) cPayload);

  jboolean isCopy;
  const char *convertedValue = env->GetStringUTFChars(keyfile, &isCopy);

  int bytes_encrypted = control_packet_encrypt(cPacket, cPayload, convertedValue);

  jcharArray encryptedPacket = env->NewCharArray(2071);
  env->SetCharArrayRegion(encryptedPacket, 0, 28, (jchar*)cPayload);

  return encryptedPacket;
}extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_devcomjavamobile_MainActivity_createFingerprint(JNIEnv *env, jobject thiz) {
  std::string fingerprintstr =  create_fingerprint_forcpp();
  return env->NewStringUTF(fingerprintstr.c_str());
}