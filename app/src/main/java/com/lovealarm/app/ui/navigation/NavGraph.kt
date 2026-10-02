package com.lovealarm.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lovealarm.app.ui.RootState
import com.lovealarm.app.ui.RootViewModel
import com.lovealarm.app.ui.home.HomeScreen
import com.lovealarm.app.ui.onboarding.OnboardingScreen
import com.lovealarm.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history" // milestone 2
}

/**
 * Top-level composable. Picks Onboarding or the main NavHost from [RootState].
 * [openHistoryOnStart] is set when the app was opened from a notification (History arrives in milestone 2;
 * until then it lands on Home).
 */
@Composable
fun LoveAlarmRoot(openHistoryOnStart: Boolean = false) {
    val rootViewModel: RootViewModel = viewModel()
    val state by rootViewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is RootState.Loading -> CenteredLoading()
        is RootState.Error -> CenteredError(s.message, onRetry = rootViewModel::retry)
        is RootState.Onboarding -> OnboardingScreen(uid = s.uid, profile = s.profile)
        is RootState.Ready -> MainNavHost(uid = s.uid, pairId = s.profile.pairId, openHistoryOnStart = openHistoryOnStart)
    }
}

@Composable
private fun MainNavHost(uid: String, pairId: String, openHistoryOnStart: Boolean) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                uid = uid,
                pairId = pairId,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                uid = uid,
                pairId = pairId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Try again") }
    }
}
