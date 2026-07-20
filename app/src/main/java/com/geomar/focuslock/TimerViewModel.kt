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
import org.json.JSONArray
import org.json.JSONObject

data class Template(val name: String, val totalMinutes: Int)

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

    private val _templates = MutableStateFlow(loadTemplates())
    val templates: StateFlow<List<Template>> = _templates

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

    fun addTemplate(name: String, totalMinutes: Int) {
        val trimmed = name.trim().take(24)
        if (trimmed.isEmpty() || totalMinutes <= 0) return
        // Same name replaces the old entry.
        val updated = _templates.value.filterNot { it.name.equals(trimmed, ignoreCase = true) } +
            Template(trimmed, totalMinutes)
        _templates.value = updated
        saveTemplates(updated)
    }

    fun deleteTemplate(template: Template) {
        val updated = _templates.value - template
        _templates.value = updated
        saveTemplates(updated)
    }

    private fun loadTemplates(): List<Template> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_TEMPLATES, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Template(o.getString("name"), o.getInt("min"))
        }
    }.getOrDefault(emptyList())

    private fun saveTemplates(list: List<Template>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("min", it.totalMinutes)) }
        prefs.edit().putString(KEY_TEMPLATES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_END_TIME = "end_time"
        private const val KEY_TOTAL = "total"
        private const val KEY_TEMPLATES = "templates"
    }
}
