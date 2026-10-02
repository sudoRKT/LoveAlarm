package com.lovealarm.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.lovealarm.app.data.CreatedPair
import com.lovealarm.app.data.Fields
import com.lovealarm.app.data.FsCollections
import com.lovealarm.app.data.LoveAlarmException
import com.lovealarm.app.data.PAIR_CODE_LENGTH
import com.lovealarm.app.data.PairInfo
import com.lovealarm.app.data.PairRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/** pairs/{pairId}.code and the pairCodes/{code} fields not already in [Fields]. */
private const val FIELD_CODE = "code"
private const val FIELD_PAIR_ID = "pairId"
private const val FIELD_CREATED_BY = "createdBy"

/** No 0/O, no 1/I, so a code read out loud or typed from a screenshot is hard to get wrong. */
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val MAX_CODE_ATTEMPTS = 5

private const val MSG_CODE_NOT_FOUND = "That code doesn't match any pair. Check it and try again."
private const val MSG_PAIR_FULL = "That pair already has two people."

/**
 * Pairs live in pairs/{pairId}; pairCodes/{code} points a shareable code at its pair.
 *
 * Contract note: [createPair] and [joinPair] only add the user to pairs/{pairId}.members.
 * They do NOT write users/{uid}.pairId. The caller (onboarding) writes the pairId to the user
 * document itself when it is ready, because RootViewModel leaves onboarding the moment that
 * field appears. If the app dies in between, [findPairsForUser] finds the pair again.
 *
 * Every failure is thrown as [LoveAlarmException] with a message that can be shown as-is.
 */
class FirestorePairRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PairRepository {

    private val random = SecureRandom()

    private fun pairs() = db.collection(FsCollections.PAIRS)
    private fun pairDoc(pairId: String) = pairs().document(pairId)
    private fun codeDoc(code: String) = db.collection(FsCollections.PAIR_CODES).document(code)

    private enum class JoinOutcome { JOINED, ALREADY_MEMBER, FULL, PAIR_MISSING }

    override suspend fun createPair(uid: String): String = createPairWithId(uid).code

    override suspend fun createPairWithId(uid: String): CreatedPair {
        repeat(MAX_CODE_ATTEMPTS) {
            val code = newCode()
            val pairRef = pairs().document() // Firestore auto id
            val codeRef = codeDoc(code)
            val created = try {
                db.runTransaction { transaction ->
                    // Reads first. If the code is already taken, write nothing and try another code.
                    val existing = transaction.get(codeRef)
                    if (existing.exists()) {
                        false
                    } else {
                        transaction.set(
                            pairRef,
                            mapOf(
                                Fields.MEMBERS to listOf(uid),
                                Fields.CREATED_AT to FieldValue.serverTimestamp(),
                                FIELD_CODE to code,
                            ),
                        )
                        transaction.set(
                            codeRef,
                            mapOf(
                                FIELD_PAIR_ID to pairRef.id,
                                FIELD_CREATED_BY to uid,
                                Fields.CREATED_AT to FieldValue.serverTimestamp(),
                            ),
                        )
                        true
                    }
                }.await()
            } catch (e: Exception) {
                throw friendly(e, "Couldn't create a pair. Check your connection and try again.")
            }
            if (created) return CreatedPair(pairId = pairRef.id, code = code)
        }
        throw LoveAlarmException("Couldn't create a pair code. Please try again.")
    }

    override suspend fun joinPair(uid: String, code: String): String {
        val normalised = code.trim().uppercase()
        if (normalised.length != PAIR_CODE_LENGTH || normalised.any { it !in CODE_ALPHABET }) {
            throw LoveAlarmException(MSG_CODE_NOT_FOUND)
        }

        val codeSnapshot = try {
            codeDoc(normalised).get().await()
        } catch (e: Exception) {
            throw friendly(e, "Couldn't check that code. Check your connection and try again.")
        }
        val pairId = codeSnapshot.getString(FIELD_PAIR_ID)
        if (!codeSnapshot.exists() || pairId.isNullOrBlank()) throw LoveAlarmException(MSG_CODE_NOT_FOUND)

        val pairRef = pairDoc(pairId)
        val outcome = try {
            db.runTransaction { transaction ->
                // Pure function of what it reads: Firestore may run it more than once.
                val snapshot = transaction.get(pairRef)
                val members = snapshot.toPairInfo()?.members
                when {
                    members == null -> JoinOutcome.PAIR_MISSING
                    uid in members -> JoinOutcome.ALREADY_MEMBER
                    members.size >= 2 -> JoinOutcome.FULL
                    else -> {
                        transaction.update(pairRef, mapOf(Fields.MEMBERS to members + uid))
                        JoinOutcome.JOINED
                    }
                }
            }.await()
        } catch (e: Exception) {
            // The rules hide a full pair from people who are not in it, so a refusal here means it's taken.
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw LoveAlarmException(MSG_PAIR_FULL, e)
            }
            throw friendly(e, "Couldn't join that pair. Check your connection and try again.")
        }

        return when (outcome) {
            JoinOutcome.JOINED, JoinOutcome.ALREADY_MEMBER -> pairId
            JoinOutcome.FULL -> throw LoveAlarmException(MSG_PAIR_FULL)
            JoinOutcome.PAIR_MISSING -> throw LoveAlarmException(MSG_CODE_NOT_FOUND)
        }
    }

    override fun observePair(pairId: String): Flow<PairInfo?> = callbackFlow {
        val registration = pairDoc(pairId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(friendly(error, "Couldn't load your pair."))
                return@addSnapshotListener
            }
            trySend(snapshot?.toPairInfo())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getPair(pairId: String): PairInfo? =
        try {
            pairDoc(pairId).get().await().toPairInfo()
        } catch (e: Exception) {
            throw friendly(e, "Couldn't load your pair.")
        }

    override suspend fun getPairCode(pairId: String): String? {
        val pair: DocumentSnapshot = try {
            pairDoc(pairId).get().await()
        } catch (e: Exception) {
            throw friendly(e, "Couldn't load your pair code.")
        }
        pair.getString(FIELD_CODE)?.takeIf { it.isNotBlank() }?.let { return it }

        // Older pairs may lack the code field: look it up the other way round.
        return try {
            db.collection(FsCollections.PAIR_CODES)
                .whereEqualTo(FIELD_PAIR_ID, pairId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id
        } catch (e: Exception) {
            null
        }
    }

    /** Used by onboarding to recover if the app was closed after joining but before finishing. */
    override suspend fun findPairsForUser(uid: String): List<PairInfo> {
        val docs = try {
            pairs().whereArrayContains(Fields.MEMBERS, uid).get().await().documents
        } catch (e: Exception) {
            throw friendly(e, "Couldn't check for an existing pair. Check your connection and try again.")
        }
        return docs
            .mapNotNull { it.toPairInfo() }
            .sortedWith(
                compareByDescending<PairInfo> { it.isFull }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L },
            )
    }

    private fun newCode(): String {
        val chars = CharArray(PAIR_CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        return String(chars)
    }

    private fun friendly(e: Throwable, fallback: String): LoveAlarmException {
        if (e is LoveAlarmException) return e
        val message = if (e is FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "You seem to be offline. Connect to the internet and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "The server said no to that. Make sure the app is up to date and try again."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "You're not signed in yet. Close the app, open it again and retry."
                else -> fallback
            }
        } else {
            fallback
        }
        return LoveAlarmException(message, e)
    }
}
