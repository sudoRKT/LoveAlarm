package com.lovealarm.app

import android.app.Application
import com.lovealarm.app.push.Notifications

class LoveAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
    }
}
