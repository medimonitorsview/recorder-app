package org.mdeimonitorsview.android.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;

import com.google.android.gms.vision.text.TextRecognizer;

import org.mdeimonitorsview.android.recorder.classes.CameraData;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity {
    public static final String IMAGE_RESOLUTION_INDEX = "IMAGE_RESOLUTION_INDEX";
    public static final String IMAGE_RESOLUTION_STRING = "IMAGE_RESOLUTION_STRING";
    public static final String SERVER_URL_STRING = "SERVER_URL_STRING";
    public static final String IMAGE_FREQUENCY_KEY = "IMAGE_FREQUENCY";
    public static final String MONITOR_ID_KEY = "MONITOR_ID";


    public static final int WRONG_QR_RESULT_CODE = 2;

    public static final int QR_ACTIVITY_REQUEST_CODE = 1;
    public static final int IMAGE_FREQUENCY_DEFAULT_MILI = 2000;
    public static final int SAGMMENT_SYNCER_FREQUENCY = 5000;
    public static final String TAG = MainActivity.class.getSimpleName();


    ZXingScannerView mScannerView;

    Button resolutionPlus;
    Button resolutionMinus;
    Button scanQrButton;

    TextView currentResolution;
    EditText serverUrlEt;
    EditText imageFrequencyEt;

    ResolutionViewModel resolutionViewModel;

    private Size imageResolution;

    public Size[] supportedResolution;
    private SharedPreferences preference;
    private String image_resolution_index;
    ViewGroup view;

    HashMap<String, Rect> croppingMap;

    private String monitorId;
    private String serverUrl;
    public SegmentsSyncer segmentsSyncer;
    UiHandler uiHandler;

    private Runnable segmentsSyncerRunnable;
    private Handler segmentSyncerHandler;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            }
        }

        // this ui handler is serve the  SegmentSyncer which keeps working in
        // the CameraActivity. that why we use here the application context
        uiHandler = new UiHandler(Looper.getMainLooper(), getApplicationContext());
        segmentSyncerHandler = new Handler();

        preference = PreferenceManager.getDefaultSharedPreferences(this);

        CameraData cameraData = getCameraData();
        
        supportedResolution = cameraData.getSupportedResolutions();
        Log.d(TAG, "manual focus supported: " + String.valueOf(cameraData.isManualFocusSupported()));
        uiHandler.showToast(String.valueOf(cameraData.isManualFocusSupported()));

        resolutionPlus = findViewById(R.id.plus);
        resolutionMinus = findViewById(R.id.minus);
        scanQrButton = findViewById(R.id.scan_qr_bt);
        currentResolution = findViewById(R.id.resolution_string_tv);

        serverUrlEt = findViewById(R.id.server_url_et);
        serverUrl = preference.getString(SERVER_URL_STRING, getString(R.string.default_server_url));
        serverUrlEt.setText(serverUrl);

        monitorId = preference.getString(MONITOR_ID_KEY, null);

        if (monitorId != null) {
            startSegmentsSyncer();
        }

        imageFrequencyEt = findViewById(R.id.image_frequncy_et);

        Integer imageFrequency = preference.getInt(IMAGE_FREQUENCY_KEY, IMAGE_FREQUENCY_DEFAULT_MILI);
        imageFrequencyEt.setText(imageFrequency.toString());

        ImageButton startTakingPicturesButton = findViewById(R.id.start_taking_pictures_button);
        startTakingPicturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraActivity();
            }
        });


        final Observer<Integer> resolutionObserver  = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {

                imageResolution = supportedResolution[resolutionViewModel.getResolutionIndex().getValue()];
                currentResolution.setText(imageResolution.toString());
            }
        };


        resolutionViewModel = ViewModelProviders.of(this).get(ResolutionViewModel.class);
        resolutionViewModel.getResolutionIndex().setValue(preference.getInt(IMAGE_RESOLUTION_INDEX, 0));

        resolutionViewModel.getResolutionIndex().observe(this, resolutionObserver);

        mScannerView = new ZXingScannerView(this);

        resolutionPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseResolutionValue();
            }
        });

        resolutionMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseResolutionValue();
            }
        });


        scanQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQr();
            }
        });

        if (preference.getBoolean(AppConstants.IS_TAKING_PICTURES_KEY, false)) {

            // change the app mode back to normal mode after the installing
            preference.edit().putBoolean(AppConstants.IS_TAKING_PICTURES_KEY, false).apply();
            startCameraActivity();
        }
    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        }, 101);
    }

    private boolean checkIfAlreadyHavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private  void scanQr() {
        Intent intent = new Intent(this, QrScanActivity.class);
        startActivityForResult(intent, QR_ACTIVITY_REQUEST_CODE);
    }

    public void increaseResolutionValue() {
        if (supportedResolution == null) {
            return;
        }

        resolutionViewModel.getResolutionIndex().setValue(Math.min(resolutionViewModel.getResolutionIndex().getValue() + 1, supportedResolution.length - 1));
        preference.edit().putInt(IMAGE_RESOLUTION_INDEX, resolutionViewModel.getResolutionIndex().getValue()).apply();

    }

    public void decreaseResolutionValue() {
        resolutionViewModel.getResolutionIndex().setValue(Math.max(resolutionViewModel.getResolutionIndex().getValue() - 1, 0));
        preference.edit().putInt(IMAGE_RESOLUTION_INDEX, resolutionViewModel.getResolutionIndex().getValue()).apply();
    }

    private void startCameraActivity() {

//        ApkDownloader apkDownloader = new ApkDownloader(serverUrl, AppConstants.APK_PATH);
//        apkDownloader.execute(this);

        TextRecognizer tr = new TextRecognizer.Builder(this.getApplicationContext()).build();
        if (tr == null || !tr.isOperational()) {
            Toast.makeText(this, "OCR לא תקין, אנא חבר את המכשיר לאינטרנט הפעל מחדש את האפליקציה ולחץ על כפתור זה שנית", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, "OCR מוכן", Toast.LENGTH_SHORT).show();
        }

        if (monitorId == null) {
            Toast.makeText(this, "לא הוגדר מכשיר, אנא הגדר מכשיר", Toast.LENGTH_LONG).show();
            return;
        }
        Intent cameraActivityIntent = new Intent(this, CameraActivity.class);
        cameraActivityIntent.putExtra(IMAGE_RESOLUTION_INDEX, resolutionViewModel.getResolutionIndex().getValue());
        cameraActivityIntent.putExtra(IMAGE_RESOLUTION_STRING, supportedResolution[resolutionViewModel.getResolutionIndex().getValue()].toString());

        serverUrl = serverUrlEt.getText().toString();
        cameraActivityIntent.putExtra(SERVER_URL_STRING, serverUrl);
        preference.edit().putString(SERVER_URL_STRING, serverUrl).apply();

        int imageFrequency = Integer.parseInt(imageFrequencyEt.getText().toString());
        preference.edit().putInt(IMAGE_FREQUENCY_KEY, imageFrequency).apply();


        cameraActivityIntent.putExtra(IMAGE_FREQUENCY_KEY, imageFrequency);
        cameraActivityIntent.putExtra(MONITOR_ID_KEY, monitorId);

        startActivity(cameraActivityIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CameraData getCameraData() {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        CameraData cameraData = new CameraData();
        try {
            CameraCharacteristics characteristics
                    = Objects.requireNonNull(manager).getCameraCharacteristics(manager.getCameraIdList()[0]);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            cameraData.setSupportedResolutions(Objects.requireNonNull(map).getOutputSizes(ImageFormat.JPEG));

            int[] capabilities = characteristics
                    .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            boolean isManualFocusSupported = IntStream.of(capabilities)
                    .anyMatch(x -> x == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
            cameraData.setManualFocusSupported(isManualFocusSupported);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
        return cameraData;
    }


    //todo: turn off the first asynctask
    private void startSegmentsSyncer() {
        segmentsSyncer = new SegmentsSyncer(serverUrl, monitorId, uiHandler);
        segmentsSyncerRunnable = new Runnable() {
            @Override
            public void run() {
                segmentsSyncer.getMonitorData();
                segmentSyncerHandler.postDelayed(segmentsSyncerRunnable, SAGMMENT_SYNCER_FREQUENCY);
            }
        };
        segmentSyncerHandler.postDelayed(segmentsSyncerRunnable, 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            monitorId = data.getStringExtra(MONITOR_ID_KEY);
            preference.edit().putString(MONITOR_ID_KEY, monitorId).apply();
            startSegmentsSyncer();
        }
    }
}
