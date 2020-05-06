#! /bin/bash
set -eax

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "${script_dir}"
if [[ ! -d 'OpenCV-android-sdk/sdk/java' ]]
then
    if [[ ! -f 'opencv-4.3.0-android-sdk.zip' ]]
    then
    curl 'https://netix.dl.sourceforge.net/project/opencvlibrary/4.3.0/opencv-4.3.0-android-sdk.zip' -o opencv-4.3.0-android-sdk.zip
    fi
    mkdir -p opencv
    unzip -n opencv-4.3.0-android-sdk.zip -x OpenCV-android-sdk/sdk/build.gradle
fi