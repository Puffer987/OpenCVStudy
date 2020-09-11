#include <jni.h>
#include <string>
#include <iostream>
//#include <opencv2/opencv.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_com_adolf_opencvstudy_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_adolf_opencvstudy_view_BinaryActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
