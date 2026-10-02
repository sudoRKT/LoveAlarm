package com.lovealarm.app.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lovealarm.app.data.Fields
import com.lovealarm.app.data.FsCollections
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.MAX_TILE_TEXT_LENGTH
import com.lovealarm.app.data.Tile
import com.lovealarm.app.data.TileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Custom tiles live at pairs/{pairId}/tiles/{tileId}. Presets are never stored.
 *
 * The query filters on ownerUid only and sorts in Kotlin. A where + orderBy on different fields
 * would need a composite index that someone would have to create by hand in the console.
 */
class FirestoreTileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : TileRepository {

    private fun tiles(pairId: String) =
        db.collection(FsCollections.PAIRS).document(pairId).collection(FsCollections.TILES)

    override fun observeCustomTiles(pairId: String, ownerUid: String): Flow<List<Tile>> = callbackFlow {
        val registration = tiles(pairId)
            .whereEqualTo(Fields.OWNER_UID, ownerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(LoveAlarmException("Couldn't load your tiles.", error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toTile() } ?: emptyList()
                // A tile written offline has no server createdAt yet; it sorts after the others with the same order.
                val sorted = list.sortedWith(
                    compareBy<Tile>({ it.order }, { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }),
                )
                trySend(sorted)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addTile(pairId: String, ownerUid: String, text: String): String {
        val clean = text.trim()
        if (clean.isEmpty()) throw LoveAlarmException("A tile needs some text.")
        if (clean.length > MAX_TILE_TEXT_LENGTH) {
            throw LoveAlarmException("Tiles can be up to $MAX_TILE_TEXT_LENGTH characters.")
        }
        val data = hashMapOf<String, Any>(
            Fields.OWNER_UID to ownerUid,
            Fields.TEXT to clean,
            Fields.ORDER to System.currentTimeMillis(),
            Fields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        val id: String = try {
            val ref = tiles(pairId).document()
            // Not awaited: Firestore queues the write offline and the snapshot listener shows it at once.
            ref.set(data)
            ref.id
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't add the tile: ${e.message ?: "unknown error"}", e)
        }
        return id
    }

    override suspend fun deleteTile(pairId: String, tileId: String) {
        if (tileId.isBlank()) throw LoveAlarmException("Couldn't delete the tile.")
        try {
            tiles(pairId).document(tileId).delete()
        } catch (e: Exception) {
            throw LoveAlarmException("Couldn't delete the tile: ${e.message ?: "unknown error"}", e)
        }
    }
}
