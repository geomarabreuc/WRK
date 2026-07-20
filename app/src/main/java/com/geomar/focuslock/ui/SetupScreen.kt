package com.geomar.focuslock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PRESETS = listOf(25, 50, 90)
private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 480
private const val STEP = 5

@Composable
fun SetupScreen(onStart: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "WRK",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 10.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Stepper("−") { minutes = (minutes - STEP).coerceAtLeast(MIN_MINUTES) }
            Text(
                "$minutes",
                fontSize = 128.sp,
                fontWeight = FontWeight.ExtraLight,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Stepper("+") { minutes = (minutes + STEP).coerceAtMost(MAX_MINUTES) }
        }
        WrkCaption("minutes")

        Spacer(Modifier.height(56.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            PRESETS.forEach { preset ->
                val selected = minutes == preset
                Text(
                    "$preset",
                    fontSize = 18.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) WrkWhite else WrkDim,
                    textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { minutes = preset }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        WrkButton("Start focus", onClick = { onStart(minutes) })
    }
}

@Composable
private fun Stepper(symbol: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(symbol, fontSize = 34.sp, fontWeight = FontWeight.Light, color = WrkDim)
    }
}
