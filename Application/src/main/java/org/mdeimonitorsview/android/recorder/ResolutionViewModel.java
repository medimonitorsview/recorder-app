package org.mdeimonitorsview.android.recorder;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ResolutionViewModel extends ViewModel {

    private MutableLiveData<Integer> resolutionIndex;

    public ResolutionViewModel() {
        resolutionIndex = new MutableLiveData<>();
    }

    public MutableLiveData<Integer> getResolutionIndex() {
        return resolutionIndex;
    }

    public void setResolutionIndex(MutableLiveData<Integer> resolutionIndex) {
        this.resolutionIndex = resolutionIndex;
    }
}
