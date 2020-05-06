package org.mdeimonitorsview.android.recorder.classes;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.mdeimonitorsview.android.recorder.AppConstants;
import org.mdeimonitorsview.android.recorder.BuildConfig;

import java.io.File;
import java.io.IOException;

import eu.sisik.devowner.UtilsKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import static org.mdeimonitorsview.android.recorder.App.staticLogger;

public class ApkDownloader extends AsyncTask<Context, Integer, Boolean> {

    private final String serverUrl;
    private final String apkPath;

    public ApkDownloader(String baseUrl, String apkPath) {
        this.serverUrl = baseUrl;
        this.apkPath = apkPath;
    }

    public void downloadApk(int version) throws IOException {
        Log.d(AppConstants.INSTALLER_TAG, "downloading apk");
        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s/%s/%s/%d",
                serverUrl, AppConstants.GET_APK_REST, AppConstants.APP_NAME, version);
        final Request request = new Request.Builder()
                .url(url)
                .build();

        staticLogger.info("send request " + request);
        Response response = client.newCall(request).execute();
        if (response.code() != 200) {
            staticLogger.error("get wrong response to apk download request: " + response.code());
            return;
        }
        saveFileFromOkHttp(response);
    }

    //todo: finish this
    public int getApkVersionCode() throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s/%s/%s/get_version",
                serverUrl, AppConstants.GET_APK_VERSION_REST, AppConstants.APP_NAME);

        final Request request = new Request.Builder()
                .url(url)
                .build();

        staticLogger.info("send request " + request);

        Response response = client.newCall(request).execute();
        if (response.code() != 200) {
            staticLogger.error(String.format("get wrong response to %s : %d", AppConstants.GET_APK_VERSION_REST, response.code()));
        }
        String responseBody = response.body().string();
        return new Gson().fromJson(responseBody, JsonArray.class).get(0).getAsInt();
    }

    public void saveFileFromOkHttp(Response response) throws IOException {
        Log.d(AppConstants.INSTALLER_TAG, "save apk in path: " + apkPath);
        File downloadedFile = new File(apkPath);
        BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
        sink.writeAll(response.body().source());
        sink.close();
    }

    public int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }


    public void updateApp(Context context) throws IOException {
        Log.d(AppConstants.INSTALLER_TAG, "check apk version");
        int lastVersion = getApkVersionCode();
        if (getVersionCode() < lastVersion) {
            Log.d(AppConstants.INSTALLER_TAG, "start downloading apk");
            downloadApk(lastVersion);
            Log.d(AppConstants.INSTALLER_TAG, "starting installing apk");
            UtilsKt.install(context, context.getPackageName(), apkPath);
        }
    }

    @Override
    protected Boolean doInBackground(Context... contexts) {
        try {
            updateApp(contexts[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
