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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/** Consecutive work days ending today (or yesterday, if today isn't done yet). */
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
fun HistoryScreen(workDays: Set<LocalDate>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var month by remember { mutableStateOf(YearMonth.now()) }
    val streak = remember(workDays) { currentStreak(workDays) }
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WrkCaption("streak")
        Text("$streak", fontSize = 96.sp, fontWeight = FontWeight.ExtraLight)
        WrkCaption(if (streak == 1) "day" else "days")

        Spacer(Modifier.height(40.dp))

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
                        done = day in workDays,
                        isToday = day == today,
                        today = today,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(20.dp))
        WrkCaption("${workDays.size} days total", color = WrkFaint)

        Spacer(Modifier.weight(1f))
        WrkGhostButton("Back", onClick = onBack)
    }
}

@Composable
private fun DayCell(
    day: LocalDate?,
    done: Boolean,
    isToday: Boolean,
    today: LocalDate,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .then(
                when {
                    day == null -> Modifier
                    done -> Modifier.background(WrkWhite)
                    isToday -> Modifier.border(1.dp, WrkDim)
                    else -> Modifier
                }
            ),
    ) {
        if (day != null) {
            Text(
                "${day.dayOfMonth}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = when {
                    done -> WrkBlack
                    day > today -> WrkFaint
                    else -> WrkDim
                },
            )
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
