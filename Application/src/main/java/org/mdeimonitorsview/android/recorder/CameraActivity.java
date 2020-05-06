/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mdeimonitorsview.android.recorder;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.mdeimonitorsview.android.recorder.classes.ApkDownloader;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class CameraActivity extends AppCompatActivity {

    private static Integer NUMBER_OF_IMAGES_PER_SAVING = null;

    public static int FREQUENCY_OCR_LOGGING_MILI = 5 * 60 * 1000;

    //todo: check if using static logger
    org.slf4j.Logger logger = LoggerFactory.getLogger(MainActivity.class);

    public final String TAG = "CameraActivity";

    public final int DELAY_BEFORE_TAKING_PICTURES_MILLIS = 4000;

    public Integer imageFrequencyMili;

    String IMAGE_ID_KEY = "image_ID";
    int imageId;

    Float focusDistance;

    Handler handler;

    Runnable takingPicturesRunnable;

    private Camera2BasicFragment camera2BasicFragment;

    Button stopTakingPicturesButton;
    public SharedPreferences preference;

    //todo: remove after debug
    private long lastRunTime = 0;

    int resolutionIndex;

    public HashMap<String, Rect> croppingMap;

    public String serverUrl;
    public String monitorId;

    //todo remove this
    public TextView monitorIdTv;

    public static Integer getNumberOfImagesPerSaving() {
        return NUMBER_OF_IMAGES_PER_SAVING;
    }


    public Float getFocusDistance() {
        return focusDistance;
    }

    public void setFocusDistance(Float focusDistance) {
        this.focusDistance = focusDistance;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Log.d(AppConstants.INSTALLER_TAG, "onCameraActiviy");
        //UtilsKt.install(this, getPackageName(), AppConstants.APK_PATH);

        // set the default focus to be detected automatically
        focusDistance = null;

        // load image frequency from shared preference
        preference = PreferenceManager.getDefaultSharedPreferences(this);
        imageFrequencyMili = getIntent().getIntExtra(MainActivity.IMAGE_FREQUENCY_KEY, 3000);
        resolutionIndex = getIntent().getIntExtra(MainActivity.IMAGE_RESOLUTION_INDEX,  0);
        String resolutionString = getIntent().getStringExtra(MainActivity.IMAGE_RESOLUTION_STRING);
        monitorId = getIntent().getStringExtra(MainActivity.MONITOR_ID_KEY);

        serverUrl = getIntent().getStringExtra(MainActivity.SERVER_URL_STRING);

        TextView resolutionStringTv = findViewById(R.id.resolution_string_tv);
        resolutionStringTv.setText(resolutionString);

        TextView frequencyTv = findViewById(R.id.image_frequency_tv);
        frequencyTv.setText(imageFrequencyMili.toString());

//        NUMBER_OF_IMAGES_PER_SAVING = FREQUENCY_OCR_LOGGING_MILI / imageFrequencyMili;
        NUMBER_OF_IMAGES_PER_SAVING = 1;

        monitorIdTv = findViewById(R.id.monitor_id_tv);
        monitorIdTv.setText(monitorId);

        imageId = preference.getInt(IMAGE_ID_KEY, 1);

        camera2BasicFragment = Camera2BasicFragment.newInstance();

        stopTakingPicturesButton = findViewById(R.id.stop_taking_pictures_button);

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.camera_container, camera2BasicFragment)
                    .commit();
        }


        handler = new Handler();
        takingPicturesRunnable = new Runnable() {
            @Override
            public void run() {
                logger.info("taking picture");

                try {
                    camera2BasicFragment.takePicture();
                } catch (java.lang.NullPointerException e) {
                    logger.error(String.format("exception while taking picture %s", imageId), e);

                    // restart the fragment if cant not take a picture
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.camera_container, camera2BasicFragment)
                            .commit();

                }
                addToImageIdCounter();
                handler.postDelayed(takingPicturesRunnable, imageFrequencyMili);
            }
        };

        stopTakingPicturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTakingPicturesDialog();
            }
        });
    }



    public void addToImageIdCounter() {
        imageId += 1;
        preference.edit().putInt(IMAGE_ID_KEY, imageId).apply();
    }

    public void startTakingPictures() {
//        if (!handler.hasCallbacks(takingPicturesRunnable)) {
//            logger.info(String.format("start taking pictures: monitor-id: %s resolution: %s frequency: %s",
//                    ));
            handler.postDelayed(takingPicturesRunnable, DELAY_BEFORE_TAKING_PICTURES_MILLIS);
//        }
    }

    public void stopTakingPictures() {
        handler.removeCallbacks(takingPicturesRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTakingPictures();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTakingPictures();
    }

    public void stopTakingPicturesDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setMessage(R.string.stop_taking_pictures_dialog)
                .setPositiveButton(R.string.stop_taking_picture_dialog_keep_taking_pictures, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(R.string.stop_taking_pictures_dialog_stop_taking_pictures, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopTakingPictures();
                        finish();
                    }
                }).show();
    }

    public int getImageId() {
        return imageId;
    }

    // maybe this should not be here:

    public void updateApp() {
        ApkDownloader apkDownloader = new ApkDownloader(serverUrl, App.APK_PATH);
        preference.edit().putBoolean(AppConstants.IS_TAKING_PICTURES_KEY, true).apply();
        apkDownloader.execute(this);
    }
}