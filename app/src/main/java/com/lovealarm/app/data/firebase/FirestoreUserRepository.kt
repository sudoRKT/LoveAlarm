package com.lovealarm.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lovealarm.app.data.Collections
import com.lovealarm.app.data.Fields
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.UserProfile
import com.lovealarm.app.data.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {

    private fun doc(uid: String) = db.collection(Collections.USERS).document(uid)

    override fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = doc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(LoveAlarmException("Couldn't load your profile.", error))
                return@addSnapshotListener
            }
            trySend(snapshot?.toUserProfile())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getUser(uid: String): UserProfile? =
        try {
            doc(uid).get().await().toUserProfile()
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't load profile.", e)
        }

    override suspend fun upsertUser(uid: String, name: String?, timeZone: String?, fcmToken: String?, pairId: String?) {
        val data = buildMap<String, Any> {
            name?.let { put(Fields.NAME, it.trim()) }
            timeZone?.let { put(Fields.TIME_ZONE, it) }
            fcmToken?.let { put(Fields.FCM_TOKEN, it) }
            pairId?.let { put(Fields.PAIR_ID, it) }
        }
        if (data.isEmpty()) return
        try {
            doc(uid).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't save your profile. Check your connection and try again.", e)
        }
    }

    override suspend fun updateFcmToken(uid: String, token: String) {
        upsertUser(uid, fcmToken = token)
    }
}
