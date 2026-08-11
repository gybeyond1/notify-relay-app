package com.notifyrelay

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class WebSocketService : Service() {

    companion object {
        const val TAG = "WebSocketService"
        const val NOTIFICATION_ID = 1001
        var receivedCount = 0
            private set
    }

    private var webSocket: WebSocketClient? = null
    private var serverUrl: String = ""
    private var token: String = ""
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverUrl = intent?.getStringExtra("server_url") ?: ""
        token = intent?.getStringExtra("token") ?: ""

        startForeground(NOTIFICATION_ID, createForegroundNotification())
        connectWebSocket()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close()
    }

    private fun createForegroundNotification(): Notification {
        val channelId = MainActivity.CHANNEL_ID
        val collapsedViews = RemoteViews(packageName, R.layout.notification_small)
        collapsedViews.setTextViewText(R.id.notification_title, "通知中继运行中")
        collapsedViews.setTextViewText(R.id.notification_text, "等待接收通知...")

        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setCustomContentView(collapsedViews)
        }

        return builder.build()
    }

    private fun connectWebSocket() {
        if (serverUrl.isEmpty() || token.isEmpty()) {
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            serverUrl = prefs.getString(MainActivity.KEY_SERVER_URL, "") ?: ""
            token = prefs.getString(MainActivity.KEY_TOKEN, "") ?: ""
        }

        if (serverUrl.isEmpty() || token.isEmpty()) {
            Log.e(TAG, "服务器地址或Token为空")
            return
        }

        val wsUrl = serverUrl.replace("http", "ws") + "/ws?token=$token"

        webSocket = object : WebSocketClient(URI.create(wsUrl)) {
            override fun onOpen(handshake: ServerHandshake?) {
                Log.d(TAG, "WebSocket 已连接")
                sendStatusBroadcast(true)
            }

            override fun onMessage(message: String?) {
                message ?: return
                try {
                    val json = JSONObject(message)
                    when (json.optString("type")) {
                        "notification" -> {
                            val title = json.optString("title", "")
                            val body = json.optString("body", "")
                            val app = json.optString("app", json.optString("package", ""))
                            showNotification(title, body, app)
                        }
                        "history" -> {
                            val notifications = json.getJSONArray("notifications")
                            for (i in notifications.length() - 1 downTo 0) {
                                val n = notifications.getJSONObject(i)
                                showNotification(
                                    n.optString("title", ""),
                                    n.optString("body", ""),
                                    n.optString("app", n.optString("package", ""))
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析消息失败: ${e.message}")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "WebSocket 已关闭: $reason")
                sendStatusBroadcast(false)
                Thread {
                    Thread.sleep(5000)
                    if (webSocket?.isClosed == true) {
                        connectWebSocket()
                    }
                }.start()
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "WebSocket 错误: ${ex?.message}")
                sendStatusBroadcast(false)
            }
        }

        webSocket?.connect()
    }

    private fun showNotification(title: String, body: String, app: String) {
        receivedCount++
        sendCountBroadcast()

        val channelId = MainActivity.CHANNEL_ID
        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(app)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)

        val notification = builder.build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendStatusBroadcast(connected: Boolean) {
        val intent = Intent("com.notifyrelay.STATUS_CHANGED").apply {
            putExtra("connected", connected)
        }
        sendBroadcast(intent)
    }

    private fun sendCountBroadcast() {
        val intent = Intent("com.notifyrelay.RECEIVED_COUNT").apply {
            putExtra("count", receivedCount)
        }
        sendBroadcast(intent)
    }
}
