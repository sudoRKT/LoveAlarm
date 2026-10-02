package com.lovealarm.app

import com.lovealarm.app.data.AlertRepository
import com.lovealarm.app.data.AuthRepository
import com.lovealarm.app.data.PairRepository
import com.lovealarm.app.data.TileRepository
import com.lovealarm.app.data.UserRepository
import com.lovealarm.app.data.firebase.FirebaseAuthRepository
import com.lovealarm.app.data.firebase.FirestoreAlertRepository
import com.lovealarm.app.data.firebase.FirestorePairRepository
import com.lovealarm.app.data.firebase.FirestoreTileRepository
import com.lovealarm.app.data.firebase.FirestoreUserRepository

/**
 * Tiny service locator. The app is small enough that a DI framework would be more code than the app.
 * Everything here is a lazy singleton; view models and the messaging service read from it.
 */
object AppGraph {
    val auth: AuthRepository by lazy { FirebaseAuthRepository() }
    val users: UserRepository by lazy { FirestoreUserRepository() }
    val pairs: PairRepository by lazy { FirestorePairRepository() }
    val tiles: TileRepository by lazy { FirestoreTileRepository() }
    val alerts: AlertRepository by lazy { FirestoreAlertRepository() }
}
