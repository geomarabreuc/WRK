package com.geomar.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SessionScreen(
    remainingMillis: Long,
    totalMillis: Long,
    pinned: Boolean,
    onRePin: () -> Unit,
    onCancel: () -> Unit,
) {
    val totalSeconds = (remainingMillis + 999) / 1000
    val timeText = if (totalSeconds >= 3600) {
        String.format(
            Locale.US, "%d:%02d:%02d",
            totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60
        )
    } else {
        String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("WRK", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
            WrkCaption(if (pinned) "locked" else "unlocked", color = if (pinned) WrkDim else WrkWhite)
        }

        Spacer(Modifier.weight(1f))

        Text(
            timeText,
            style = TextStyle(
                fontSize = if (totalSeconds >= 3600) 68.sp else 96.sp,
                fontWeight = FontWeight.ExtraLight,
            )
        )

        Spacer(Modifier.height(40.dp))

        ProgressLine(fraction = 1f - remainingMillis.toFloat() / totalMillis)

        Spacer(Modifier.height(24.dp))

        WrkCaption(if (pinned) "stay with it" else "focus broken")

        Spacer(Modifier.weight(1f))

        if (!pinned) {
            WrkButton("Resume focus", onClick = onRePin)
            Spacer(Modifier.height(12.dp))
            CancelButton(onCancel)
        }
    }
}

/** 2dp hairline progress bar: elapsed portion white, rest faint. */
@Composable
private fun ProgressLine(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(WrkFaint)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(WrkWhite)
        )
    }
}

/** End-session button armed by a first tap; disarms after 3s without confirmation. */
@Composable
private fun CancelButton(onCancel: () -> Unit) {
    var arming by remember { mutableStateOf(false) }
    LaunchedEffect(arming) {
        if (arming) {
            delay(3000)
            arming = false
        }
    }
    WrkGhostButton(
        text = if (arming) "Tap again to end" else "End session",
        onClick = { if (arming) onCancel() else arming = true }
    )
}

@Composable
fun FinishedScreen(totalMillis: Long, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        WrkCaption("session complete")
        Spacer(Modifier.height(24.dp))
        Text(
            "${totalMillis / 60_000}",
            fontSize = 128.sp,
            fontWeight = FontWeight.ExtraLight,
        )
        WrkCaption("minutes of deep work")
        Spacer(Modifier.weight(1f))
        WrkButton("Done", onClick = onDone)
    }
}
