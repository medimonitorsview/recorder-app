package org.mdeimonitorsview.android.recorder;


import android.graphics.Rect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.jetbrains.annotations.NotNull;
import org.mdeimonitorsview.android.recorder.classes.Segment;
import org.opencv.core.Point;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.mdeimonitorsview.android.recorder.App.staticLogger;


public class SegmentsSyncer {

    private static final String GET_MONITOR_DATA_REST = "api/monitor";

    public static int frequency;
    private final String serverUrl;
    private final String monitorId;
    private static MonitorInformation monitorInformation;

    //    Handler uiHandler = new android.os.Handler(Looper.getMainLooper()) {
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            Toast.makeText()
//        }
//    }
    public static class MonitorInformation{
        public HashMap<String, Segment> segments;
        public int imageId;
        // the points of the screen. *Don't* assume anything about their order, the settings app seems to mess stuff.
        // And the naming in spec is a bit confusing.
        public Point[] screenPoints = null;
        public MonitorInformation(){
            segments = new HashMap<>();
        }
    }

    public static class MonitorInformationDeSerializer implements JsonDeserializer<MonitorInformation> {

        @Override
        public MonitorInformation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MonitorInformation monitorInformation = new MonitorInformation();


            JsonObject jsonObject = json.getAsJsonObject();
            JsonArray segments = jsonObject.get("segments").getAsJsonArray();
            JsonElement imageId = jsonObject.get("imageId");
            if (jsonObject.has("screenCorners") && jsonObject.get("screenCorners").isJsonObject()){
                try {
                    JsonObject corners = jsonObject.get("screenCorners").getAsJsonObject();
                    monitorInformation.screenPoints = new Point[4];
                    JsonElement lto = corners.get("left-top");
                    JsonArray lt = lto.getAsJsonArray();
                    monitorInformation.screenPoints[0] = new Point(lt.get(0).getAsFloat(), lt.get(1).getAsFloat());
                    JsonArray bl = corners.get("bottom-left").getAsJsonArray();
                    monitorInformation.screenPoints[1] = new Point(bl.get(0).getAsFloat(), bl.get(1).getAsFloat());
                    JsonArray br = corners.get("bottom-right").getAsJsonArray();
                    monitorInformation.screenPoints[2] = new Point(br.get(0).getAsFloat(), br.get(1).getAsFloat());
                    JsonArray rt = corners.get("right-top").getAsJsonArray();
                    monitorInformation.screenPoints[3] = new Point(rt.get(0).getAsFloat(), rt.get(1).getAsFloat());
                }
                catch (Exception e){
                    staticLogger.error("Unable to parse screen corners");
                }
            }

            if (imageId!=null) {
                monitorInformation.imageId = imageId.getAsInt();
            }
            for (JsonElement jsonElement : segments) {
                JsonObject croppingElement = jsonElement.getAsJsonObject();
                JsonElement name = croppingElement.get("name");
                if (name.isJsonNull()){
                    continue;
                }
                Rect rect = new Rect(
                        croppingElement.get("left").getAsInt(),
                        croppingElement.get("top").getAsInt(),
                        croppingElement.get("right").getAsInt(),
                        croppingElement.get("bottom").getAsInt());
                Segment segment = new Segment(rect);
                segment.name = name.getAsString();
                monitorInformation.segments.put(segment.name, segment);
            }

            return monitorInformation;
        }

        private String getString(JsonElement je){
            if(je == null || je.isJsonNull()) {
                return null;
            }
            return je.getAsString();
        }
    }

    private final UiHandler uiHandler;

    SegmentsSyncer(String baseUrl, String monitorId, UiHandler uiHandler) {
        this.serverUrl = baseUrl;
        this.monitorId = monitorId;
        this.uiHandler = uiHandler;
    }

    public void getMonitorData () {

        if (monitorId == null) {
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s/%s/%s",
                serverUrl, GET_MONITOR_DATA_REST, monitorId);
        final Request request = new Request.Builder()
                .url(url)
                .build();

        staticLogger.info("send request " + request);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {}

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String json = null;
                try {
                    json = response.body().string();
                } catch (IOException e) {
                    staticLogger.error(String.format("error in response to %s", call.request()), e);
                    uiHandler.showToast(R.string.can_not_get_segments_from_server_string);
                }

                Gson gson = new GsonBuilder()
                        .serializeNulls()
                        .registerTypeAdapter(MonitorInformation.class, new MonitorInformationDeSerializer())
                        .create();
                try {
                    SetMonitorInformation(gson.fromJson(json, MonitorInformation.class));
                } catch (com.google.gson.JsonSyntaxException e) {
                    staticLogger.error(String.format("got bad data from server: %s", json));
                    uiHandler.showToast("מקבל סגמנטים שגויים מהשרת");
                }
            }
        });
    }
    public static synchronized void SetMonitorInformation(MonitorInformation mi){
        monitorInformation = mi;
    }
    //todo: remove the static
    public static synchronized MonitorInformation getSegments() {
        return monitorInformation;
    }
}
