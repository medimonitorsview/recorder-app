#define DOCTEST_CONFIG_IMPLEMENT_WITH_MAIN
#include "Tracker.h"
#include <doctest/doctest.h>
#include <iostream>
#include <filesystem>
#include <rapidjson/document.h>
using namespace std::string_literals;

namespace fs = std::filesystem;

TEST_CASE("Basic"){
    mmv::Tracker tracker;

    mmv::Segments initial_segments =  {
        {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0},{1000,0}},
        {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50},{1050,50}},
        {"hello","world","this","is","a","sentence","with","ten?","words","!","!"}
    };

    mmv::Segments segments_2 =  {
        {{10,0},{110,0},{210,0},{310,0},{410,0},{510,0},{610,0},{710,0},{810,0},{910,0}},
        {{60,50},{160,50},{260,50},{360,50},{460,50},{560,50},{660,50},{760,50},{860,50},{960,50}},
        {"hello","world","this","is","a","sentence","with","nine?","words","!"}
    };

    tracker.set_marked_image(initial_segments,0);
    tracker.track(cv::Mat(),segments_2,1,8,100);
    auto result_segments = tracker.transform(initial_segments);
    auto size = std::min(result_segments.size(),segments_2.size());
    for (int i=0;i<size;++i){
        CHECK(result_segments.tl[i]==segments_2.tl[i]);
        CHECK(result_segments.br[i]==segments_2.br[i]);
        std::cout << result_segments.value[i] << " " << segments_2.value[i] << std::endl;
        if (i!=7){
            CHECK(result_segments.value[i]==segments_2.value[i]); //ten!=nine
        }
    }

}

TEST_CASE("Accumelate"){
    mmv::Tracker tracker;

    mmv::Segments initial_segments =  {
        {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0},{1000,0}},
        {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50},{1050,50}},
        {"hello","world","this","is","a","sentence","with","ten?","words","!","!"}
    };

    int max_movement = 30;
    int max_noise = 2;
    int max_error = 5;
    tracker.set_marked_image(initial_segments,0);
    int errors=0;
    int frame_count = 10;
    int dx=0;
    int dy=0;
    int frame = 0;
    // Step 1, the words are abouth the same, some of the move a bit, disappear a bit.
    for (;frame < frame_count ; ++frame){

        dx = (std::rand() % (2*max_movement)) - max_movement;
        dy = (std::rand() % (2*max_movement)) - max_movement;
        mmv::Segments segments_2 =  {
            {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0}},
            {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50}},
            {"hello","world","this","is","a","sentence","with","nine?","words","!"}
        };
        mmv::Segments segments_3;
        for (int si = 0 ; si < segments_2.size(); ++si)
            {
                if ((std::rand() % 6)==0)
                    continue;

            int noise = max_noise==0?0:((std::rand() % (2*max_noise)) - max_noise);
            segments_2.tl[si].x+=(dx+noise);
            segments_2.tl[si].y+=(dy+noise);
            segments_2.br[si].x+=(dx+noise);
            segments_2.br[si].y+=(dy+noise);
            segments_3.push_back(segments_2[si]);
        } 
        tracker.track(cv::Mat(),segments_3,frame,5,10*max_movement);
        auto result_segments = tracker.transform(initial_segments);
        CHECK(result_segments.size() == initial_segments.size());
        auto size = std::min(result_segments.size(),segments_3.size());
        if (size == 0){
            errors++;
        }
        for (int i=0;i<size;++i){
            if ((abs(result_segments.tl[i].x-(initial_segments.tl[i].x+dx)) > max_error) ||
                (abs(result_segments.tl[i].y-(initial_segments.tl[i].y+dy)) > max_error) ||
                (abs(result_segments.br[i].x-(initial_segments.br[i].x+dx)) > max_error) ||
                (abs(result_segments.br[i].y-(initial_segments.br[i].y+dy)) > max_error))
                {
                    //std::cerr << result_segments[i] << " vs " << initial_segments[i]+ cv::Point2i{dx,dy} <<  " vs " <<  segments_2[i] << std::endl;
                    errors++;
                }
        }
        std::cout << "marked to last is  " << tracker.marked_to_cur_transform << "dx:" << dx << " dy:" << dy << "\n";
    }
    CHECK(errors < frame_count* initial_segments.size()* 0.01);
    std::cout << "marked to last is  " << tracker.marked_to_cur_transform;
    errors=0;
    // Step 2, the words chnage, at least most of them.
    // What we expect to have now, is relative transfromation. hopefully it wont 
    // acculmelate too much
    int cdx=dx;
    int cdy=dy;
    dx = 0;
    dy = 0;
    const mmv::Segments segments_2 =  {
        {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0}},
        {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50}},
        {"not","so","nice","world","with","some","plauge","running","around"}
    };
    for (;frame < frame_count *2; ++frame){


        mmv::Segments segments_3;
        for (int si = 0 ; si < segments_2.size(); ++si)
            {
                if ((std::rand() % 6)==0)
                    continue;

            int noise = max_noise==0?0:((std::rand() % (2*max_noise)) - max_noise);
            cv::Point2i movment = {cdx+dx+noise, cdy+dy+noise};
            mmv::Segment s = { segments_2[si].rect + movment, segments_2[si].value};
            segments_3.push_back(s);
        } 
        tracker.track(cv::Mat(),segments_3,frame,5,10*max_movement);
        auto result_segments = tracker.transform(initial_segments);
        CHECK(result_segments.size() == initial_segments.size());
        auto size = result_segments.size();
        if (size == 0){
            errors++;
        }
        for (int i=0;i<size;++i){
            if ((abs(result_segments.tl[i].x-(initial_segments.tl[i].x+cdx+dx)) > max_error) ||
                (abs(result_segments.tl[i].y-(initial_segments.tl[i].y+cdy+dy)) > max_error) ||
                (abs(result_segments.br[i].x-(initial_segments.br[i].x+cdx+dx)) > max_error) ||
                (abs(result_segments.br[i].y-(initial_segments.br[i].y+cdy+dy)) > max_error))
                {
                    //std::cerr << result_segments[i] << " vs " << initial_segments[i]+ cv::Point2i{dx,dy} <<  " vs " <<  segments_2[i] << std::endl;
                    errors++;
                }
        }
        std::cout << "marked to last is:\n" << tracker.marked_to_cur_transform << "\ndx:" << (cdx+dx) << " dy:" << (cdy+dy) << "\n";
        dx = (std::rand() % (2*max_movement)) - max_movement;
        dy = (std::rand() % (2*max_movement)) - max_movement;
        
    }
    CHECK(errors < frame_count* initial_segments.size()* 0.05);

    errors = 0;
    // Step 3, return to original text
    mmv::Segments segments_4 =  {
        {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0}},
        {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50}},
        {"hello","world","this","is","a","sentence","with","nine?","words","!"}
    };
    for (; frame < frame_count*3 ; ++frame){

        int dx = (std::rand() % (2*max_movement)) - max_movement;
        int dy = (std::rand() % (2*max_movement)) - max_movement;

        mmv::Segments segments_3;
        for (int si = 0 ; si < segments_4.size(); ++si)
        {
                if (si>0 && (std::rand() % 6)==0)
                    continue;

            int noise = max_noise==0?0:((std::rand() % (2*max_noise)) - max_noise);
            cv::Point2i movment = {dx+noise, dy+noise};
            mmv::Segment s = { segments_4[si].rect + movment, segments_4[si].value};
            segments_3.push_back(s);
        } 
        tracker.track(cv::Mat(),segments_3,frame,5,10*max_movement);
        auto result_segments = tracker.transform(initial_segments);
        auto size = std::min(result_segments.size(),segments_2.size());
        if (size == 0){
            errors++;
        }
        for (int i=0;i<size;++i){
            if ((abs(result_segments.tl[i].x-(initial_segments.tl[i].x+dx)) > max_error) ||
                (abs(result_segments.tl[i].y-(initial_segments.tl[i].y+dy)) > max_error) ||
                (abs(result_segments.br[i].x-(initial_segments.br[i].x+dx)) > max_error) ||
                (abs(result_segments.br[i].y-(initial_segments.br[i].y+dy)) > max_error))
                {
                    //std::cerr << result_segments[i] << " vs " << initial_segments[i]+ cv::Point2i{dx,dy} <<  " vs " <<  segments_2[i] << std::endl;
                    errors++;
                }
        }
    }
    CHECK(errors < frame_count* initial_segments.size()* 0.01);
}


TEST_CASE("Movemnt"){
    mmv::Tracker tracker;

    mmv::Segments initial_segments =  {
        {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0},{1000,0}},
        {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50},{1050,50}},
        {"hello","world","this","is","a","sentence","with","ten?","words","!","!"}
    };

    int max_movement = 30;
    int max_noise = 5;
    int max_error = 10;
    tracker.set_marked_image(initial_segments,0);
    int errors=0;
    int frame_count = 100;
    // Step 1, the words are abouth the same, some of the move a bit, disappear a bit.
    for (int frame = 0; frame < frame_count ; ++frame){

        int dx = (std::rand() % (2*max_movement)) - max_movement;
        int dy = (std::rand() % (2*max_movement)) - max_movement;
        mmv::Segments segments_2 =  {
            {{0,0},{100,0},{200,0},{300,0},{400,0},{500,0},{600,0},{700,0},{800,0},{900,0}},
            {{50,50},{150,50},{250,50},{350,50},{450,50},{550,50},{650,50},{750,50},{850,50},{950,50}},
            {"hello","world","this","is","a","sentence","with","nine?","words","!"}
        };
        mmv::Segments segments_3;
        for (int si = 0 ; si < segments_2.size(); ++si)
            {
                if ((std::rand() % 6)==0)
                    continue;

            int noise = max_noise==0?0:((std::rand() % (2*max_noise)) - max_noise);
            segments_2.tl[si].x+=(dx+noise);
            segments_2.tl[si].y+=(dy+noise);
            segments_2.br[si].x+=(dx+noise);
            segments_2.br[si].y+=(dy+noise);
            segments_3.push_back(segments_2[si]);
        } 
        tracker.track(cv::Mat(),segments_3,frame,5,10*max_movement);
        auto result_segments = tracker.transform(initial_segments);
        auto size = std::min(result_segments.size(),segments_2.size());
        if (size == 0){
            errors++;
        }
        for (int i=0;i<size;++i){
            if ((abs(result_segments.tl[i].x-(initial_segments.tl[i].x+dx)) > max_error) ||
                (abs(result_segments.tl[i].y-(initial_segments.tl[i].y+dy)) > max_error) ||
                (abs(result_segments.br[i].x-(initial_segments.br[i].x+dx)) > max_error) ||
                (abs(result_segments.br[i].y-(initial_segments.br[i].y+dy)) > max_error))
                {
                    //std::cerr << result_segments[i] << " vs " << initial_segments[i]+ cv::Point2i{dx,dy} <<  " vs " <<  segments_2[i] << std::endl;
                    errors++;
                }
        }
    }
    CHECK(errors < frame_count* initial_segments.size()* 0.01);
}

TEST_CASE("RunFolder"){
    rapidjson::Document doc;
    auto folder = fs::current_path().concat("/../../data/2020_05_04_09/"s);
    auto out_folder = fs::current_path().concat("/../../data/2020_05_04_09_out/"s);
    if (!fs::exists(out_folder)){
        fs::create_directories(out_folder);        
    }
        
    std::vector<std::string> image_files;
    std::unordered_map<int, mmv::Segments> segments;
    std::vector<std::string> filenames;
    cv::glob(std::string(folder) + "*.json"s, filenames);
    std::sort(filenames.begin(),filenames.end(),[](auto& fa, auto& fb){
        auto ba = std::filesystem::path(fa).stem();
        auto bb = std::filesystem::path(fb).stem();
        return ba < bb;
    });

    mmv::Tracker tracker;
    mmv::Segments marked_segments;
    for (auto filename : filenames)
    {
        std::ifstream f(filename);
        if (!f){
            continue;
        }
        std::ostringstream ss;
        ss << f.rdbuf(); // reading data
        doc.Parse(ss.str().c_str());
        auto image_id = doc["imageId"].GetInt();
        for (const auto &s : doc["segments"].GetArray())
        {
            
            auto segment = mmv::Segment{
                .rect = {
                    cv::Point(s["left"].GetInt(), s["top"].GetInt()),
                    cv::Point(s["right"].GetInt(), s["bottom"].GetInt())
                    },
                .value = s["value"].IsString()?s["value"].GetString():""
            };
            segments[image_id].push_back(segment);
        }

        std::cout << "image " << image_id << " has " << segments[image_id].size() << " detections\n";
        if (tracker.marked_image_index==-1){
            tracker.set_marked_image(segments[image_id], image_id);
            marked_segments = segments[image_id];
        }
        tracker.track(cv::Mat(),segments[image_id],image_id,6,100);
        //std::cout << tracker.marked_to_cur_transform << "\n";
        //std::cout << segments[tracker.marked_image_index]  << "\n";
        //std::cout << tracker.transform(marked_segments) << "\n";
        auto image_file = std::string(folder)  + std::to_string(image_id) + ".png"s;
        auto out_image_file = std::string(out_folder) + std::to_string(image_id)  +".png"s;
        
        auto image = cv::imread(image_file);
        if (image.empty()){
            std::cout << "can't read " << image_file << std::endl;
            continue;
        }
        auto transformed = tracker.transform(marked_segments);
        for(const auto& s : transformed){
            cv::rectangle(image,s.rect,{0,255,0}, 4);
        }
        cv::imwrite(out_image_file,image);
        

    }
}