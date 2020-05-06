#include "Tracker.h"
using namespace std::string_literals;

#ifdef __ANDROID__

#include <android/log.h>
#include <sstream>

struct AndroidLog
{
   std::stringstream m_ss;
   int logLevel;

   AndroidLog(int _logLevel):logLevel(_logLevel){}
   ~AndroidLog()
   {
      __android_log_print(logLevel,"TRACKER","%s", m_ss.str().c_str());
   }

   template<typename T> AndroidLog& operator<<(T const& val)
   {
      m_ss << val;
      return *this;
   }
};

#define LOG(LOG_LEVEL) AndroidLog(ANDROID_LOG_##LOG_LEVEL) << __FUNCTION__ << ":" << __LINE__ << " : "
#else
#define LOG(LOG_LEVEL) std::cout << " " << __FUNCTION__ << ":" << __LINE__ << " : "
#endif
namespace mmv{
    Tracker& get_tracker(){
        static Tracker tracker;
        return tracker;
    };

    Segment operator+ (const Segment& s1,const cv::Point2i& r ){
        return { cv::Rect2i(s1.rect.tl()+r,s1.rect.br()+r), s1.value};

    }
    std::ostream& operator << (std::ostream& o,const Segment & s){
        o <<  s.rect << ": " << s.value;
        return o;
    }

    std::ostream& operator << (std::ostream& o,const Segments & s){
        for (int i=0;i<s.size();++i){
            o << s[i] <<"\n";
        }
        return o;
    }
    void Tracker::track(const cv::Mat_<cv::Vec3b> &image, const Segments &segments, int image_index, int min_matches, int max_distance)
    {
        assert(marked_image_index>-1 && marked_image_segments.size()>0);
        if (!image.empty())
        {
            //TODO: Use features from image
        }
        auto mark_matches = match_segments(marked_image_segments, segments, max_distance);
        LOG(DEBUG) << "Found " << std::get<0>(mark_matches).size() << " mateches to mark\n";
        cv::Mat mark_to_cur, ref_to_cur;
        if (std::get<0>(mark_matches).size() >= std::max(min_matches, 6))
        {   
            if (std::get<0>(mark_matches).size() > 20){
                mark_to_cur = cv::estimateAffine2D(std::get<0>(mark_matches), std::get<1>(mark_matches));
            }
            else{
                mark_to_cur = cv::estimateAffinePartial2D(std::get<0>(mark_matches), std::get<1>(mark_matches));
            }
        }
        else
        {
            LOG(DEBUG) << "No transformation from marked image (" << marked_image_index << "), to current ( " << image_index << ")\n";
        }

        auto ref_matcehs = match_segments(ref_image_segments, segments, max_distance);
        LOG(DEBUG) << "Found " << std::get<0>(ref_matcehs).size() << " mateches to ref\n";
        if (std::get<0>(ref_matcehs).size() >= std::max(min_matches, 6))
        {
            
            if (std::get<0>(mark_matches).size() > 20){
                ref_to_cur = cv::estimateAffine2D(std::get<0>(ref_matcehs), std::get<1>(ref_matcehs));
            }
            else{
                ref_to_cur = cv::estimateAffinePartial2D(std::get<0>(ref_matcehs), std::get<1>(ref_matcehs));
            }
        }
        else
        {
            LOG(DEBUG) <<  "No transformation from ref image (" << ref_image_index << "), to current ( " << image_index << ")\n";
        }

        if (!mark_to_cur.empty())
        {
            LOG(DEBUG) <<  "Switch reference, (mark to cur known) " << ref_image_index << " to " << image_index << "\n";
            update_reference_image(segments, image_index, mark_to_cur);
            marked_to_cur_transform = to_3x3(mark_to_cur).clone();
        }
        else if (!ref_to_cur.empty())
        {
            LOG(DEBUG) <<  "No transformation from marked image (" << image_index << "), using prev ( " << ref_image_index << ")\n";
            marked_to_cur_transform = (to_3x3(ref_to_cur) * marked_to_ref_transform);
        }
        else
        {
            // No known match. assume we didn't move in this frame, and try to use it as refernce,
            // If it has enough points. Note that this is twice the requirements
            // of the requirement to use transformation, since there we compare to
            // the number of points and here to the number of boxes - each box give us two points.
            if (segments.size() > std::max(min_matches, 6))
            {

                LOG(DEBUG) <<  "Switch reference from (Mark to cur unkonw, assume I)" << ref_image_index << " to " << image_index << "\n";
                update_reference_image(segments, image_index, marked_to_cur_transform);
            }
        }
        LOG(DEBUG) << "marked_to_cur_transform: \n" << marked_to_cur_transform << "\n";
    }

}