package com.lovealarm.app.data.firebase

// OWNED BY: pairing subagent. Foundation stub so the project compiles.

import com.google.firebase.firestore.FirebaseFirestore
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.PairInfo
import com.lovealarm.app.data.PairRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FirestorePairRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PairRepository {
    override suspend fun createPair(uid: String): String = throw LoveAlarmException("Not implemented yet.")
    override suspend fun joinPair(uid: String, code: String): String = throw LoveAlarmException("Not implemented yet.")
    override fun observePair(pairId: String): Flow<PairInfo?> = flowOf(null)
    override suspend fun getPair(pairId: String): PairInfo? = null
    override suspend fun getPairCode(pairId: String): String? = null
}
