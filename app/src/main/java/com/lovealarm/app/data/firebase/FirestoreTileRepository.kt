package com.lovealarm.app.data.firebase

// OWNED BY: home/tiles subagent. Foundation stub so the project compiles.

import com.google.firebase.firestore.FirebaseFirestore
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.Tile
import com.lovealarm.app.data.TileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FirestoreTileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : TileRepository {
    override fun observeCustomTiles(pairId: String, ownerUid: String): Flow<List<Tile>> = flowOf(emptyList())
    override suspend fun addTile(pairId: String, ownerUid: String, text: String): String = throw LoveAlarmException("Not implemented yet.")
    override suspend fun deleteTile(pairId: String, tileId: String) = throw LoveAlarmException("Not implemented yet.")
}
