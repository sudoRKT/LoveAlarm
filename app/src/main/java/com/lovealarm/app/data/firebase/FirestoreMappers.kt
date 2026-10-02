package com.lovealarm.app.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.lovealarm.app.data.Alert
import com.lovealarm.app.data.AlertType
import com.lovealarm.app.data.Fields
import com.lovealarm.app.data.PairInfo
import com.lovealarm.app.data.PushStatus
import com.lovealarm.app.data.Tile
import com.lovealarm.app.data.UserProfile

/**
 * Manual document <-> model mapping. No reflection, so nothing breaks under R8 and
 * a missing field never crashes the app.
 */

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        name = getString(Fields.NAME).orEmpty(),
        timeZone = getString(Fields.TIME_ZONE).orEmpty(),
        fcmToken = getString(Fields.FCM_TOKEN).orEmpty(),
        pairId = getString(Fields.PAIR_ID).orEmpty(),
    )
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toPairInfo(): PairInfo? {
    if (!exists()) return null
    val members = (get(Fields.MEMBERS) as? List<Any?>)?.mapNotNull { it as? String } ?: emptyList()
    return PairInfo(
        id = id,
        members = members,
        createdAt = getTimestamp(Fields.CREATED_AT),
    )
}

fun DocumentSnapshot.toTile(): Tile? {
    if (!exists()) return null
    return Tile(
        id = id,
        ownerUid = getString(Fields.OWNER_UID).orEmpty(),
        text = getString(Fields.TEXT).orEmpty(),
        order = getLong(Fields.ORDER) ?: 0L,
        createdAt = getTimestamp(Fields.CREATED_AT),
        isPreset = false,
    )
}

fun DocumentSnapshot.toAlert(): Alert? {
    if (!exists()) return null
    return Alert(
        id = id,
        fromUid = getString(Fields.FROM_UID).orEmpty(),
        toUid = getString(Fields.TO_UID).orEmpty(),
        text = getString(Fields.TEXT).orEmpty(),
        type = AlertType.from(getString(Fields.TYPE)),
        replyTo = getString(Fields.REPLY_TO)?.takeIf { it.isNotBlank() },
        createdAt = getTimestamp(Fields.CREATED_AT),
        pushStatus = PushStatus.from(getString(Fields.PUSH_STATUS)),
    )
}

fun Timestamp?.millisOrNow(): Long = this?.toDate()?.time ?: System.currentTimeMillis()
