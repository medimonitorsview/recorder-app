# Medical monitors recorder app

This repo is an android app to record medical monitors, it works in conjunction with a 
setup application and a server, please see: [server-deploy](https://github.com/medimonitorsview/server-deploy) for an overview on the system.

## Installation

Note: This application should be installed in device owner mode, on a factory reset device in order to properly function. If the application will be installed normally from the APK it will work but would not be able to update itself.

1. Factory reset the device.

2. Advance in the device setup until the wifi screen and connect to an internet connected wifi network.

3. Return (with back arrows) to the first screen, and tap it 6 time in one location (any location will do). A camera screen should appear.

4. In another device or computer go to the release you want to install [releases](https://github.com/medimonitorsview/recorder-app/releases), download and display the qr image.

5. Scan the qr with the recording phone, and press next to install the application, and continue with device setup, until it finishes.

6. Open the application, and press the red camera button. If OCR package is not yet downloaded, it will display a message. Wait until it is downloaded (To speed it up, you can enter the Play Store app and cancel updates)

7. Close the application, open it again again, and press the red camera button, verify you see a message that OCR is ready.

8. Now application is ready for use, you can disconnect from the internet connected wifi.

Alternatively, just install the apk from the releases](https://github.com/medimonitorsview/recorder-app/releases) page, but then you won't be able to auto update
the app from the central server, and you will need to manually update it.

## Usage

1. From the server frontend (see [server-deploy](https://github.com/medimonitorsview/server-deploy)) prepare qr code stickers page.

2. Stick one QR sticker on the device you want to record.

3. Start the application. 

4. Select recording resolution (A bit more than 1000 on each dimension is a good balance 
for a low end phone between speed and accuracy)

5. Enter central server IP

6. Press the scan QR and scan the QR on the device.

7. Start recording, and then position the device in front of the screen.

8. Use the setup app set the measurements that you want to record.

## ***Device positioning***

Good positioning of the phone in front of the screen is crucial for recording accuracy. When you position the device you must pay attention to the following:

1. Position the phone horizontally (in landscape mode), such that the text on the    
   recording screen is in the correct orientation - upward facing.

2. It should see all the screen area that contains the data you want to record, and a 
   bit more, but keep the area outside the screen minimal.

3. It should be held in a secure way with a fixed and strong handle such that it
   won't move in relation to the monitor.

4. It should be positioned in an angle as parallel to the screen as possible without 
   interfering to manual reading of the monitor.

5. Try to keep reflections from lights in the room minimal, and in any case - not on 
   the actual data you want to record. To see if there are reflections, look at the
   video preview on the device, in the image in the settings app or in the server.


## Development:


### Android App

Before opening this app with android studio, please make sure you have opencv downloaded to the correct location, and build the app once.

- On windows: you need to manually download opencv for android, and extract it to the root folder, WITHOUT overriding `OpenCV-android-sdk/sdk/build.gradle`
- Run `./gradlew assmble`. It should build the application.

Now you can open the app folder in android studio.

Please note, that the NDK version was selected to be compatible with the one
in github actions images. don't change it without checking the CI

If you want to build also a release version please set keys information in `~/.gradle/gradle.properties`, you need to set here:

```bash
RELEASE_STORE_PASSWORD=
RELEASE_KEY_ALIAS=
RELEASE_KEY_PASSWORD=
RELEASE_STORE_FILE=
```

The github action CI will use keys in github secrets to generate the release version.

Notes:

* The application has native (c++ component), which can be tested in host computer, see below
* The application uses google computer vision api, the older, non firebase version, *which is deprecated* the api is very similar to the firebase google ml, and the transition should be simple, but we didn't want to tie ourself to firebase account.


### Host Native development
This app contains also native (c++) code. The native code has also an "host" version that you can test it on an x86 linux computer for development. The native code can be built with cmake, and uses `doctest` for testing (vscode has a nice plugin to suport it it)

To be able to build the host code you need to install some libs (note that opencv>=3, maybe 4 is probably needed):

You can try:

```bash
sudo apt-get install rapidjson-dev libopencv-dev libeigen3-dev
```

## Auto updates

The app should be able to update itself from the central server without any manual 
action. The recommended way to perform this is by setting it as a device owner this
QR after factory reset. 

It is possible to install it as device owner with adb without  factory reset, with the 
following procedure:


Note that to make this work the app should have "android:testOnly" flag.


1. Install the apk
2. Set the app as a device owner with the following command

```cmd 
adb shell dpm set-device-owner org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver
``` 

You can not uninstall a device owner app, you must first remove the app from being a device owner with the following command, which will work only non factory reset install mode.

```cmd
adb shell dpm remove-active-admin org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver
```

Check the version code of an app
```cmd
adb shell dumpsys package org.mdeimonitorsview.android.recorder | Select-String code
```
