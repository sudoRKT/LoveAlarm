package com.lovealarm.app.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lovealarm.app.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives the data-only, high-priority pushes sent by the sender container on the home server.
 * Because there is no notification block, onMessageReceived runs even when the app is closed.
 *
 * Data keys (all strings): alertId, pairId, fromUid, fromName, text, type ("tile" | "reply"), replyTo, createdAt (epoch millis).
 */
class LoveAlarmMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val data = message.data
            val text = data[KEY_TEXT].orEmpty()
            if (text.isBlank()) {
                Log.i(TAG, "Push without text ignored (keys=${data.keys})")
                return
            }
            val alertId = data[KEY_ALERT_ID].orEmpty().ifBlank { UUID.randomUUID().toString() }
            val fromName = data[KEY_FROM_NAME].orEmpty()
            Log.i(TAG, "Alert $alertId type=${data[KEY_TYPE]} pair=${data[KEY_PAIR_ID]} from=${data[KEY_FROM_UID]}")
            Notifications.showAlert(applicationContext, alertId, fromName, text)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to handle push", e)
        }
    }

    override fun onNewToken(token: String) {
        val uid = try {
            AppGraph.auth.currentUid
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read uid for new token", e)
            null
        }
        TokenRegistrar.lastToken = token
        if (uid == null) {
            Log.i(TAG, "New push token before sign-in; it will be stored on next app start")
            return
        }
        scope.launch {
            try {
                AppGraph.users.updateFcmToken(uid, token)
                Log.i(TAG, "New push token stored")
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't store new push token", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LoveAlarmFcm"

        const val KEY_ALERT_ID = "alertId"
        const val KEY_PAIR_ID = "pairId"
        const val KEY_FROM_UID = "fromUid"
        const val KEY_FROM_NAME = "fromName"
        const val KEY_TEXT = "text"
        const val KEY_TYPE = "type"
        const val KEY_REPLY_TO = "replyTo"
        const val KEY_CREATED_AT = "createdAt"
    }
}
