package com.geomar.focuslock.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.geomar.focuslock.Template
import java.util.Locale

private const val MINUTE_STEP = 5

@Composable
fun SetupScreen(
    templates: List<Template>,
    streak: Int,
    onShowHistory: () -> Unit,
    onStart: (Int) -> Unit,
    onSaveTemplate: (String, Int) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
) {
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    val totalMinutes = hours * 60 + minutes

    fun setTotal(total: Int) {
        hours = total / 60
        minutes = total % 60
    }

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
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onShowHistory,
                )
        ) {
            WrkCaption(if (streak > 0) "$streak day streak · calendar" else "calendar")
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.Top) {
            TimeUnit(
                value = hours,
                max = 24,
                twoDigits = false,
                label = "hours",
                onDec = { hours = (hours - 1).coerceAtLeast(0) },
                onInc = {
                    hours = (hours + 1).coerceAtMost(24)
                    if (hours == 24) minutes = 0
                },
                onSet = {
                    hours = it
                    if (hours == 24) minutes = 0
                },
            )
            Text(
                ":",
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraLight,
                color = WrkDim,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TimeUnit(
                value = minutes,
                max = 59,
                twoDigits = true,
                label = "minutes",
                onDec = { minutes = (minutes - MINUTE_STEP).coerceAtLeast(0) },
                onInc = { if (hours < 24) minutes = (minutes + MINUTE_STEP).coerceAtMost(59) },
                onSet = { if (hours < 24) minutes = it else minutes = 0 },
            )
        }

        Spacer(Modifier.height(48.dp))

        TemplatesSection(
            templates = templates,
            currentTotal = totalMinutes,
            onSelect = ::setTotal,
            onSave = onSaveTemplate,
            onDelete = onDeleteTemplate,
        )

        Spacer(Modifier.weight(1f))

        WrkButton("Start focus", onClick = { onStart(totalMinutes) }, enabled = totalMinutes > 0)
    }
}

/**
 * One picker column: big numeral (tap to type an exact value), caption,
 * − / + steppers underneath.
 */
@Composable
private fun TimeUnit(
    value: Int,
    max: Int,
    twoDigits: Boolean,
    label: String,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onSet: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val numeralStyle = TextStyle(
        fontSize = 76.sp,
        fontWeight = FontWeight.ExtraLight,
        color = WrkWhite,
        textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum",
    )

    fun commit() {
        text.toIntOrNull()?.let { onSet(it.coerceIn(0, max)) }
        editing = false
        hadFocus = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(2) },
                singleLine = true,
                textStyle = numeralStyle,
                cursorBrush = SolidColor(WrkWhite),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .width(120.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        // Ignore the initial unfocused callback fired on attach;
                        // only commit once focus was actually gained and then lost.
                        if (it.isFocused) hadFocus = true
                        else if (hadFocus && editing) commit()
                    }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        } else {
            Text(
                if (twoDigits) String.format(Locale.US, "%02d", value) else "$value",
                style = numeralStyle,
                modifier = Modifier
                    .width(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        text = ""
                        editing = true
                    }
            )
        }
        WrkCaption(label)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            Stepper("−", onDec)
            Stepper("+", onInc)
        }
    }
}

@Composable
private fun Stepper(symbol: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(1.dp, WrkFaint)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Light, color = WrkDim)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TemplatesSection(
    templates: List<Template>,
    currentTotal: Int,
    onSelect: (Int) -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (Template) -> Unit,
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }

    fun commit() {
        if (name.isNotBlank()) onSave(name, currentTotal)
        naming = false
        name = ""
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        WrkCaption("templates")
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            templates.forEach { template ->
                TemplateChip(
                    text = template.name,
                    emphasized = template.totalMinutes == currentTotal,
                    onClick = { onSelect(template.totalMinutes) },
                    onLongClick = { onDelete(template) },
                )
            }
            if (naming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, WrkWhite)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        singleLine = true,
                        textStyle = TextStyle(color = WrkWhite, fontSize = 14.sp),
                        cursorBrush = SolidColor(WrkWhite),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        "✓",
                        color = WrkWhite,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { commit() }
                    )
                }
            } else {
                // "+" chip — creates a template from the current duration.
                // Built like TemplateChip so it matches their exact size.
                TemplateChip(
                    text = "+",
                    emphasized = false,
                    onClick = { naming = true },
                    onLongClick = {},
                )
            }
        }
        if (templates.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            WrkCaption("tap to apply · hold to delete", color = WrkFaint)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateChip(
    text: String,
    emphasized: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .border(1.dp, if (emphasized) WrkWhite else WrkFaint)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (emphasized) WrkWhite else WrkDim,
            fontWeight = FontWeight.Normal,
        )
    }
}
