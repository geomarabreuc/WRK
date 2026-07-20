package com.geomar.focuslock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WrkWhite = Color(0xFFFFFFFF)
val WrkBlack = Color(0xFF000000)
val WrkDim = WrkWhite.copy(alpha = 0.35f)
val WrkFaint = WrkWhite.copy(alpha = 0.15f)

private val WrkColors = darkColorScheme(
    background = WrkBlack,
    surface = WrkBlack,
    primary = WrkWhite,
    onPrimary = WrkBlack,
    onBackground = WrkWhite,
    onSurface = WrkWhite,
    secondary = WrkWhite,
    outline = WrkDim,
)

@Composable
fun WrkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WrkColors, content = content)
}

/** Uppercase letterspaced label — the app's signature caption style. */
@Composable
fun WrkCaption(text: String, color: Color = WrkDim) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 4.sp,
    )
}

/** Solid white, squared, full-width primary action. */
@Composable
fun WrkButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = WrkWhite,
            contentColor = WrkBlack,
            disabledContainerColor = WrkFaint,
            disabledContentColor = WrkDim,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Text(text.uppercase(), letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Hairline-bordered, squared, full-width secondary action. */
@Composable
fun WrkGhostButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RectangleShape,
        border = BorderStroke(1.dp, WrkDim),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WrkWhite),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Text(text.uppercase(), letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
    }
}
