package org.mdeimonitorsview.android.recorder.devowner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.mdeimonitorsview.android.recorder.AppConstants
import org.mdeimonitorsview.android.recorder.CameraActivity
import org.mdeimonitorsview.android.recorder.MainActivity

/**
 * Copyright (c) 2018 by Roman Sisik. All rights reserved.
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Restart app here
        val i = Intent(context, MainActivity::class.java)
        Log.d(AppConstants.INSTALLER_TAG,"update receiver")
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
