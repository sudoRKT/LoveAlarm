package com.lovealarm.app.ui.onboarding

// OWNED BY: pairing subagent. Foundation stub so the project compiles.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lovealarm.app.data.UserProfile

/**
 * First-open flow: name -> pairing (create / join with code) -> notification permission.
 * When the user document gets a name and a pairId, RootViewModel switches to Home automatically.
 * [profile] is the current user document (null before it exists); use it to resume a half-finished onboarding.
 */
@Composable
fun OnboardingScreen(uid: String, profile: UserProfile?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Onboarding (not built yet)")
    }
}
