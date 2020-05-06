package org.mdeimonitorsview.android.recorder;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.internal.fk;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Element;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.mdeimonitorsview.android.recorder.classes.Segment;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Ocr {

    public static class MatchResult{
        public int dx = 0;
        public int dy = 0;
        public int matchCount = 0;
        public boolean changeRef=false;
        public MatchResult(){}
        public String toString(){
            return "dx:" + dx + " dy:" + dy + " change:" + changeRef + " matches: " + matchCount;
        }
    }

    public static final float INTERSECTION_THRESHOLD = 0.3f;
    public static org.slf4j.Logger staticLogger = LoggerFactory.getLogger("static-logger");

    public final TextRecognizer textRecognizer;


    public Ocr(TextRecognizer textRecognizer){
        this.textRecognizer = textRecognizer;
    }

    public boolean isOperational(){
        if (textRecognizer == null) {
            return false;
        }
        return textRecognizer.isOperational();
    }
    public Bitmap EnhacneImage(Bitmap bitmap){
//        Mat image = new Mat();
//        Mat fimage = new Mat();
//        Utils.bitmapToMat(bitmap, image);
//        image.convertTo(fimage, CvType.CV_32FC3);
//        Core.divide(fimage,new Scalar(255.0f),fimage);
//        Core.pow(fimage,10.0f,fimage);
//        Core.multiply(fimage,new Scalar(255.0f),fimage);
//        fimage.convertTo(image,CvType.CV_8UC3);
//        Utils.matToBitmap(image, bitmap);
//        return bitmap;
        return null;
    }
    public ArrayList<Segment> detectSegments(Bitmap bitmap){
        Frame.Builder f = new Frame.Builder();
        f.setBitmap(bitmap);
        SparseArray<TextBlock> dataReco = null;
        dataReco = textRecognizer.detect(f.build());

        // todo put null or empty
        if (dataReco == null) {
            return null;
        }
        return getTextElements(dataReco);

    }

    private float getOcrScore(TextBlock text)
    {
        try {
            Field field = text.getClass().getDeclaredField("zzbNQ");
            field.setAccessible(true);
            com.google.android.gms.internal.fk[] fkArr = (fk[]) field.get(text); //IllegalAccessException
            fk fk = fkArr[0];
            field = fk.getClass().getDeclaredField("zzbOc");
            field.setAccessible(true);
            Float score = (Float) field.get(fk);
            if (score.isNaN())
            {
                return (Segment.SCORE_FAILED);
            }
            else
            {
                return (score.floatValue());
            }
        }
        catch (NoSuchFieldException exception)
        {
            staticLogger.warn(exception.getMessage());
            return (Segment.SCORE_FAILED);
        }
        catch (IllegalAccessException exception) {
            staticLogger.warn(exception.getMessage());
            return (Segment.SCORE_FAILED);
        }
        catch (Exception exception)
        {
            staticLogger.warn(exception.getMessage());
            return Segment.SCORE_FAILED;
        }
    }
    private void populateSegments(Text data, ArrayList<Segment> segments, float score){
        List<? extends Text> parts = data.getComponents();
        if (parts == null || parts.isEmpty() || data instanceof Element){
            Segment segment = new Segment(data.getBoundingBox());
            segment.value = data.getValue();
            segment.level =  data.getClass().getSimpleName();
            segment.source = Segment.SOURCE;
            segment.score = score;
            segments.add(segment);
        }
        else
        {
            for (int j = 0; j < parts.size(); j++) {
                populateSegments(parts.get(j), segments, score);
            }
        }
    }

    private ArrayList<Segment> getTextElements(SparseArray<TextBlock> data){
        ArrayList<Segment> segments = new ArrayList<>();
        staticLogger.debug("Got " + data.size() + " text blocks");
        for (int i = 0; i < data.size() ; i++) {
            TextBlock textBlock = data.valueAt(i);
            float score = getOcrScore(textBlock);
            populateSegments(textBlock, segments, score);
        }
        return segments;
    }

    private float intersectionPercent(Rect A, Rect B){
        int xA = Math.max(A.left, B.left);
        int yA = Math.max(A.top, B.top);
        int xB = Math.min(A.right, B.right);
        int yB = Math.min(A.bottom, B.bottom);
        float interArea =  Math.abs(Math.max(xB - xA, 0)) * Math.max(yB - yA, 0);
        float minArea = Math.min(A.height()*A.width(),B.height()*B.width());
        return interArea / minArea;
    }

    private float[][] computeIntersections(Segment[] markedSegments, Segment[] detectedSegments){

        float[][] intersections = new float[markedSegments.length][detectedSegments.length];
        for (int i=0;i<markedSegments.length;i++){
            for (int j=0;j<detectedSegments.length;j++) {

                Rect markedRect = markedSegments[i].getRect();
                Rect detectedRect = detectedSegments[j].getRect();
                intersections[i][j] = intersectionPercent(markedRect, detectedRect);
            }
        }
        return intersections;
    }

    ArrayList<Segment> filterDetections(ArrayList<Segment> detectedSegments, Segment marked){
        if (detectedSegments == null || detectedSegments.size()<=1){
            return detectedSegments;
        }
        Collections.sort(detectedSegments, (s1, s2) -> s1.left - s2.left);
        ArrayList<Segment> result = new ArrayList<>();
        Rect markedRect= marked.getRect();
        float heightMarked = markedRect.bottom - markedRect.top;

        result.add(detectedSegments.get(0));

        float lastEnd=detectedSegments.get(0).right;
        float lastTop=detectedSegments.get(0).top;
        for(int i = 1 ; i < detectedSegments.size(); ++i){

            Segment detectedSegment = detectedSegments.get(i);
            Rect detectedRect = detectedSegment.getRect();

            float heightDetected = detectedRect.bottom - detectedRect.top;
            boolean topClose = Math.abs(detectedRect.top - lastTop)<0.5 * heightDetected;
            float ratio = heightDetected / heightMarked;

            if (0.66 < ratio && ratio < 1.33 && lastEnd <= detectedRect.left && topClose) {
                result.add(detectedSegments.get(i));
                lastEnd = detectedRect.right;
                lastTop = detectedRect.top;
            }
        }
        return result;
    }

    private Segment mergeDetections(Segment markedSegment, ArrayList<Segment> detectedSegments, ArrayList<Float> intersectionScores){
        Segment outSegment = new Segment(markedSegment);

        if (detectedSegments.size() == 1){
            Segment res = detectedSegments.get(0);
            outSegment.value = res.value;
            outSegment.score = res.score;
            outSegment.level = "merged 1";
        }
        // Basic merging logic
        if (detectedSegments.size() > 1){
            ArrayList<Segment> filteredDetections = filterDetections(detectedSegments, markedSegment);
            String value = "";
            float score = 0;
            for(int i=0; i<filteredDetections.size(); ++i  ){
                value += filteredDetections.get(i).value;
                score += filteredDetections.get(i).score;
            }
            score /= filteredDetections.size();
            outSegment.value = value;
            outSegment.score = score;
            outSegment.level = "merged " + filteredDetections.size();
        }

        return outSegment;
    }


    public ArrayList<Segment> updateSegments(ArrayList<Segment> detectedSegmentsList, Segment[] markedSegments) {
        try {
            ArrayList<Segment> updatedSegments = new ArrayList<Segment>();
            Segment[]  detectedSegments = detectedSegmentsList.toArray(new Segment[detectedSegmentsList.size()]);


            float[][] intersectionsScores = computeIntersections(markedSegments, detectedSegments);

            for (int i = 0 ; i<markedSegments.length; ++i) {
                Segment markedSegment = markedSegments[i];
                ArrayList<Segment> intersections = new ArrayList<Segment>();
                ArrayList<Float> scores = new ArrayList<Float>();
                for (int j = 0; j < detectedSegments.length; ++j) {
                    if (intersectionsScores[i][j] > INTERSECTION_THRESHOLD) {
                        intersections.add(detectedSegments[j]);
                        scores.add(intersectionsScores[i][j]);
                    }
                }
                updatedSegments.add(mergeDetections(markedSegment, intersections, scores));
                updatedSegments.addAll(intersections);
            }
            return  updatedSegments;
        }
        catch (Exception e)
        {
            staticLogger.error("got exception", e);
        }
        return null;
    }
}
