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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    /** Focused minutes per day; multiple sessions on the same day sum. */
    private val _workDays = MutableStateFlow(loadWorkDays())
    val workDays: StateFlow<Map<LocalDate, Int>> = _workDays

    /** Minimum daily minutes for a day to count toward streak/calendar. 0 = any session counts. */
    private val _minGoal = MutableStateFlow(prefs.getInt(KEY_MIN_GOAL, 0))
    val minGoal: StateFlow<Int> = _minGoal

    init {
        // Recover a session that survived process death.
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val total = prefs.getLong(KEY_TOTAL, 0L)
        if (endTime > System.currentTimeMillis() && total > 0) {
            startTicking(endTime, total)
        } else if (endTime != 0L) {
            // Timer ran out while the process was dead — still a completed session.
            if (total > 0) {
                recordWorkDay(
                    minutes = (total / 60_000L).toInt(),
                    day = Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()).toLocalDate(),
                )
            }
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
                    recordWorkDay(minutes = (total / 60_000L).toInt())
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

    fun setMinGoal(minutes: Int) {
        val m = minutes.coerceIn(0, 24 * 60)
        _minGoal.value = m
        prefs.edit().putInt(KEY_MIN_GOAL, m).apply()
    }

    private fun recordWorkDay(minutes: Int, day: LocalDate = LocalDate.now()) {
        if (minutes <= 0) return
        val updated = _workDays.value + (day to (_workDays.value[day] ?: 0) + minutes)
        _workDays.value = updated
        val obj = JSONObject()
        updated.forEach { (d, m) -> obj.put(d.toString(), m) }
        prefs.edit().putString(KEY_WORK_DAYS, obj.toString()).apply()
    }

    private fun loadWorkDays(): Map<LocalDate, Int> {
        val raw = prefs.getString(KEY_WORK_DAYS, "{}") ?: "{}"
        runCatching {
            val obj = JSONObject(raw)
            return obj.keys().asSequence().associate { LocalDate.parse(it) to obj.getInt(it) }
        }
        // Legacy format: JSON array of dates, minutes unknown — kept as 0.
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).associate { LocalDate.parse(arr.getString(it)) to 0 }
        }.getOrDefault(emptyMap())
    }

    companion object {
        private const val KEY_END_TIME = "end_time"
        private const val KEY_TOTAL = "total"
        private const val KEY_TEMPLATES = "templates"
        private const val KEY_WORK_DAYS = "work_days"
        private const val KEY_MIN_GOAL = "min_goal"
    }
}
