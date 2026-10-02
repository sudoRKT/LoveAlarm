package com.lovealarm.app.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interfaces. Implementations live in data/firebase.
 * UI code and view models talk to these, never to Firestore directly.
 */

interface AuthRepository {
    /** The signed-in uid, or null before [ensureSignedIn] has completed. */
    val currentUid: String?

    /** Signs in anonymously if needed and returns the uid. Safe to call repeatedly. */
    suspend fun ensureSignedIn(): String
}

interface UserRepository {
    fun observeUser(uid: String): Flow<UserProfile?>
    suspend fun getUser(uid: String): UserProfile?

    /** Creates the user document if missing. Only the given non-null fields are written. */
    suspend fun upsertUser(
        uid: String,
        name: String? = null,
        timeZone: String? = null,
        fcmToken: String? = null,
        pairId: String? = null,
    )

    suspend fun updateFcmToken(uid: String, token: String)
}

interface PairRepository {
    /** Creates a new pair with [uid] as its first member and returns the 6-character code to share. */
    suspend fun createPair(uid: String): String

    /** Joins the pair behind [code]. Returns the pairId. Throws [LoveAlarmException] if the code is wrong or the pair is full. */
    suspend fun joinPair(uid: String, code: String): String

    fun observePair(pairId: String): Flow<PairInfo?>
    suspend fun getPair(pairId: String): PairInfo?

    /** The code that was used to create this pair, for showing in Settings. */
    suspend fun getPairCode(pairId: String): String?
}

interface TileRepository {
    /** The custom tiles owned by [ownerUid] in this pair, ordered by `order`. Presets are not stored here. */
    fun observeCustomTiles(pairId: String, ownerUid: String): Flow<List<Tile>>
    suspend fun addTile(pairId: String, ownerUid: String, text: String): String
    suspend fun deleteTile(pairId: String, tileId: String)
}

interface AlertRepository {
    /**
     * Writes a pending alert. The sender container on the home server turns it into a push. Returns the alert id.
     * With [waitForServer] = false (default) the write is queued and the call returns at once, so a tap feels instant
     * even offline. With true it waits for the server to accept it, so failures surface (used by the test button).
     */
    suspend fun sendAlert(
        pairId: String,
        fromUid: String,
        toUid: String,
        text: String,
        type: AlertType = AlertType.TILE,
        replyTo: String? = null,
        waitForServer: Boolean = false,
    ): String

    /** All alerts in the pair, oldest first. Used by History (milestone 2). */
    fun observeAlerts(pairId: String): Flow<List<Alert>>
}
