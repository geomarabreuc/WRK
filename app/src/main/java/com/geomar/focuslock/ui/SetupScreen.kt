package com.geomar.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val PRESETS = listOf(25, 50, 90)

@Composable
fun SetupScreen(onStart: (Int) -> Unit) {
    var selectedMinutes by rememberSaveable { mutableStateOf(25) }
    var customText by rememberSaveable { mutableStateOf("") }

    val customMinutes = customText.toIntOrNull()
    val effectiveMinutes = customMinutes ?: selectedMinutes
    val valid = effectiveMinutes in 1..480

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("FocusLock", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PRESETS.forEach { minutes ->
                FilterChip(
                    selected = customMinutes == null && selectedMinutes == minutes,
                    onClick = {
                        selectedMinutes = minutes
                        customText = ""
                    },
                    label = { Text("$minutes min") }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = customText,
            onValueChange = { customText = it.filter(Char::isDigit).take(3) },
            label = { Text("Custom minutes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { onStart(effectiveMinutes) },
            enabled = valid,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                "Start $effectiveMinutes min session",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
