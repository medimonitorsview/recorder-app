cmake_minimum_required(VERSION 3.10)
set(SRC_FILES  src/main/cpp/ImageEnhance.cpp src/main/cpp/Tracker.cpp)

if (ANDROID)
    set(SRC_FILES  ${SRC_FILES} src/main/cpp/ImageEnhance_jni.cpp)
    set(INC_FOLDERS ${CMAKE_CURRENT_LIST_DIR}/build/eigen-3.3.7/)
    set(OpenCV_DIR "${CMAKE_CURRENT_LIST_DIR}/../OpenCV-android-sdk/sdk/native/jni/")
endif()


add_library(ImageEnhance SHARED ${SRC_FILES})

find_package(OpenCV REQUIRED)
if (NOT ${OPENCV_FOUND})
    message(ERROR_FATAL "Could not find opencv ")
endif()

target_link_libraries(ImageEnhance PUBLIC ${OpenCV_LIBS})

target_include_directories(ImageEnhance PUBLIC ${INC_FOLDERS})
target_compile_features(ImageEnhance PUBLIC cxx_std_17)

if (ANDROID)
    target_compile_options(ImageEnhance PUBLIC -O2 -DNDEBUG)
    set_target_properties(ImageEnhance PROPERTIES CXX_VISIBILITY_PRESET hidden)
endif()

if (NOT ANDROID)
    find_package(Eigen3)
    target_link_libraries(ImageEnhance PUBLIC Eigen3::Eigen)
endif()

if (NOT ANDROID)
    include(FetchContent)
    enable_testing()
    FetchContent_Declare(
        doctest
        URL "https://github.com/onqtam/doctest/archive/2.3.6.tar.gz"
        URL_MD5 298e5773f3eb9825f6e3015e8f9511ca)
    FetchContent_GetProperties(doctest)
    if(NOT doctest_POPULATED)
        FetchContent_Populate(doctest)
        add_subdirectory(${doctest_SOURCE_DIR} ${doctest_BINARY_DIR})
    endif()
    include(${doctest_SOURCE_DIR}/scripts/cmake/doctest.cmake)
    
    add_executable(ImageEnhacneTest src/main/cpp/ImageEnhanceTest.cpp)
    target_link_libraries(ImageEnhacneTest ImageEnhance doctest::doctest)
    target_compile_features(ImageEnhacneTest PRIVATE cxx_std_17)
    
    add_executable(TrackerTest src/main/cpp/TrackerTest.cpp)
    target_link_libraries(TrackerTest ImageEnhance doctest::doctest)
    target_compile_features(TrackerTest PRIVATE cxx_std_17)
    
    doctest_discover_tests(TrackerTest)

endif()