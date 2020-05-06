package org.mdeimonitorsview.android.recorder.classes;

import android.util.Size;

public class CameraData {

    private Size[] supportedResolutions;
    private boolean manualFocusSupported;

    public Size[] getSupportedResolutions() {
        return supportedResolutions;
    }

    public void setSupportedResolutions(Size[] supportedResolutions) {
        this.supportedResolutions = supportedResolutions;
    }

    public boolean isManualFocusSupported() {
        return manualFocusSupported;
    }

    public void setManualFocusSupported(boolean manualFocusSupported) {
        this.manualFocusSupported = manualFocusSupported;
    }
}
