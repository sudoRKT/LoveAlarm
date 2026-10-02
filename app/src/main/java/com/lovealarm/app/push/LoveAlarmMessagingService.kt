package com.lovealarm.app.push

// OWNED BY: FCM subagent. Foundation stub so the manifest entry resolves.

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LoveAlarmMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
