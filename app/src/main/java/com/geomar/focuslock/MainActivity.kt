package com.geomar.focuslock

import android.app.ActivityManager
import android.media.AudioAttributes
import android.media.Ringtone
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.geomar.focuslock.ui.FinishedScreen
import com.geomar.focuslock.ui.HistoryScreen
import com.geomar.focuslock.ui.SessionScreen
import com.geomar.focuslock.ui.SetupScreen
import com.geomar.focuslock.ui.WrkTheme
import com.geomar.focuslock.ui.currentStreak
import com.geomar.focuslock.ui.qualifyingDays
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()
    private var alarm: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WrkTheme {
                Surface { WrkApp() }
            }
        }
    }

    @Composable
    private fun WrkApp() {
        val state by viewModel.state.collectAsState()
        val running = state is TimerState.Running

        // Swallow back press while a session runs.
        BackHandler(enabled = running) {}

        // Track whether the task is actually pinned; user can escape pinning
        // (Back+Recents hold) without ending the session, so poll while running.
        var pinned by remember { mutableStateOf(false) }
        LaunchedEffect(running) {
            if (!running) {
                pinned = false
                return@LaunchedEffect
            }
            while (true) {
                pinned = isTaskPinned()
                delay(500)
            }
        }

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
                    stopAlarm()
                }
            }
        }

        val templates by viewModel.templates.collectAsState()
        val workDays by viewModel.workDays.collectAsState()
        val minGoal by viewModel.minGoal.collectAsState()
        var showHistory by remember { mutableStateOf(false) }

        when (val s = state) {
            is TimerState.Idle -> if (showHistory) {
                HistoryScreen(
                    workMinutes = workDays,
                    minGoal = minGoal,
                    onSetGoal = viewModel::setMinGoal,
                    onBack = { showHistory = false },
                )
            } else {
                SetupScreen(
                    templates = templates,
                    streak = currentStreak(qualifyingDays(workDays, minGoal)),
                    onShowHistory = { showHistory = true },
                    onStart = viewModel::startSession,
                    onSaveTemplate = viewModel::addTemplate,
                    onDeleteTemplate = viewModel::deleteTemplate,
                )
            }
            is TimerState.Running -> SessionScreen(
                remainingMillis = s.remainingMillis,
                totalMillis = s.totalMillis,
                pinned = pinned,
                onRePin = ::tryStartLockTask,
                onCancel = viewModel::cancelSession,
            )
            is TimerState.Finished -> FinishedScreen(s.totalMillis, onDone = viewModel::acknowledgeFinished)
        }
    }

    private fun isTaskPinned(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
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
        startAlarm()
    }

    // Loops the system alarm sound until the user taps Done (state returns to Idle).
    private fun startAlarm() {
        stopAlarm()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            alarm = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        }
    }

    private fun stopAlarm() {
        runCatching { alarm?.stop() }
        alarm = null
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
