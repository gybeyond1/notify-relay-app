package com.notifyrelay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    
    companion object {
        const val PREFS_NAME = "notify_relay_prefs"
        const val KEY_MODE = "mode"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_TOKEN = "token"
        const val KEY_SENT_COUNT = "sent_count"
        const val KEY_RECEIVED_COUNT = "received_count"
        
        const val MODE_SENDER = "sender"
        const val MODE_RECEIVER = "receiver"
        const val MODE_DUAL = "dual"
        
        const val CHANNEL_ID = "notify_relay_channel"
    }
    
    private lateinit var etServerUrl: TextInputEditText
    private lateinit var etToken: TextInputEditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvCurrentMode: TextView
    private lateinit var tvSentCount: TextView
    private lateinit var tvReceivedCount: TextView
    private lateinit var btnOpenSettings: Button
    
    private var currentMode: String = MODE_SENDER
    private var isConnected: Boolean = false
    
    private val countReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.notifyrelay.SENT_COUNT" -> updateSentCount(intent.getIntExtra("count", 0))
                "com.notifyrelay.RECEIVED_COUNT" -> updateReceivedCount(intent.getIntExtra("count", 0))
                "com.notifyrelay.STATUS_CHANGED" -> {
                    val connected = intent.getBooleanExtra("connected", false)
                    updateConnectionStatus(connected)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()
        loadSavedConfig()
        
        if (currentMode.isEmpty()) {
            showModeSelection()
        } else {
            showConfigScreen()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_desc)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun showModeSelection() {
        val modes = arrayOf(
            getString(R.string.mode_sender),
            getString(R.string.mode_receiver),
            getString(R.string.mode_both)
        )
        val modeValues = arrayOf(MODE_SENDER, MODE_RECEIVER, MODE_DUAL)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.mode_select_title)
            .setItems(modes) { _, which ->
                currentMode = modeValues[which]
                saveModeOnly()
                showConfigScreen()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showConfigScreen() {
        setContentView(R.layout.activity_config)
        
        etServerUrl = findViewById(R.id.etServerUrl)
        etToken = findViewById(R.id.etToken)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        tvCurrentMode = findViewById(R.id.tvCurrentMode)
        tvSentCount = findViewById(R.id.tvSentCount)
        tvReceivedCount = findViewById(R.id.tvReceivedCount)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etServerUrl.setText(prefs.getString(KEY_SERVER_URL, ""))
        etToken.setText(prefs.getString(KEY_TOKEN, ""))
        
        val modeText = when (currentMode) {
            MODE_SENDER -> getString(R.string.sender_mode)
            MODE_RECEIVER -> getString(R.string.receiver_mode)
            MODE_DUAL -> getString(R.string.dual_mode)
            else -> getString(R.string.sender_mode)
        }
        tvCurrentMode.text = modeText
        
        updateSentCount(prefs.getInt(KEY_SENT_COUNT, 0))
        updateReceivedCount(prefs.getInt(KEY_RECEIVED_COUNT, 0))
        
        btnConnect.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                connect()
            }
        }
        
        btnOpenSettings.setOnClickListener {
            openSystemSettings()
        }
        
        checkPermissions()
    }
    
    private fun connect() {
        val url = etServerUrl.text.toString().trim()
        val token = etToken.text.toString().trim()
        
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)) {
            Toast.makeText(this, "请填写服务器地址和Token", Toast.LENGTH_SHORT).show()
            return
        }
        
        saveConfig()
        
        when (currentMode) {
            MODE_SENDER, MODE_DUAL -> startSender()
            MODE_RECEIVER -> startReceiver()
        }
    }
    
    private fun disconnect() {
        when (currentMode) {
            MODE_SENDER, MODE_DUAL -> stopSender()
            MODE_RECEIVER -> stopReceiver()
        }
        updateConnectionStatus(false)
    }
    
    private fun startSender() {
        // 检查通知监听权限
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, R.string.perm_notification_listener_desc, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return
        }
        
        // 启动通知监听服务
        val intent = Intent(this, NotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        updateConnectionStatus(true)
        Toast.makeText(this, R.string.notify_listening, Toast.LENGTH_SHORT).show()
    }
    
    private fun stopSender() {
        val intent = Intent(this, NotificationService::class.java)
        stopService(intent)
    }
    
    private fun startReceiver() {
        val url = etServerUrl.text.toString().trim()
        val token = etToken.text.toString().trim()
        
        val intent = Intent(this, WebSocketService::class.java).apply {
            putExtra("server_url", url)
            putExtra("token", token)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopReceiver() {
        val intent = Intent(this, WebSocketService::class.java)
        stopService(intent)
    }
    
    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
    
    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        isConnected = connected
        runOnUiThread {
            tvStatus.text = if (connected) {
                getString(R.string.status_connected)
            } else {
                getString(R.string.status_disconnected)
            }
            btnConnect.text = if (connected) {
                getString(R.string.btn_disconnect)
            } else {
                getString(R.string.btn_connect)
            }
        }
    }
    
    private fun updateSentCount(count: Int) {
        runOnUiThread {
            tvSentCount.text = "发送: $count"
        }
    }
    
    private fun updateReceivedCount(count: Int) {
        runOnUiThread {
            tvReceivedCount.text = "接收: $count"
        }
    }
    
    private fun saveConfig() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SERVER_URL, etServerUrl.text.toString().trim())
            .putString(KEY_TOKEN, etToken.text.toString().trim())
            .apply()
    }
    
    private fun saveModeOnly() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_MODE, currentMode)
            .apply()
    }
    
    private fun loadSavedConfig() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentMode = prefs.getString(KEY_MODE, "") ?: ""
    }
    
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.notifyrelay.SENT_COUNT")
            addAction("com.notifyrelay.RECEIVED_COUNT")
            addAction("com.notifyrelay.STATUS_CHANGED")
        }
        registerReceiver(countReceiver, filter)
    }
    
    override fun onPause() {
        super.onPause()
        unregisterReceiver(countReceiver)
    }
}
