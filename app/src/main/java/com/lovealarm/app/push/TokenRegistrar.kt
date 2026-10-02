package com.lovealarm.app.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.lovealarm.app.AppGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Keeps users/{uid}.fcmToken in step with this phone's FCM token. Called by RootViewModel after sign-in. */
object TokenRegistrar {
    private const val TAG = "LoveAlarmToken"

    /** The most recent token seen on this phone, for debugging. */
    @Volatile
    var lastToken: String? = null

    /** Fetches the current FCM token and stores it on users/{uid}. Never throws (except to propagate cancellation). */
    suspend fun refresh(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            lastToken = token
            AppGraph.users.updateFcmToken(uid, token)
            Log.i(TAG, "Push token stored for $uid")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't refresh push token", e)
        }
    }
}
