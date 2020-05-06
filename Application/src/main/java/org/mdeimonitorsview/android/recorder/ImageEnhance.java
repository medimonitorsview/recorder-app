package org.mdeimonitorsview.android.recorder;

import android.graphics.Bitmap;

import org.mdeimonitorsview.android.recorder.classes.Segment;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.Arrays;
import java.util.Collection;


class ImageEnhance {
    static {
        System.loadLibrary("ImageEnhance");
        System.loadLibrary("opencv_java4");
    }


    private native void SegmentProcess(long matAddr, Segment[] segments, boolean expand);
    private  native void GlobalProcess(long matAddr, Point[] screenCorners, float power, float invert_threshold);


    public void GlobalEnhance(Bitmap bitmap, Point[] screenCorners){
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);
        GlobalProcess(image.getNativeObjAddr(),screenCorners,3.0f,255*0.75f);
        Utils.matToBitmap(image, bitmap);
    }

    //expand - epxand the segment a bit to each side to capture additional digits for cases of
    //numbers with variable length
    public void SegmentsEnhance(Bitmap bitmap, Segment[] segments, boolean expand){
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);
        SegmentProcess(image.getNativeObjAddr(),segments, expand);
        Utils.matToBitmap(image, bitmap);
    }

    public native double[] GetTransformation();
    public native int GetRefImage();
    public native int GetMarkedImage();
    private native void SetMarkedImage(int imageId, Segment[] segments);
    private native void Track(int imageId,Segment[] segments,int min_matches, int max_distance);
    private native Segment[] TransformSegments(Segment[] segments);

    public void SetMarkedImage(int imageId, Collection<Segment> segments){
        Segment[] s = segments.toArray(new Segment[segments.size()]);
        SetMarkedImage(imageId,s);
    }

    public void Track(int imageId,Collection<Segment> segments,int min_matches, int max_distance){
        Segment[] s = segments.toArray(new Segment[segments.size()]);
        if (s.length>0) {
            Track(imageId, s, min_matches, max_distance);
        }
    }
    public Segment[] Transform(Segment... segments){
        return Transform(Arrays.asList(segments));
    }
    public Segment[] Transform(Collection<Segment> segments){
        Segment[] s = segments.toArray(new Segment[segments.size()]);
        if (s.length>0) {
            return TransformSegments(s);
        }
        return null;
    }
}