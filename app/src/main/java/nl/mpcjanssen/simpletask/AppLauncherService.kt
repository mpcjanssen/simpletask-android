package nl.mpcjanssen.simpletask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AppLauncherService : Service() {

    private val appLauncherReceiver = AppLauncherReceiver()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Create a notification channel for the foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    "simpletask_launcher_service",
                    "SimpleTask Launcher Service",
                    NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification for the foreground service using NotificationCompat.Builder()
        val notification = NotificationCompat.Builder(this, "simpletask_launcher_service")
                .setContentTitle("SimpleTask Habit Mode")
                .setContentText("Waiting for Screen Unlock")
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .build()

        // Start the foreground service with the notification
        startForeground(1, notification)

        // Register the BroadcastReceiver for the ACTION_USER_PRESENT broadcast
        val intentFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(appLauncherReceiver, intentFilter)
    }

    override fun onDestroy() {
        // Unregister the BroadcastReceiver when the service is destroyed
        unregisterReceiver(appLauncherReceiver)

        super.onDestroy()
    }
}
