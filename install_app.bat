set APK_PATH=%1
ECHO %1%
adb install %1%
adb shell dpm set-device-owner org.mdeimonitorsview.android.recorder/.devowner.DevAdminReceiver