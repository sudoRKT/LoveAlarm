package com.lovealarm.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lovealarm.app.ui.navigation.LoveAlarmRoot
import com.lovealarm.app.ui.theme.LoveAlarmTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Put this boolean extra on the launch intent to land on History (milestone 2). */
        const val EXTRA_OPEN_HISTORY = "open_history"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openHistory = intent?.getBooleanExtra(EXTRA_OPEN_HISTORY, false) == true
        setContent {
            LoveAlarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoveAlarmRoot(openHistoryOnStart = openHistory)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
