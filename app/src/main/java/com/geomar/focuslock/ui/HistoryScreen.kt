package com.geomar.focuslock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/** Days whose focused minutes meet the daily goal (goal 0 = any recorded day). */
fun qualifyingDays(workMinutes: Map<LocalDate, Int>, minGoal: Int): Set<LocalDate> =
    workMinutes.filterValues { it >= minGoal }.keys

/** Consecutive qualifying days ending today (or yesterday, if today isn't done yet). */
fun currentStreak(days: Set<LocalDate>): Int {
    var d = LocalDate.now()
    if (d !in days) d = d.minusDays(1)
    var n = 0
    while (d in days) {
        n++
        d = d.minusDays(1)
    }
    return n
}

@Composable
fun HistoryScreen(
    workMinutes: Map<LocalDate, Int>,
    minGoal: Int,
    onSetGoal: (Int) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(today) }
    val doneDays = remember(workMinutes, minGoal) { qualifyingDays(workMinutes, minGoal) }
    val streak = remember(doneDays) { currentStreak(doneDays) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WrkCaption("streak")
        Text("$streak", fontSize = 96.sp, fontWeight = FontWeight.ExtraLight)
        WrkCaption(if (streak == 1) "day" else "days")

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            NavArrow("‹") { month = month.minusMonths(1) }
            WrkCaption("${month.month.name.lowercase()} ${month.year}", color = WrkWhite)
            NavArrow("›", enabled = month < YearMonth.now()) { month = month.plusMonths(1) }
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    fontSize = 11.sp,
                    color = WrkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Monday-first grid; leading nulls pad the first week.
        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
        val cells = List<LocalDate?>(leadingBlanks) { null } +
            (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        minutes = day?.let { workMinutes[it] } ?: 0,
                        done = day in doneDays,
                        selected = day == selected,
                        today = today,
                        onSelect = { selected = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(20.dp))
        SelectedDayCaption(selected, workMinutes[selected] ?: 0)
        Spacer(Modifier.height(8.dp))
        WrkCaption("${doneDays.size} days total", color = WrkFaint)

        Spacer(Modifier.weight(1f))

        GoalSetting(minGoal, onSetGoal)
        Spacer(Modifier.height(20.dp))
        WrkGhostButton("Back", onClick = onBack)
    }
}

@Composable
private fun SelectedDayCaption(day: LocalDate, minutes: Int) {
    val date = "${day.month.name.take(3).lowercase()} ${day.dayOfMonth}"
    WrkCaption(
        if (minutes > 0) "$date · $minutes min focused" else "$date · no focus",
        color = WrkWhite,
    )
}

@Composable
private fun DayCell(
    day: LocalDate?,
    minutes: Int,
    done: Boolean,
    selected: Boolean,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val base = modifier
        .aspectRatio(1f)
        .padding(3.dp)
    val decorated = when {
        day == null -> base
        done -> base.background(WrkWhite)
        selected -> base.border(1.dp, WrkWhite)
        day == today -> base.border(1.dp, WrkDim)
        else -> base
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = decorated.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = day != null && !day.isAfter(today),
        ) { day?.let(onSelect) },
    ) {
        if (day != null) {
            Text(
                "${day.dayOfMonth}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = when {
                    done -> WrkBlack
                    minutes > 0 -> WrkWhite // focused, but below the daily goal
                    day > today -> WrkFaint
                    else -> WrkDim
                },
            )
        }
    }
}

/** "goal · N min/day" caption; tap to type a new minimum. 0 turns the goal off. */
@Composable
private fun GoalSetting(goal: Int, onSet: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun commit() {
        text.toIntOrNull()?.let { onSet(it.coerceIn(0, 24 * 60)) }
        editing = false
        hadFocus = false
    }

    if (editing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WrkCaption("goal · ")
            BasicTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                singleLine = true,
                textStyle = TextStyle(
                    color = WrkWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                ),
                cursorBrush = SolidColor(WrkWhite),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .width(56.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        // Ignore the initial unfocused callback fired on attach.
                        if (it.isFocused) hadFocus = true
                        else if (hadFocus && editing) commit()
                    },
            )
            WrkCaption(" min/day")
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    } else {
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                text = if (goal > 0) "$goal" else ""
                editing = true
            }
        ) {
            WrkCaption(if (goal > 0) "goal · $goal min/day" else "set daily goal")
        }
    }
}

@Composable
private fun NavArrow(symbol: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Light, color = if (enabled) WrkDim else WrkFaint)
    }
}
