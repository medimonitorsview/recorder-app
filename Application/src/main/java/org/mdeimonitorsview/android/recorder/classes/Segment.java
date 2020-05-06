package org.mdeimonitorsview.android.recorder.classes;

import android.graphics.Rect;

public class Segment {

    public static final int SCORE_FAILED = 0;
    public static final String SOURCE = "device";


    public int left = 0;
    public int right = 0;
    public int bottom = 0;
    public int top = 0;
    public String name = null;
    public String value = null;
    public String source = null;
    public String level = null;

    public float score = 0;

    public Segment() {
    }

    public Segment(Rect _rect) {
        setRect(_rect);
        name = null;
        value = null;
        score = 0.0f;
        level = null;
        source = Segment.SOURCE;
    }

    public Segment(Segment that) {
        this.setRect(that.getRect());
        this.name = that.name;
        this.value = that.value;
        this.source = that.source;
        this.level = that.level;
        this.score = that.score;
    }


    public Rect getRect() {
        return new Rect(left, top, right, bottom);
    }

    public void setRect(Rect rect) {
        this.left = rect.left;
        this.top = rect.top;
        this.bottom = rect.bottom;
        this.right = rect.right;
    }

    public boolean sameSegment(Segment s) {
        return s.top == top && s.left == left && s.bottom == bottom && s.right == right
                && ((s.name == null && name==null) || s.name.equals(name));
    }
}
