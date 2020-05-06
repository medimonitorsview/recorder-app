package org.mdeimonitorsview.android.recorder.publishers;

import org.mdeimonitorsview.android.recorder.UiHandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.mdeimonitorsview.android.recorder.App.staticLogger;


public class ImagePublisher extends BasePublisher {

    /**
     * The JPEG image
     */

    private final String SUFFIX_URL = "api/monitor_image";
    private final String TAG = "Publisher";
    private final byte[] image;
    private boolean sendImage;

    /**
     * The file we save the image into.
     *
     * @return
     */

    public ImagePublisher(byte[] image, boolean sendImage, int imageId, String monitorId, long timestamp, String BaseUrl, UiHandler uiHandler) {
        super(imageId, monitorId, BaseUrl, uiHandler);
        this.image = image;
        this.timeStamp = timestamp;
        this.sendImage = sendImage;
    }

    @Override
    protected String getSuffixUrl() {
        return SUFFIX_URL;
    }

    @Override
    protected void setHeaders(Request.Builder builder) {
        String imageIdString = String.valueOf(imageId);
        builder.addHeader(IMAGE_ID_KEY, imageIdString);

        builder.addHeader(TIME_STAMP_KEY, String.valueOf(timeStamp));

        if (monitorId != null) {
            builder.addHeader(MONITOR_ID_KEY, monitorId);
        }
    }

    @Override
    protected Callback getCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (e instanceof java.net.SocketTimeoutException) {
                    staticLogger.info("timeout: " + call.request().header(IMAGE_ID_KEY));
                    uiHandler.showToast(String.format("%s - image -timeout ", imageId));

                } else if (e instanceof java.net.ConnectException) {
                    staticLogger.info("connection exception: " + call.request().header(IMAGE_ID_KEY));
                    uiHandler.showToast(String.format("%s - image - connection exception", imageId));
                } else {
                    staticLogger.error("network problem: " + call.request().header(IMAGE_ID_KEY), e);
                    uiHandler.showToast(String.format("%s - image - network problem", imageId));
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() != 200) {
                    staticLogger.info(String.format("%s got response code %s: %s",
                            imageId,
                            response.code(),
                            response.body().string()
                            ));
                    uiHandler.showToast(String.format("%s - response code %s", imageId, response.code()));
                }
            }
        };
    }

    @Override
    protected RequestBody BuildRequestBody() {
        if (sendImage) {
            MediaType mediaType = MediaType.parse("image/jpeg");
            return RequestBody.create(image, mediaType);
        }
        else{
            return RequestBody.create(new byte[0], null);
        }
    }
}