package org.mdeimonitorsview.android.recorder;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class UiHandler extends Handler {

    private final Context context;

    UiHandler(Looper looper, Context context) {
        super(looper);
        this.context = context;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
    }

    public void showToast(int resourceIndex) {
        showToast(context.getString(resourceIndex));
    }

    public void showToast(String message) {
        Message messageToSend = new Message();
        messageToSend.obj = message;
        sendMessage(messageToSend);
    }

}
