package org.mdeimonitorsview.android.recorder.publishers;

import org.mdeimonitorsview.android.recorder.classes.Segment;

import java.util.ArrayList;

// each filed has to have getter and setter to be able to
// work with gson
public class MonitorData {

    private int imageId;
    private String monitorId;
    private long timestamp;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    private int version;

    private ArrayList<Segment> segments;

    public MonitorData(ArrayList<Segment> segments, int imageId, String monitorId, long timestamp) {
        this.setSegments(segments);
        this.imageId = imageId;
        this.monitorId = monitorId;
        this.timestamp = timestamp;
    }

    public MonitorData(ArrayList<Segment> segments, long timestamp) {
        this.setSegments(segments);
        this.timestamp = timestamp;

        // use full class path to avoid confusion with other modules
        this.version = org.mdeimonitorsview.android.recorder.BuildConfig.VERSION_CODE;
    }
    public MonitorData(){
    }
    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(String monitorId) {
        this.monitorId = monitorId;
    }

    public ArrayList<Segment> getSegments() {
        return segments;
    }

    public void setSegments(ArrayList<Segment> segments) {
        this.segments = segments;
    }
}
