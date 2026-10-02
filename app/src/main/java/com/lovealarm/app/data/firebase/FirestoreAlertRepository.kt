package com.lovealarm.app.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lovealarm.app.data.Alert
import com.lovealarm.app.data.AlertRepository
import com.lovealarm.app.data.AlertType
import com.lovealarm.app.data.Collections
import com.lovealarm.app.data.Fields
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.MAX_TILE_TEXT_LENGTH
import com.lovealarm.app.data.PushStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreAlertRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AlertRepository {

    private fun alerts(pairId: String) =
        db.collection(Collections.PAIRS).document(pairId).collection(Collections.ALERTS)

    override suspend fun sendAlert(
        pairId: String,
        fromUid: String,
        toUid: String,
        text: String,
        type: AlertType,
        replyTo: String?,
        waitForServer: Boolean,
    ): String {
        val clean = text.trim().take(MAX_TILE_TEXT_LENGTH)
        if (clean.isEmpty()) throw LoveAlarmException("Nothing to send.")
        val data = hashMapOf<String, Any>(
            Fields.FROM_UID to fromUid,
            Fields.TO_UID to toUid,
            Fields.TEXT to clean,
            Fields.TYPE to type.value,
            Fields.REPLY_TO to (replyTo ?: ""),
            Fields.CREATED_AT to FieldValue.serverTimestamp(),
            Fields.PUSH_STATUS to PushStatus.PENDING.value,
        )
        val ref = alerts(pairId).document()
        try {
            val task = ref.set(data)
            if (waitForServer) task.await()
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't send: ${e.message ?: "unknown error"}", e)
        }
        return ref.id
    }

    override fun observeAlerts(pairId: String): Flow<List<Alert>> = callbackFlow {
        val registration = alerts(pairId)
            .orderBy(Fields.CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(LoveAlarmException("Couldn't load history.", error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toAlert() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
