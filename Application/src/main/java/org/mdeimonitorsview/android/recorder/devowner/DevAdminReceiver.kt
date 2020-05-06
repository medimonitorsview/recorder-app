package org.mdeimonitorsview.android.recorder.devowner

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.mdeimonitorsview.android.recorder.AppConstants

/**
 * Copyright (c) 2018 by Roman Sisik. All rights reserved.
 */
class DevAdminReceiver: DeviceAdminReceiver() {
    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
        Log.d(AppConstants.INSTALLER_TAG, "Device Owner Enabled")
    }
}