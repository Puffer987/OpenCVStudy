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
Java_com_adolf_opencvstudy_BinaryActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_adolf_opencvstudy_BinaryActivity_autoBinary(jlong refMat) {
//    cv::Mat image = cv::imread("../test.bmp", CV_LOAD_IMAGE_GRAYSCALE);
//    if (image.empty()) {
//        std::cout << "read image failure" << std::endl;
//        return -1;
//    }
//
//    // 全局二值化
//    int th = 100;
//    cv::Mat global;
//    cv::threshold(image, global, th, 255, CV_THRESH_BINARY_INV);
//
//
//    // 局部二值化
//
//    int blockSize = 25;
//    int constValue = 10;
//    cv::Mat local;
//    cv::adaptiveThreshold(image, local, 255, CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY_INV,
//                          blockSize, constValue);
//
//
//    cv::imwrite("global.jpg", global);
//    cv::imwrite("local.jpg", local);
//
//    cv::imshow("globalThreshold", global);
//    cv::imshow("localThreshold", local);
//    cv::waitKey(0);
//
//
//    return 0;
//}
