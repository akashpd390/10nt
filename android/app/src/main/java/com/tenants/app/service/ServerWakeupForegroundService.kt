package com.tenants.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.tenants.app.MainActivity
import com.tenants.app.R
import com.tenants.app.data.remote.TenantsApiService
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

/**
 * Foreground Service that handles waking up Render free-tier instance.
 * Automatically checks health every 6 seconds up to 15 times (~90s).
 * If the app is in the background or closed when the server responds 200 OK,
 * fires a notification to remind the user to return to the app.
 */
class ServerWakeupForegroundService : Service() {

    private val apiService: TenantsApiService by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val foregroundNotification = NotificationCompat.Builder(this, CHANNEL_WAKEUP_PROGRESS)
            .setContentTitle(getString(R.string.server_connecting_title))
            .setContentText(getString(R.string.server_connecting_desc))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID_PROGRESS, foregroundNotification)

        startHealthPolling()

        return START_NOT_STICKY
    }

    private fun startHealthPolling() {
        serviceScope.launch {
            var isAwake = false
            var attempts = 0
            val maxAttempts = 15

            while (!isAwake && attempts < maxAttempts) {
                try {
                    val response = apiService.checkHealth()
                    if (response.isSuccessful) {
                        isAwake = true
                        break
                    }
                } catch (e: Exception) {
                    // Cold-start still in progress
                }
                attempts++
                delay(6000) // Poll every 6 seconds
            }

            if (isAwake) {
                notifyIfAppInBackground()
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notifyIfAppInBackground() {
        val isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

        // Only fire intrusive notification if user moved away from app or closed it
        if (!isAppInForeground) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val readyNotification = NotificationCompat.Builder(this, CHANNEL_WAKEUP_ALERT)
                .setContentTitle(getString(R.string.server_ready_title))
                .setContentText(getString(R.string.server_ready_desc))
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_READY, readyNotification)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                CHANNEL_WAKEUP_PROGRESS,
                "Server Wakeup Progress",
                NotificationManager.IMPORTANCE_LOW
            )

            val alertChannel = NotificationChannel(
                CHANNEL_WAKEUP_ALERT,
                "Server Wakeup Ready Alert",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_WAKEUP_PROGRESS = "channel_server_wakeup_progress"
        const val CHANNEL_WAKEUP_ALERT = "channel_server_wakeup_alert"
        const val NOTIFICATION_ID_PROGRESS = 1001
        const val NOTIFICATION_ID_READY = 1002

        fun start(context: Context) {
            val intent = Intent(context, ServerWakeupForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
