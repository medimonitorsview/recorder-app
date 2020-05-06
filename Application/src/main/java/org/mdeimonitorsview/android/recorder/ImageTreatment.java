package org.mdeimonitorsview.android.recorder;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.internal.fk;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.mdeimonitorsview.android.recorder.classes.Segment;
import org.mdeimonitorsview.android.recorder.publishers.MonitorData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.mdeimonitorsview.android.recorder.App.OCR_FILES_DIR;
import static org.mdeimonitorsview.android.recorder.App.staticLogger;

public class ImageTreatment {

    public static int DOT_LOG_OCR;
    // this variable determine if
//    private int logOcrImageId;

    private Ocr ocr;
    //    private final Activity mActivity;
    private final UiHandler uiHandler;

    private ReferencesImage referencesImage;

    SharedPreferences preference;

    public static final int MAX_DISTANCE = 200;
    public static final int MIN_MATCHES = 6;
    public static final boolean EXPAND_SEGMENT_ENHANCMENT = true;
    public static Ocr.MatchResult lastMatch = new Ocr.MatchResult();
    public final static Gson gson = new GsonBuilder().serializeNulls().create();

    ImageTreatment(TextRecognizer textRecognizer, UiHandler uiHandler, SharedPreferences preference) {
        this.preference = preference;
        this.ocr = new Ocr(textRecognizer);
        this.uiHandler = uiHandler;
//        logOcrImageId = 0;
    }

//    public int getLogOcrImageId() {
//        return logOcrImageId;
//    }
//
//    public void setLogOcrImageId(int logOcrImageId) {
//        this.logOcrImageId = logOcrImageId;
//    }

    public boolean isOperational() {
        return ocr.isOperational();
    }

    public Segment getMeasurement(Bitmap bitmap, Segment segment) {

        Bitmap croppedPart = crop(bitmap, segment.getRect());
        if (croppedPart == null) {
            //uiHandler.showToast("bad segments - need to be reconfigured");
            return new Segment(segment);
        }

        Segment detected_segment = new Segment(segment);
        RecognizeText(croppedPart, detected_segment);
        return detected_segment;
    }


    public ArrayList<Segment> getAllMeasurement(Bitmap bitmap, SegmentsSyncer.MonitorInformation monitorInformation, int imageId) {
        long startTime = System.nanoTime();
        //Bitmap bitmap = original_bitmap.copy(original_bitmap.getConfig(), true);

        if ((imageId % AppConstants.IMAGE_SAVING_FREQUENCY) == 0) {
            Bitmap dst = bitmap.copy(bitmap.getConfig(), false);
            DataSaver.getInstance().save(dst, imageId);
        }

        ImageEnhance ie = new ImageEnhance();
        if (monitorInformation== null || monitorInformation.segments == null || monitorInformation.segments.isEmpty()) {
            return null;
        }
        staticLogger.debug("Received image with segments");


        Segment trackerSegment = new Segment();
        trackerSegment.name = "tracker";
        trackerSegment.top = (int)(bitmap.getHeight()*0.25);
        trackerSegment.right = (int)(bitmap.getWidth()*0.25);
        trackerSegment.bottom = bitmap.getHeight()/2;
        trackerSegment.right = bitmap.getWidth()/2;
        trackerSegment.value = "";
        ArrayList<Segment> markedSegments = new ArrayList<>(monitorInformation.segments.values());
        ArrayList<Segment> detectedSegments = new ArrayList<>();
        ArrayList<Segment> baseDetections = ocr.detectSegments(bitmap);
        detectedSegments.addAll(baseDetections);
        ie.GlobalEnhance(bitmap,monitorInformation.screenPoints);

        ArrayList<Segment> globalEnhanceDetections = ocr.detectSegments(bitmap);
        detectedSegments.addAll(globalEnhanceDetections);

        if (detectedSegments != null) {
            staticLogger.debug("Detected " + detectedSegments.size() + " segments");
        }
        // Assumptions:
        // - no move has been made since segmentation. When application restarts -
        // - when application restarts we can compare the image we see to the segmented image
        boolean hasUpdates = false;
        if (ie.GetMarkedImage() != monitorInformation.imageId){

            String storedSegments =  preference.getString("marked_image_segments",null);
            MonitorData monitorData = null;
            if (storedSegments!=null) {
                monitorData = gson.fromJson(storedSegments, MonitorData.class);
            }
            if (monitorData!=null && monitorData.getImageId() == monitorInformation.imageId) {
                ie.SetMarkedImage(monitorData.getImageId(), monitorData.getSegments());
            }
            else
            {
                MonitorData newMonitorData = new MonitorData(baseDetections, monitorInformation.imageId,null,0);
                String json = gson.toJson(newMonitorData);
                preference.edit().putString("marked_image_segments", json).apply();
                ie.SetMarkedImage(monitorInformation.imageId, baseDetections);
            }

        }

        ie.Track(imageId,detectedSegments,MIN_MATCHES,MAX_DISTANCE);
        Segment[] markedSegmentsInImage = ie.Transform(markedSegments);
        for(int i = 0 ; i < markedSegmentsInImage.length; ++ i){
            // The c++ methods do not handle anything except the location and maybe value.
            markedSegmentsInImage[i].name= markedSegments.get(i).name;
            markedSegmentsInImage[i].value = markedSegments.get(i).value;
            markedSegmentsInImage[i].level = markedSegments.get(i).level;
            markedSegmentsInImage[i].source = markedSegments.get(i).source;
            markedSegmentsInImage[i].score = markedSegments.get(i).score;
        }
        String transform = "";
        for (double x : ie.GetTransformation()){
            transform += " " + String.format("%.3f", x);
        }
        trackerSegment.value += " T: " + transform + " ";
        staticLogger.error("TRANSFORMATION: " + transform);
        // Merge the detected segments with the requested segments to fill values in requested segments.
        ArrayList<Segment> mergedSegments = ocr.updateSegments(detectedSegments, markedSegmentsInImage);
        if (mergedSegments == null || mergedSegments.isEmpty()) {
            return null;
        }


        for (Segment s : markedSegmentsInImage) {
            Segment segment = getMeasurement(bitmap, s);
            segment.level = "crop";
            mergedSegments.add(segment);
        }

        ie.SegmentsEnhance(bitmap, markedSegmentsInImage, EXPAND_SEGMENT_ENHANCMENT);
        for (Segment s : markedSegmentsInImage) {
            Segment segment = getMeasurement(bitmap, s);
            segment.level = "crop enhance";
            mergedSegments.add(segment);
        }


        long endTime = System.nanoTime();

        // get difference of two nanoTime values
        long timeElapsed = (endTime - startTime) / 1000000;
        trackerSegment.value += " time (ms):" + timeElapsed;
        Segment[] transformedTracker = ie.Transform(trackerSegment);
        if (transformedTracker!=null && transformedTracker.length>0){
            trackerSegment.top = transformedTracker[0].top;
            trackerSegment.left = transformedTracker[0].left;
            trackerSegment.right = transformedTracker[0].right;
            trackerSegment.bottom = transformedTracker[0].bottom;
        }
        mergedSegments.add(trackerSegment);
        return mergedSegments;
    }


    public Bitmap convertByteArrayToBitmap(byte[] bytes) {

        if (bytes == null) {
            return null;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return bitmap;
        } catch (Exception e) {
            //Log
            return null;
        }
    }

    public byte[] convertImageToByteArray(Image image) {

        if (image == null) {
            return null;
        }

        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }


    private Bitmap crop(Bitmap bitmap, Rect rect) {

        try {
            Log.d(getClass().getSimpleName(), rect.toString());
            rect.left = Math.max(0, rect.left);
            rect.top = Math.max(0, rect.top);
            rect.bottom = Math.min(rect.bottom, bitmap.getHeight());
            rect.right = Math.min(rect.right, bitmap.getWidth());
            bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        } catch (IllegalArgumentException e) {
            staticLogger.error("try to crop part which is out of image", e);
            return null;
        }

        return bitmap;
    }


    private void RecognizeText(Bitmap bitmap, Segment segment) {
        try {
            Frame.Builder f = new Frame.Builder();
            f.setBitmap(bitmap);
            SparseArray<TextBlock> dataReco = null;
            dataReco = ocr.textRecognizer.detect(f.build());

            if (dataReco == null || dataReco.size() != 1) {
                segment.score = Segment.SCORE_FAILED;
                segment.value = null;
                return;
            }

            String ocrValue = dataReco.valueAt(0).getValue();
            if (ocrValue == null) {
                segment.score = Segment.SCORE_FAILED;
                segment.value = null;
            } else {
                updateOcrScore(dataReco.valueAt(0), segment);
                segment.value = ocrValue;
            }

            Log.d(getClass().getSimpleName(), ocrValue);
        } catch (Exception e) {
            staticLogger.error("got exception", e);
        }
    }


    private void updateOcrScore(TextBlock text, Segment segment) {
        try {
            Field field = text.getClass().getDeclaredField("zzbNQ");
            field.setAccessible(true);
            com.google.android.gms.internal.fk[] fkArr = (fk[]) field.get(text); //IllegalAccessException
            fk fk = fkArr[0];
            field = fk.getClass().getDeclaredField("zzbOc");
            field.setAccessible(true);
            Float score = (Float) field.get(fk);
            if (score.isNaN()) {
                segment.score = (Segment.SCORE_FAILED);
            } else {
                segment.score = (score.floatValue());
            }
        } catch (NoSuchFieldException exception) {
            staticLogger.warn(exception.getMessage());
            segment.score = (Segment.SCORE_FAILED);
        } catch (IllegalAccessException exception) {
            staticLogger.warn(exception.getMessage());
            segment.score = (Segment.SCORE_FAILED);
        } catch (Exception exception) {
            staticLogger.warn(exception.getMessage());
            segment.score = Segment.SCORE_FAILED;
        }
    }


    //todo: check that all the thread not killing the system
    public void saveImage(final Bitmap bitmap, int imageId) {
        if (imageId % 2 == 0) {
            Date myDate = new Date();
            String folder_timed = new SimpleDateFormat("yyy_MM_dd_HH").format(new Date());
            File folder = new File(OCR_FILES_DIR + "/" + folder_timed + "/");
            if (! folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, Integer.toString(imageId) + ".jpg");
            Log.d("imageSaver", "save image: " + file);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 1, out); // bmp is your Bitmap instance
            } catch (IOException e) {
                staticLogger.error("can not save file: " + file.getAbsolutePath(), e);
            }

        }
    }
}
//        new Thread(() -> {
//            try (FileOutputStream out = new FileOutputStream(file)) {
//                bitmap.compress(Bitmap.CompressFormat.PNG, 1, out); // bmp is your Bitmap instance
//                // PNG is a lossless format, the compression factor (100) is ignored
//            } catch (IOException e) {
//                staticLogger.error("can not save file: " + file.getAbsolutePath(), e);
//            }
//        }).start();
//    }
//}

