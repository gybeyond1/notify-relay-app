package com.notifyrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotifyDismissReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "NotifyDismissReceiver"
        const val ACTION_DISMISS = "com.notifyrelay.ACTION_DISMISS"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "通知被点击/清除")
            }
        }
    }
}
