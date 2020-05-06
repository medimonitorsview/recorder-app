#include <jni.h>
#include <android/log.h>
#include "ImageEnhance.h"
#include "Tracker.h"
#include <opencv2/opencv.hpp>


 struct JniIntArray {

     jint *bufferPtr= nullptr;
     jsize lengthOfArray;
     JNIEnv *env;
     jintArray array;

     JniIntArray(JNIEnv *env, jintArray array) {
         this->env = env;
         this->array = array;
         this->bufferPtr = env->GetIntArrayElements(this->array, NULL);
         this->lengthOfArray = env->GetArrayLength(this->array);
     }

     ~JniIntArray() {
         if (bufferPtr!= nullptr) {
             env->ReleaseIntArrayElements(array, bufferPtr, JNI_COMMIT);
             bufferPtr = nullptr;
         }
     }


 };


mmv::Segment get_segment(JNIEnv* env, jobject object) {
    auto object_class = env->GetObjectClass(object);
    auto top = env->GetIntField(object, env->GetFieldID(object_class, "top", "I"));
    auto left = env->GetIntField(object, env->GetFieldID(object_class, "left", "I"));
    auto bottom = env->GetIntField(object, env->GetFieldID(object_class, "bottom", "I"));
    auto right = env->GetIntField(object, env->GetFieldID(object_class, "right", "I"));

    jstring string = (jstring) env->GetObjectField(object, env->GetFieldID(object_class, "value",
                                                                      "Ljava/lang/String;"));
    std::string value;
    if (string != nullptr){
        const char *rawString = env->GetStringUTFChars(string, 0);
        if (rawString) {
            value = rawString;
        }
        env->ReleaseStringUTFChars(string, rawString);
    }
    return {{cv::Point2i(left, top), cv::Point2i(right, bottom)}, value};
}

cv::Point get_point(JNIEnv* env, jobject object) {
    auto object_class = env->GetObjectClass(object);
    auto x = env->GetDoubleField(object, env->GetFieldID(object_class, "x", "D"));
    auto y = env->GetDoubleField(object, env->GetFieldID(object_class, "y", "D"));
    return cv::Point{int(x), int(y)};
}

jobject get_segment(JNIEnv* env, jclass object_class, mmv::Segment segment) {
    auto default_ctor = env->GetMethodID(object_class,"<init>","()V");
    auto segment_object = env->NewObject(object_class,default_ctor);
    env->SetIntField(segment_object, env->GetFieldID(object_class, "top", "I"),segment.rect.tl().y);
    env->SetIntField(segment_object, env->GetFieldID(object_class, "left", "I"),segment.rect.tl().x);
    env->SetIntField(segment_object, env->GetFieldID(object_class, "bottom", "I"),segment.rect.br().y);
    env->SetIntField(segment_object, env->GetFieldID(object_class, "right", "I"),segment.rect.br().x);
    jstring string = env->NewStringUTF(segment.value.c_str());;
    env->SetObjectField(segment_object, env->GetFieldID(object_class, "value","Ljava/lang/String;"),(jobject )string);
    return segment_object;
}




mmv::Segments get_segments(JNIEnv* env, jobjectArray segmentsArray) {
    int count = env->GetArrayLength(segmentsArray);
    mmv::Segments segments;
    for (int i=0; i<count; i++) {
        segments.push_back(get_segment(env,env->GetObjectArrayElement(segmentsArray, i)));
    }
    return segments;
};

std::vector<cv::Point> get_points(JNIEnv* env, jobjectArray pointsArray) {
    int count = env->GetArrayLength(pointsArray);
    std::vector<cv::Point> points;
    for (int i=0; i<count; i++) {
        points.push_back(get_point(env,env->GetObjectArrayElement(pointsArray, i)));
    }
    return points;
};

jobjectArray get_segments(JNIEnv* env, const mmv::Segments& segments) {
    auto object_class = env->FindClass("org/mdeimonitorsview/android/recorder/classes/Segment");
    auto default_ctor = env->GetMethodID(object_class,"<init>","()V");
    auto segment_object = env->NewObject(object_class,default_ctor);
    jobjectArray objectArray = env->NewObjectArray(segments.size(), object_class, segment_object);
    for (int i=0; i<segments.size(); i++) {
        env->SetObjectArrayElement(objectArray,i,get_segment(env,object_class,segments[i]));
    }
    return objectArray;
};

extern "C"
JNIEXPORT void JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_SegmentProcess(JNIEnv *env, jobject thiz,
                                                                       jlong mat_addr, jobjectArray segmentsArray,
                                                                       jboolean expand) {
    cv::setNumThreads(1);
    cv::Mat& mat =  *(cv::Mat *) mat_addr;
    cv::Mat_ <cv::Vec3b> rgb;
    cv::cvtColor(mat, rgb,cv::COLOR_RGBA2RGB);
    auto pre_data = mat.data;
    auto segments = get_segments(env,segmentsArray);
    for(int i=0;i<segments.size();++i) {
        mmv::SegmentProcess(rgb, segments.tl[i].y,segments.tl[i].x,segments.br[i].y,segments.br[i].x, expand);
    }
    cv::cvtColor(rgb,mat, cv::COLOR_RGB2RGBA);
    if (mat.data!=pre_data){
        throw std::runtime_error("Output written to wrong location");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_GlobalProcess(JNIEnv *env, jobject thiz,
                                                                      jlong mat_addr, jobjectArray screen_corners, jfloat power,
                                                                      jfloat invert_threshold) {
    cv::setNumThreads(1);
    cv::Mat& mat =  *(cv::Mat *) mat_addr;
    cv::Mat_ <cv::Vec3b> rgb;
    cv::cvtColor(mat, rgb,cv::COLOR_RGBA2RGB);
    auto pre_data = mat.data;
    auto points = get_points(env,screen_corners);
    mmv::GlobalProcess(rgb, points, power, invert_threshold);
    cv::cvtColor(rgb,mat, cv::COLOR_RGB2RGBA);
    if (mat.data!=pre_data){
        throw std::runtime_error("Output written to wrong location");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_SetMarkedImage(JNIEnv *env, jobject thiz,
                                                                       jint image_id,
                                                                       jobjectArray segments_array) {

    auto segments = get_segments(env,segments_array);
    mmv::get_tracker().set_marked_image(segments,image_id);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_GetMarkedImageSegments(JNIEnv *env, jobject thiz,
                                                                       jint image_id,
                                                                       jobjectArray segments_array) {
    return get_segments(env, mmv::get_tracker().marked_image_segments);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_Track(JNIEnv *env, jobject thiz,
                                                              jint image_id, jobjectArray segments_array,
                                                              jint min_matches, jint max_distance) {
    auto segments = get_segments(env,segments_array);
    mmv::get_tracker().track(cv::Mat3b(),segments,image_id,min_matches,max_distance);
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_GetRefImage(JNIEnv *env, jobject thiz) {
    return  mmv::get_tracker().ref_image_index;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_GetMarkedImage(JNIEnv *env, jobject thiz) {
    return  mmv::get_tracker().marked_image_index;
}

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_GetTransformation(JNIEnv *env,
                                                                          jobject thiz) {
    jdoubleArray transformation = env->NewDoubleArray(9);
    auto mat = mmv::get_tracker().marked_to_cur_transform;
    //inefficent way!
    double trans[9];
    trans[0] = mat.at<double>(0,0);
    trans[1] = mat.at<double>(0,1);
    trans[2] = mat.at<double>(0,2);
    trans[3] = mat.at<double>(1,0);
    trans[4] = mat.at<double>(1,1);
    trans[5] = mat.at<double>(1,2);
    trans[6] = mat.at<double>(2,0);
    trans[7] = mat.at<double>(2,1);
    trans[8] = mat.at<double>(2,2);
    env->SetDoubleArrayRegion(transformation,0,9,trans);
    return transformation;
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_mdeimonitorsview_android_recorder_ImageEnhance_TransformSegments(JNIEnv *env, jobject thiz,
                                                                  jobjectArray segments) {
    return get_segments(env,mmv::get_tracker().transform(get_segments(env,segments)));
}