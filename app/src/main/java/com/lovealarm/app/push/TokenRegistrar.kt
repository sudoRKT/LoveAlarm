package com.lovealarm.app.push

// OWNED BY: FCM subagent. Foundation stub. Called by RootViewModel once the user is signed in.

object TokenRegistrar {
    /** Fetches the current FCM token and stores it on users/{uid}. Never throws. */
    suspend fun refresh(uid: String) {
        // Implemented by the FCM subagent.
    }
}
