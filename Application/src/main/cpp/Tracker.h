#pragma once
#include <vector>
#include <string>
#include <opencv2/opencv.hpp>

namespace mmv
{
struct Segment
{
    cv::Rect2i rect;
    std::string value;
    bool operator==(const Segment &other)
    {
        return rect == other.rect && value == other.value;
    }
};
Segment operator+(const Segment &s1, const cv::Point2i &r);
std::ostream &operator<<(std::ostream &o, const Segment &s);

struct Segments
{
    std::vector<cv::Point2i> tl={};
    std::vector<cv::Point2i> br={};
    std::vector<std::string> value={};

    // Segments() = default;
    // Segments(const Segments& o):tl(o.tl),br(o.br),value(o.value){
    //     assert(tl.size()==br.size() && br.size() == value.size());
    // }
    // Segments& operator=(const Segments& o){
    //     tl = o.tl;
    //     br = o.br;
    //     value = o.value;
    //     assert(tl.size()==br.size() && br.size() == value.size());
    //     return *this;
    // }
    // Segments(
    // const std::vector<cv::Point2i>& _tl,
    // const std::vector<cv::Point2i>& _br,
    // const std::vector<std::string>& _value):tl(_tl),br(_br),value(_value){
    //     assert(tl.size()==br.size() && br.size() == value.size());
    // }

    const size_t size() const
    {
        assert(tl.size() == br.size() && br.size() == value.size());
        return tl.size();
    }
    const size_t area(size_t i) const
    {
        auto d = (br[i] - tl[i]);
        return d.x * d.y;
    }
    void resize(size_t size)
    {
        tl.resize(size);
        br.resize(size);
        value.resize(size);
        assert(tl.size() == br.size() && br.size() == value.size());
    }
    Segment operator[](size_t i) const
    {
        return Segment{cv::Rect2i(tl[i], br[i]), value[i]};
    }
    void push_back(const Segment &segment)
    {
        tl.push_back(segment.rect.tl());
        br.push_back(segment.rect.br());
        value.push_back(segment.value);
        assert(tl.size() == br.size() && br.size() == value.size());
    }
    struct iterator{
        size_t index;
        Segments& iterable;
        Segment operator*() const { return iterable[index];}
        const iterator operator++() {  index+=1; return *this;}
        //bool operator==(const iterator& other) { return index==other.index && &iterable == &other.iterable;}
        bool operator!=(const iterator& other) const { return index!=other.index || &iterable != &other.iterable;}
    };
    
    iterator begin(){
        return iterator { 0 , *this};
    }
    iterator end(){
        return iterator { size() , *this};
    }
};

std::ostream &operator<<(std::ostream &o, const Segments &s);

inline auto match_segments(const Segments &s1, const Segments &s2, float max_dist) -> decltype(auto)
{
    std::vector<std::tuple<int, int>> matches;
    matches.reserve(std::max(s1.size(), s2.size()));
    for (int i = 0; i < s1.size(); ++i)
    {
        float min_dist = max_dist;
        int min_index = -1;
        for (int j = 0; j < s2.size(); ++j)
        {
            //Same value
            if (s1.value[i] == s2.value[j])
            {
                //Distance, and close in size:
                cv::Point2i dp = s1.tl[i] - s2.tl[j];
                float dist_tl = std::sqrt(dp.x * dp.x + dp.y * dp.y);
                float area_ratio = s1.area(i) / float(s2.area(j));
                if (dist_tl < min_dist && 0.5 < area_ratio && area_ratio < 2.0)
                {
                    min_index = j;
                }
            }
        }
        if (min_index > -1)
        {
            matches.push_back({i, min_index});
        }
    }
    std::vector<cv::Point2i> p1, p2;
    for (auto m : matches)
    {
        p1.push_back(s1.tl[std::get<0>(m)]);
        p2.push_back(s2.tl[std::get<1>(m)]);
        p1.push_back(s1.br[std::get<0>(m)]);
        p2.push_back(s2.br[std::get<1>(m)]);
    }
    return std::make_tuple(p1, p2);
}

struct Tracker
{
    Segments marked_image_segments;
    int marked_image_index = -1;

    Segments ref_image_segments;
    cv::Mat marked_to_ref_transform;
    Segments marked_segments_in_ref;
    int ref_image_index = -1;

    cv::Mat marked_to_cur_transform;

    Tracker()
    {
        marked_to_cur_transform = cv::Mat::eye(3, 3, CV_64F);
    }

    void load_marked_image(const std::string& path);
    void save_marked_image(const std::string& path);

    cv::Mat to_3x3(const cv::Mat &trans)
    {
        cv::Mat trans3 = trans.clone();
        if (trans.rows == 2)
        {
            trans3 = cv::Mat::eye(3, 3, CV_64F);
            trans.copyTo(trans3(cv::Rect(0, 0, 3, 2)));
            return trans3;
        }
        return trans3;
    }

    Segments transform(const Segments& source, cv::Mat trans){
        if (trans.rows == 3){
            trans = trans(cv::Range(0,2),cv::Range::all());
        }
        Segments transformed = source;
        assert(source.size()==transformed.size());
        if (transformed.size()>0){
            cv::transform(source.tl, transformed.tl, trans);
            cv::transform(source.br, transformed.br, trans);
        }
        return transformed;
    }

    Segments transform(const Segments &segments){
        return transform(segments, marked_to_cur_transform);
    }
    
    void update_reference_image(const Segments &ref_segemnts, int ref_index, cv::Mat marked_to_ref)
    {
        ref_image_segments = ref_segemnts;
        ref_image_index = ref_index;
        marked_segments_in_ref = transform(marked_image_segments, marked_to_ref);
        marked_to_ref_transform = to_3x3(marked_to_ref);
    }

    void set_marked_image(const Segments segments, int id)
    {
        marked_image_segments = segments;
        marked_image_index = id;
        marked_to_cur_transform = cv::Mat::eye(3, 3, CV_64F);
        update_reference_image(segments, id, cv::Mat::eye(3, 3, CV_64F));
    }
    

    //Tracking logic:
    //* We always want to return the location of the marked segments.
    //* We want to minimize the number of jumps from the marked segments to the current image.
    //* First compare directly to the marked segments.
    //* If there is no match - compare to some reference.
    //* A reference must have transformation from the marked image.
    //* Minimize the refernce changes advances if they have no direct transformation from the marked image.
    //* As long as we have transformation to current reference, keep it. When we have no transformation
    //  to the reference image, compute the transformation from previous image, or assume identity, and add
    //  current image as new refernce.
    //  Each time we have a transformation from an earliear refernce - collapse all refernces after it.
    //  If we seem to need a new reference and we reached N refernces, replace the last reference.
    //  Let's start with 1 referene

    void track(const cv::Mat_<cv::Vec3b> &image, const Segments &segments, int image_index, int min_matches, int max_distance);

    
};

Tracker &get_tracker();
} // namespace mmv