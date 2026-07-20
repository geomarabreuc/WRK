package com.geomar.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SessionScreen(remainingMillis: Long, totalMillis: Long) {
    val totalSeconds = (remainingMillis + 999) / 1000
    val text = if (totalSeconds >= 3600) {
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, fontSize = 88.sp, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { 1f - remainingMillis.toFloat() / totalMillis },
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "Phone locked. Stay focused.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun FinishedScreen(totalMillis: Long, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Session complete", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "${totalMillis / 60_000} minutes of deep work.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(48.dp))
        androidx.compose.material3.Button(onClick = onDone) {
            Text("Done")
        }
    }
}
