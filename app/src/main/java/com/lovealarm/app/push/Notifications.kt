package com.lovealarm.app.push

// OWNED BY: FCM subagent. Foundation stub: creates the channel so the app runs; notification building is the agent's job.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.lovealarm.app.R

object Notifications {
    const val CHANNEL_ID = "love_alarm"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
