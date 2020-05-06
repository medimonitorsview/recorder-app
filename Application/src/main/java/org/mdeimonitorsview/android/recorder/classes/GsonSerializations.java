package org.mdeimonitorsview.android.recorder.classes;

import android.graphics.Rect;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;

public class GsonSerializations {
//    public static class CroppingHashMapSerializer implements JsonSerializer<HashMap<String,Rect>> {
//
//        @Override
//        public JsonElement serialize(HashMap<String, Rect> src, Type typeOfSrc, JsonSerializationContext context) {
//            JsonArray jsonArray = new JsonArray();
//            for (Map.Entry<String, Rect> entry: src.entrySet()) {
//                if(entry.getValue().mode == MeasurementsRectangle.MODE.categorized) {
//                    JsonObject json = context.serialize(entry.getValue()).getAsJsonObject();
//                    json.addProperty("name", entry.getKey());
//                    jsonArray.add(json);
//                } else {
//                    src.remove(entry.getKey());
//                }
//            }
//            return jsonArray;
//        }
//    }


    public static class MutableLiveDataSerializer implements JsonSerializer<MutableLiveData<String>> {

        @Override
        public JsonElement serialize(MutableLiveData<String> src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.getValue());
        }
    }

//    public static class MeasurementsRectangleSerializer implements JsonSerializer<MeasurementsRectangle> {
//
//        @Override
//        public JsonElement serialize(MeasurementsRectangle src, Type typeOfSrc, JsonSerializationContext context) {
//            return context.serialize(src.rect);
//        }
//    }


    public static class CroppingHashMapDeSerializer implements JsonDeserializer<HashMap> {

        @Override
        public HashMap<String, Segment> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            HashMap<String, Segment> croppingMap = new HashMap<>();

            JsonObject jsonObject = json.getAsJsonObject();
            JsonArray segments = jsonObject.get("segments").getAsJsonArray();

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
                croppingMap.put(segment.name, segment);
            }

            return croppingMap;
        }

        private String getString(JsonElement je){
            if(je == null || je.isJsonNull()) {
                return null;
            }
            return je.getAsString();
        }
    }

}
