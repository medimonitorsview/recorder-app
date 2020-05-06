package org.mdeimonitorsview.android.recorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.mdeimonitorsview.android.recorder.classes.Segment;


public class ReferencesImage {

    public int imageId;
    public  Collection<Segment> markedSegments;
    public  ArrayList<Segment> detectedSegments;
    public int dx;
    public int dy;


    public ReferencesImage(int imageId, ArrayList<Segment> detectedSegments, Collection<Segment> markedSegments, int dx, int dy) {
        this.imageId = imageId;
        this.markedSegments = markedSegments;
        this.detectedSegments = detectedSegments;
        this.dx = dx;
        this.dy = dy;
    }

    public boolean HasUpdates(HashMap<String, Segment> newSegments){
        if (newSegments.size() != markedSegments.size()){
            return true;
        }
        int countSame = 0;
        for (Segment s : markedSegments){
            if (newSegments.containsKey(s.name) && s.sameSegment(newSegments.get(s.name))){
                countSame++;
            }
        }
        if (countSame != markedSegments.size()){
            return true;
        }
        return false;
    }



}
