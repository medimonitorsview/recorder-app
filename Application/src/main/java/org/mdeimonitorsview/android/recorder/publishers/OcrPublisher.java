package org.mdeimonitorsview.android.recorder.publishers;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.mdeimonitorsview.android.recorder.AppConstants;
import org.mdeimonitorsview.android.recorder.DataSaver;
import org.mdeimonitorsview.android.recorder.ResponseDataSharingListener;
import org.mdeimonitorsview.android.recorder.UiHandler;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.mdeimonitorsview.android.recorder.App.staticLogger;


public class OcrPublisher extends BasePublisher{

    private final static String SUFFIX_URL = "api/monitor_data";
    public final static Gson gson = new GsonBuilder().serializeNulls().create();

    private final MonitorData monitorData;
    public boolean sendNextImage;

    public ResponseDataSharingListener responseDataSharingListener;
    public OcrPublisher(MonitorData monitorData, int imageId, String monitorId, String baseUrl, UiHandler uiHandler, ResponseDataSharingListener responseDataSharingListener) {
        super(imageId, monitorId, baseUrl, uiHandler);
        this.monitorData = monitorData;
        this.monitorData.setImageId(imageId);
        this.monitorData.setMonitorId(monitorId);
        this.responseDataSharingListener = responseDataSharingListener;
    }

    private void handleResponse(JsonObject json) {
        if (json.has("sendImages")) {
            sendNextImage = json.get("sendImages").getAsBoolean();
        }

        if (json.get("autoFocus").getAsBoolean()) {
            responseDataSharingListener.onAutoFocusOn();
        }

        if (json.get("checkUpdate").getAsBoolean()) {
            responseDataSharingListener.onUpdateApp();
        }

    }


    @Override
    protected String getSuffixUrl() {
        return SUFFIX_URL;
    }

    @Override
    protected void setHeaders(Request.Builder builder) {

    }

    @Override
    protected Callback getCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (e instanceof java.net.SocketTimeoutException) {
                    staticLogger.info("timeout: " + call.request().header(IMAGE_ID_KEY));
                    uiHandler.showToast(String.format("%s - ocr - timeout ", imageId));

                } else if (e instanceof java.net.ConnectException) {
                    staticLogger.info("connection exception: " + call.request().header(IMAGE_ID_KEY));
                    uiHandler.showToast(String.format("%s - ocr - connection exception", imageId));
                } else {
                    staticLogger.error("network problem: " + call.request().header(IMAGE_ID_KEY), e);
                    uiHandler.showToast(String.format("%s - ocr - network problem", imageId));
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();
                if (response.code() != 200) {
                    staticLogger.info(String.format("%s - ocr - response code %s: %s",
                            imageId,
                            response.code(),
                            responseText
                    ));
                    return;
                }

                JsonObject json = new Gson().fromJson(responseText, JsonObject.class);
                if (json != null && json.has("sendImages")) {
                    sendNextImage = json.get("sendImages").getAsBoolean();
                }

                handleResponse(json);
                uiHandler.showToast(String.format("%s - ocr - response code %s", imageId, response.code()));
            }
        };
    }

    @Override
    protected RequestBody BuildRequestBody() {
        MediaType mediaType = MediaType.parse("application/json");
        String json = gson.toJson(monitorData);

        if (imageId % AppConstants.IMAGE_SAVING_FREQUENCY == 0) {
            DataSaver.getInstance().save(json, imageId);
        }

        Log.d("json", "json" + json);
        return RequestBody.create(gson.toJson(monitorData), mediaType);
    }
}
