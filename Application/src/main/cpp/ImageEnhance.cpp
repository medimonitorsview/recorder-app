
#include "ImageEnhance.h"
#include <numeric>
#include <opencv2/opencv.hpp>
#include <opencv2/core/mat.hpp>
#include <algorithm>
namespace mmv
{
struct Percentiles
{
    int low, middle, high;
};

//https://stackoverflow.com/questions/6989100/sort-points-in-clockwise-order
void clockwise_points(std::vector<cv::Point>& points){
    cv::Point2f centerf = {0.0f,0.0f};
    for (auto& p : points){
        centerf.x+=p.x;
        centerf.y+=p.y;
    }
    centerf.x/=points.size();
    centerf.y/=points.size();
    cv::Point center { int(centerf.x), int(centerf.y)};
    std::sort(points.begin(),points.end(),[center](auto a, auto b){
        if (a.x - center.x >= 0 && b.x - center.x < 0)
            return true;
        if (a.x - center.x < 0 && b.x - center.x >= 0)
            return false;
        if (a.x - center.x == 0 && b.x - center.x == 0) {
            if (a.y - center.y >= 0 || b.y - center.y >= 0)
                return a.y > b.y;
            return b.y > a.y;
        }

        // compute the cross product of vectors (center -> a) x (center -> b)
        int det = (a.x - center.x) * (b.y - center.y) - (b.x - center.x) * (a.y - center.y);
        if (det < 0)
            return true;
        if (det > 0)
            return false;

        // points a and b are on the same line from the center
        // check which point is closer to the center
        int d1 = (a.x - center.x) * (a.x - center.x) + (a.y - center.y) * (a.y - center.y);
        int d2 = (b.x - center.x) * (b.x - center.x) + (b.y - center.y) * (b.y - center.y);
        return d1 > d2;
    });
}


Percentiles computePercentiles(
    const cv::Mat_<cv::Vec3b> &image,
    const cv::Mat_<uint8_t> &mask = cv::Mat_<uint8_t>(),
    int row_skip = 1, int col_skip = 1, float low = 0.05, float middle = 0.5, float high = 0.95,
    int hist_size = 256)
{


    cv::Mat_<int> hist(1, hist_size);
    hist.setTo(0);

    for (int i = 0; i < image.rows; i += row_skip)
    {
        const cv::Vec3b *pixel = image.ptr<cv::Vec3b>(i); // point to first pixel in row
        for (int j = 0; j < image.cols; ++j)
        {
            if (!mask.empty() && mask.at<uint8_t>(i, j) == 0)
            {
                continue;
            }

            for (int c = 0; c < 3; ++c)
            {
                auto location = cv::saturate_cast<uint8_t>(pixel[j][c]);
                hist(0, location)++;
            }
        }
    }
    cv::Mat_<float> cdf(1, hist_size);
    cdf.setTo(0);
    for (int i = 1; i < hist_size; i++)
    {
        float value = (cdf.at<float>(i - 1) + hist.at<int>(i));
        cdf.at<float>(i) = value;
    }
    float hist_count = cdf.at<float>(hist_size - 1);
    cdf /= hist_count;

    // percentiles:
    Percentiles perc = {-1, -1, -1};
    for (int i = 0; i < hist_size; i++)
    {
        if (perc.low == -1 && cdf.at<float>(i) >= low)
        {
            perc.low = 255 * i / float(hist_size);
            continue;
        }
        if (perc.middle == -1 && cdf.at<float>(i) >= middle)
        {
            perc.middle = 255 * i / float(hist_size);
            continue;
        }
        if (perc.high == -1 && cdf.at<float>(i) >= high)
        {
            perc.high = 255 * i / float(hist_size);
            break;
        }
    }
    if (perc.middle == -1){
        perc.middle = 255;
    }
    if (perc.high == -1){
        perc.high = 255;
    }
    return perc;
}

void GlobalProcess(
    cv::Mat_<cv::Vec3b>& image, std::vector<cv::Point> screenCorners,  float power /*= 3.0f*/, float invert_threshold /*= 255*0.75*/,
    int skip_row /*=1*/, int skip_col /*=1*/)
{
    cv::Mat mask;
    clockwise_points(screenCorners);
    if (screenCorners.size()>0){
        mask = cv::Mat::zeros(image.rows, image.cols, CV_8U);
        cv::fillConvexPoly( mask, &screenCorners[0],screenCorners.size() , cv::Scalar(1) );
    }
    Percentiles perc = computePercentiles(image, mask, skip_row, skip_col,0,0.5,1.0);
    if (perc.high == -1)
    {
        // The image is 0. don't do anything
        return;
    }
    bool invert = perc.middle > invert_threshold;
    if (invert)
    {
        //image = cv::Vec3b(255,255,255) - image;
        perc.low = 255 - perc.high;
        perc.high = 255 - perc.low;
    }

    const float range = perc.high - perc.low;
    for (int i = 0; i < image.rows; ++i)
    {
        cv::Vec3b *pixel = image.ptr<cv::Vec3b>(i); // point to first pixel in row
        for (int j = 0; j < image.cols; ++j)
        {
            for (int c = 0; c < 3; ++c)
            {
                auto value = cv::saturate_cast<uint8_t>(pixel[j][c]);
                if (invert){
                    value = 255-value;
                }
                auto clamped = std::max(perc.low, std::min(int(value), perc.high));
                float streched = (clamped - perc.low) / range;
                uint8_t pow_val = cv::saturate_cast<uint8_t>(255.0 * std::pow(streched, power));
                pixel[j][c] = pow_val;
            }
        }
    }
}
void SegmentProcess(cv::Mat_<cv::Vec3b>& image, int top, int left, int bottom, int right,bool expand)
{
    if (expand && bottom>top){
        int expansion = (bottom-top)*0.5; // simple assumption - a number is not wider then 1/2 it's height in mose fonts
        left = std::max(0,left-expansion);
        right = std::min(image.cols, right+expansion);
    }
    if (top >= bottom || left>=right || top<0 || bottom < 0 || right>= image.cols || bottom >= image.rows){
        return;
    }
    cv::Rect roi_rect(cv::Point(left,top),cv::Point(right,bottom));
    if (roi_rect.empty()){
        return;
    }
    cv::Mat_<cv::Vec3b> roi = image(roi_rect);
    cv::Mat_<cv::Vec3f> pixels(roi.rows , roi.cols);
    roi.copyTo(pixels);
    pixels = pixels.reshape(1, roi.rows*roi.cols);
    cv::Mat_<int> labels;
    cv::Mat1f colors;
    auto criteria = cv::TermCriteria(cv::TermCriteria::COUNT + cv::TermCriteria::EPS, 10, 1.0);
    cv::kmeans(pixels, 2, labels, criteria, 1, cv::KMEANS_PP_CENTERS, colors);
    labels = labels.reshape(0, roi.rows);
    float means[2] = {0, 0};
    int sums[2] = {0, 0};
    for (int i = 0; i < roi.rows; i += 1)
    {
        for (int j = 0; j < roi.cols; j += 1)
        {
            if (labels(i,j) == 0)
            {
                means[0] += roi(i, j)[0] + roi(i, j)[1] + roi(i, j)[2];
                sums[0]++;
            }
            else
            {
                means[1] += roi(i, j)[0] + roi(i, j)[1] + roi(i, j)[2];
                sums[1]++;
            }
        }
    }
    means[0] /= sums[0]*3;
    means[1] /= sums[1]*3;
    int label = 1;
    if (means[0] > means[1])
    {
        label = 0;
    }
    cv::Mat_<uint8_t> foreground = labels == label;
    const Percentiles perc = computePercentiles(roi, foreground, 1, 1);
    if (perc.high == -1)
    {
        // The image is 0 in the interesting places. don't do anything
        return;
    }

    const float range = perc.high - perc.low;
    for (int i = 0; i < roi.rows; ++i)
    {
        cv::Vec3b *pixel = roi.ptr<cv::Vec3b>(i);
        for (int j = 0; j < roi.cols; ++j)
        {
            if (foreground.at<uint8_t>(i, j) > 0)
            {
                for (int c = 0; c < 3; ++c)
                {
                    const int& pix = int(pixel[j][c]);
                    float value = std::max(perc.low, std::min(pix, perc.high));
                    value = 255.0f* (value - perc.low) / range;
                    pixel[j][c] = cv::saturate_cast<uint8_t>(value);
                }
            }
            else
            {
                for (int c = 0; c < 3; ++c)
                {
                    pixel[j][c] = 0;
                }
            }
        }
    }
}
} // namespace mmv