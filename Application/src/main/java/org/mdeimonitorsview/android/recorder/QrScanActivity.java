package org.mdeimonitorsview.android.recorder;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    ZXingScannerView mScannerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pa// use
    }

    @Override
    public void handleResult(Result result) {
        String monitorId = result.getText();
        int resultCode = Activity.RESULT_OK;

        if  (!monitorId.startsWith(getString(R.string.qr_monitor_prefix))) {
            Toast.makeText(this, "אנא סרוק בר קוד שמתאים למוניטור", Toast.LENGTH_LONG).show();
            resultCode = MainActivity.WRONG_QR_RESULT_CODE;
        }

        Intent intent = new Intent();
        intent.putExtra(MainActivity.MONITOR_ID_KEY, monitorId);
        setResult(resultCode, intent);
        finish();
    }
}
