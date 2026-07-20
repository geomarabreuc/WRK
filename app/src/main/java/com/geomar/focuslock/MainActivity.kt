package com.geomar.focuslock

import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.geomar.focuslock.ui.FinishedScreen
import com.geomar.focuslock.ui.SessionScreen
import com.geomar.focuslock.ui.SetupScreen

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface { FocusLockApp() }
            }
        }
    }

    @Composable
    private fun FocusLockApp() {
        val state by viewModel.state.collectAsState()

        // Swallow back press while a session runs.
        BackHandler(enabled = state is TimerState.Running) {}

        // React to state transitions: lock/unlock, keep-screen-on, completion alarm.
        LaunchedEffect(state::class) {
            when (state) {
                is TimerState.Running -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    tryStartLockTask()
                }
                is TimerState.Finished -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    tryStopLockTask()
                    notifyComplete()
                }
                is TimerState.Idle -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    tryStopLockTask()
                }
            }
        }

        when (val s = state) {
            is TimerState.Idle -> SetupScreen(onStart = viewModel::startSession)
            is TimerState.Running -> SessionScreen(s.remainingMillis, s.totalMillis)
            is TimerState.Finished -> FinishedScreen(s.totalMillis, onDone = viewModel::acknowledgeFinished)
        }
    }

    private fun tryStartLockTask() {
        try {
            startLockTask()
        } catch (_: IllegalStateException) {
            // Activity not yet in foreground or pinning unavailable; session still runs.
        }
    }

    private fun tryStopLockTask() {
        try {
            stopLockTask()
        } catch (_: IllegalStateException) {
        }
    }

    private fun notifyComplete() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
        )
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        }
    }
}
