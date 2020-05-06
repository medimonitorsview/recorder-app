#include "ImageEnhance.h"
#include <iostream>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <unordered_map>
#include <filesystem>
#include <opencv2/opencv.hpp>
#include <rapidjson/document.h>
#include <opencv2/core/utils/filesystem.hpp>
using namespace std::string_literals;
namespace fs = std::filesystem;
struct Segment
{
    int top, left, bottom, right;
    std::string name, value;
};
int main(int argc, char **argv){
    if (argc < 3)
    {
        std::cout << "Usege" << argv[0] << "<input_folder> <output_folder> [segments_file]" << std::endl;
        exit(-1);
    }
    rapidjson::Document doc;
    std::vector<std::string> image_files;
    if (!fs::is_directory(argv[2]) || !fs::exists(argv[2])) { // Check if src folder exists
       cv::utils::fs::createDirectories(argv[2]); // create src folder
    }
    std::unordered_map<std::string, std::vector<Segment>> segments;
    std::unordered_map<std::string, std::vector<cv::Point>> screens;
    if (argc == 4)
    {
        std::ifstream f(argv[3]);
        if (f)
        {
            std::ostringstream ss;
            ss << f.rdbuf(); // reading data
            doc.Parse(ss.str().c_str());
            for (const auto &segmented_file : doc.GetArray())
            {
                if (segmented_file.HasMember("filename"))
                {
                    std::string filename = segmented_file["filename"].GetString();
                    image_files.push_back(std::string(argv[1]) + "/"s + filename);

                    for (const auto &s : segmented_file["segments"].GetArray())
                    {
                        auto segment = Segment({
                            .top = s["top"].GetInt(),
                            .left = s["left"].GetInt(),
                            .bottom = s["bottom"].GetInt(),
                            .right = s["right"].GetInt(),
                            .name = s["name"].GetString(),
                            .value = s["value"].GetString(),
                        });
                        segments[filename].push_back(segment);
                    }
                    const auto& screen =segmented_file["screen"].GetObject();
                    auto top = screen["top"].GetInt();
                    auto left = screen["left"].GetInt();
                    auto bottom = screen["bottom"].GetInt();
                    auto right = screen["right"].GetInt();
                    auto name = screen["name"].GetString();
                    auto value = screen["value"].GetString();
                    
                    screens[filename].push_back({left, top});
                    screens[filename].push_back({right, top});
                    screens[filename].push_back({left, bottom});
                    screens[filename].push_back({right, bottom});
                    

                }
            }
        }
        else
        {
            std::cerr << "Could not read json file " << argv[3] << std::endl;
        }
    }
    else
    {
        for (const auto &entry : fs::directory_iterator(argv[1]))
        {
            image_files.push_back(entry.path());
        }
    }
    for (const auto &filename : image_files)
    {
        fs::path path = {filename};
        std::string image_path = path;
        std::string image_filename = std::string(path.filename());
        std::string output_filename = std::string(argv[2]) + "/"s + image_filename;
        cv::Mat_<cv::Vec3b> image = cv::imread(image_path);
        if (image.empty())
        {
            std::cout << "Cloud not read" << image_path << std::endl;
            continue;
        }
        const auto &segment_record = segments.find(image_filename);
        if (segment_record == segments.end())
        {
            std::cout << "No segments for " << image_path << std::endl;
            continue;
        }
        auto&  sc = screens[image_filename];
        mmv::GlobalProcess(image,sc, 3.0f,180,4,4);
        cv::imwrite(output_filename, image);
        //mmv::SegmentProcess(image, sc.top, sc.left, sc.bottom, sc.right);
        

        for (const auto &s : segment_record->second)
        {
            mmv::SegmentProcess(image, s.top, s.left, s.bottom, s.right, true);
        }
        cv::imwrite(output_filename + ".segments.jpg"s, image);
    }
}
