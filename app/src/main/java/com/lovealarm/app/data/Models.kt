package com.lovealarm.app.data

import com.google.firebase.Timestamp

/** Firestore collection and field names. One place, so the rules, the app and the sender agree. */
object Collections {
    const val USERS = "users"
    const val PAIR_CODES = "pairCodes"
    const val PAIRS = "pairs"
    const val TILES = "tiles"
    const val ALERTS = "alerts"
}

object Fields {
    // users/{uid}
    const val NAME = "name"
    const val TIME_ZONE = "timeZone"
    const val FCM_TOKEN = "fcmToken"
    const val PAIR_ID = "pairId"

    // pairs/{pairId}
    const val MEMBERS = "members"
    const val CREATED_AT = "createdAt"

    // tiles/{tileId}
    const val OWNER_UID = "ownerUid"
    const val TEXT = "text"
    const val ORDER = "order"

    // alerts/{alertId}
    const val FROM_UID = "fromUid"
    const val TO_UID = "toUid"
    const val TYPE = "type"
    const val REPLY_TO = "replyTo"
    const val PUSH_STATUS = "pushStatus"
}

const val MAX_TILE_TEXT_LENGTH = 120
const val PAIR_CODE_LENGTH = 6

/** users/{uid} */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val timeZone: String = "",
    val fcmToken: String = "",
    val pairId: String = "",
) {
    val isPaired: Boolean get() = pairId.isNotBlank()
}

/** pairs/{pairId} */
data class PairInfo(
    val id: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
) {
    val isFull: Boolean get() = members.size >= 2
    fun partnerOf(uid: String): String? = members.firstOrNull { it != uid }
}

/** pairs/{pairId}/tiles/{tileId}, or a built-in preset (isPreset = true, id = "preset-N"). */
data class Tile(
    val id: String = "",
    val ownerUid: String = "",
    val text: String = "",
    val order: Long = 0L,
    val createdAt: Timestamp? = null,
    val isPreset: Boolean = false,
)

enum class AlertType(val value: String) {
    TILE("tile"),
    REPLY("reply");

    companion object {
        fun from(value: String?): AlertType = entries.firstOrNull { it.value == value } ?: TILE
    }
}

enum class PushStatus(val value: String) {
    PENDING("pending"),
    SENDING("sending"),
    SENT("sent"),
    SKIPPED("skipped");

    companion object {
        fun from(value: String?): PushStatus = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

/** pairs/{pairId}/alerts/{alertId} */
data class Alert(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val text: String = "",
    val type: AlertType = AlertType.TILE,
    val replyTo: String? = null,
    val createdAt: Timestamp? = null,
    val pushStatus: PushStatus = PushStatus.PENDING,
)

/** Built-in preset tiles. Shown first on every board; cannot be edited or deleted. */
object Presets {
    val texts: List<String> = listOf(
        "Drink water 💧",
        "I miss you",
        "How are you feeling?",
        "Good morning ☀️",
        "Good night 🌙",
        "Thinking of you",
        "Have you eaten?",
        "Call me when you're free",
    )

    const val ID_PREFIX = "preset-"

    val tiles: List<Tile> = texts.mapIndexed { index, text ->
        Tile(id = "$ID_PREFIX$index", text = text, order = index.toLong(), isPreset = true)
    }
}

/** Thrown by repositories for problems the UI should show to the user in plain words. */
class LoveAlarmException(message: String, cause: Throwable? = null) : Exception(message, cause)
