package com.lovealarm.app.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lovealarm.app.MainActivity
import com.lovealarm.app.R
import com.lovealarm.app.ui.theme.BlushDeep

/** The one notification channel and the code that turns an incoming alert into a heads-up notification. */
object Notifications {
    const val CHANNEL_ID = "love_alarm"
    private const val TAG = "LoveAlarmNotify"

    /** Creates (or refreshes the name/description of) the alert channel. Safe to call on every app start. */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            // Sound is left as the channel default on purpose.
            enableVibration(true)
            enableLights(true)
            lightColor = BlushDeep.toArgb()
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the app may post notifications. Always true below Android 13 (no runtime permission there). */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** False when the user has switched notifications off for the app in system settings. */
    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Shows one alert. Tapping it opens the app (History once milestone 2 lands). Silently does nothing without permission. */
    @SuppressLint("MissingPermission")
    fun showAlert(context: Context, alertId: String, fromName: String, text: String) {
        if (!hasPermission(context)) {
            Log.w(TAG, "No notification permission; alert $alertId not shown")
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_HISTORY, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            alertId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(fromName.ifBlank { "Love Alarm" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(BlushDeep.toArgb())
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent)

        // Milestone 2: inline reply action (RemoteInput + addAction) goes here.

        try {
            NotificationManagerCompat.from(context).notify(alertId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification blocked for alert $alertId", e)
        }
    }
}
