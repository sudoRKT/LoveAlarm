package com.lovealarm.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.lovealarm.app.data.AuthRepository
import com.lovealarm.app.data.LoveAlarmException
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {

    override val currentUid: String?
        get() = auth.currentUser?.uid

    override suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = try {
            auth.signInAnonymously().await()
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't sign in. Check your internet connection and try again.", e)
        }
        return result.user?.uid ?: throw LoveAlarmException("Couldn't sign in. Please try again.")
    }
}
