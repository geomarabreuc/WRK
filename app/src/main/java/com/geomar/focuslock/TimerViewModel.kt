package com.geomar.focuslock

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val remainingMillis: Long, val totalMillis: Long) : TimerState
    data class Finished(val totalMillis: Long) : TimerState
}

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("focuslock", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state

    private var tickJob: Job? = null

    init {
        // Recover a session that survived process death.
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val total = prefs.getLong(KEY_TOTAL, 0L)
        if (endTime > System.currentTimeMillis() && total > 0) {
            startTicking(endTime, total)
        } else if (endTime != 0L) {
            clearSession()
        }
    }

    fun startSession(minutes: Int) {
        val total = minutes * 60_000L
        val endTime = System.currentTimeMillis() + total
        prefs.edit()
            .putLong(KEY_END_TIME, endTime)
            .putLong(KEY_TOTAL, total)
            .apply()
        startTicking(endTime, total)
    }

    fun acknowledgeFinished() {
        _state.value = TimerState.Idle
    }

    fun cancelSession() {
        tickJob?.cancel()
        clearSession()
        _state.value = TimerState.Idle
    }

    private fun startTicking(endTime: Long, total: Long) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    clearSession()
                    _state.value = TimerState.Finished(total)
                    break
                }
                _state.value = TimerState.Running(remaining, total)
                delay(250)
            }
        }
    }

    private fun clearSession() {
        prefs.edit().remove(KEY_END_TIME).remove(KEY_TOTAL).apply()
    }

    companion object {
        private const val KEY_END_TIME = "end_time"
        private const val KEY_TOTAL = "total"
    }
}
