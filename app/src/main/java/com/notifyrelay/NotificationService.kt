package com.notifyrelay

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NotificationService : NotificationListenerService() {

    companion object {
        const val TAG = "NotificationService"
        var sentCount = 0
            private set
    }

    private val client = OkHttpClient()
    private var serverUrl: String = ""
    private var token: String = ""

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "通知监听服务已连接")
        loadConfig()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val appName = sbn.packageName

        if (title.isEmpty() && text.isEmpty() && subText.isEmpty()) return
        if (appName == packageName) return

        val body = listOf(text, subText).filter { it.isNotEmpty() }.joinToString("\n")

        sendNotification(appName, title, body, sbn)
    }

    private fun sendNotification(packageName: String, title: String, body: String, sbn: StatusBarNotification) {
        if (serverUrl.isEmpty() || token.isEmpty()) {
            loadConfig()
        }

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = prefs.getString(MainActivity.KEY_SERVER_URL, "") ?: ""
        token = prefs.getString(MainActivity.KEY_TOKEN, "") ?: ""

        if (serverUrl.isEmpty() || token.isEmpty()) return

        val json = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("app", getAppName(packageName))
            put("package", packageName)
            put("priority", sbn.notification.priority)
        }

        val request = Request.Builder()
            .url("$serverUrl/notify")
            .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "发送失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    sentCount++
                    sendCountBroadcast()
                    Log.d(TAG, "发送成功: $title")
                }
                response.close()
            }
        })
    }

    private fun sendCountBroadcast() {
        val intent = Intent("com.notifyrelay.SENT_COUNT").apply {
            putExtra("count", sentCount)
        }
        sendBroadcast(intent)
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = prefs.getString(MainActivity.KEY_SERVER_URL, "") ?: ""
        token = prefs.getString(MainActivity.KEY_TOKEN, "") ?: ""
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 通知移除时的处理（可选）
    }
}
