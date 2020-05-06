#pragma once

#include <opencv2/opencv.hpp>

namespace mmv
{
    void GlobalProcess(cv::Mat_<cv::Vec3b>& image, std::vector<cv::Point> screenCorners, float power = 3.0f, float invert_threshold = 255*0.75f, int skip_row=1,int skip_col=1);
    void SegmentProcess(cv::Mat_<cv::Vec3b>& image, int top, int left, int bottom, int right, bool expand);
} // namespace mcv


