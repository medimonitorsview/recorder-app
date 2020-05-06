# Medical monitors recorder


This repo is an android app to record and ocr medical monitors, it works in conjuction with a 
setup application and a server, please see [TODO: Add link]

## Android App

Before opening this app with android studio, please make sure you have opencv downloaded to the correct location,
you have a few ways to do it:

- Manualy download opencv for android, and extract it to the root folder, WITHOUT overriding `OpenCV-android-sdk/sdk/build.gradle`
- Run `./get-opnecv.sh`
- Run ./gradlew which will run `./get-opencv.sh`



You should be able to compile it with android studio, note that it requires native code, and uses
google vision api (The older, non firebase version).

## Host Native development
Note that this app contains also native code. The native code has also an "host" version that 
you can on an x86 linux computer for develpoment. The native code can be built with cmake.

To be able to build the host code you need to install some libs (note that opencv>=3, mabye 4 is probably needed):

You can try:

```bash
sudo apt-get install rapidjson-dev libopencv-dev libeigen3-dev
```

## auto updates 
for some reason, if installing an app without the "android:testOnly" flag, and set this app as
a device owner. you can not removing the app from being a device owner without factory reset


## installing the app
1. first install the apk
2. set the app as a device owner with the following command
```cmd 
adb shell dpm set-device-owner org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver
``` 

you can not uninstall a device owner app, you must first remove the app from being a device owner
with the following command

```cmd
adb shell dpm remove-active-admin org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver
```

check the version code of an app
```cmd
adb shell dumpsys package org.mdeimonitorsview.android.recorder | Select-String code
```

install the ocr tester
```cmd
adb install -t -r ocr-tester.apk
```


