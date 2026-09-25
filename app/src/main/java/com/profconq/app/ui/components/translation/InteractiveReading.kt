package com.profconq.app.ui.components.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.profconq.app.ui.components.GradientCircularLoader
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.ReaderLineSpacing
import com.profconq.app.data.model.SubtitleCursorMode
import com.profconq.app.reader.ReaderFontSize
import com.profconq.app.data.repository.WordNormalizer
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpAccentSoft
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.youtube.SelectedWord

data class ReadingWordToken(
    val cleanPt: String,
    val start: Int,
    val end: Int,
)

@Composable
fun readingTextStyle(
    fontSizeLevel: Int,
    lineSpacingPercent: Int = ReaderLineSpacing.DEFAULT_PERCENT,
): TextStyle {
    val fontSize = ReaderFontSize.fontSizeForLevel(fontSizeLevel)
    val lineHeight = ReaderFontSize.lineHeightForLevel(fontSizeLevel, lineSpacingPercent)
    return MaterialTheme.typography.bodyMedium.copy(
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = PpText,
    )
}

private const val PhrasePreviewMaxLines = 3

@Composable
private fun threeLineTextHeight(textStyle: TextStyle): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    val lineHeight: TextUnit = textStyle.lineHeight
    val lineHeightDp = if (!lineHeight.isUnspecified) {
        with(density) { lineHeight.toDp() }
    } else {
        with(density) { (textStyle.fontSize.value * 1.35f).sp.toDp() }
    }
    return lineHeightDp * PhrasePreviewMaxLines
}

@Composable
fun WordTranslationBar(
    word: SelectedWord,
    canAddToDictionary: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val phraseBodyStyle = MaterialTheme.typography.bodyMedium
    val phrasePreviewHeight = if (word.isPhrase) threeLineTextHeight(phraseBodyStyle) else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = if (word.isPhrase) Alignment.Top else Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (word.isPhrase) {
                MutedText(strings.wordTranslationPhraseTitle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(phrasePreviewHeight!!),
                    contentAlignment = Alignment.TopStart,
                ) {
                    when {
                        word.isTranslating -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GradientCircularLoader(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                                MutedText(strings.wordTranslationInProgress)
                            }
                        }
                        word.ru != null -> {
                            Text(
                                text = word.ru,
                                style = phraseBodyStyle,
                                color = PpText,
                                maxLines = PhrasePreviewMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        else -> MutedText(strings.wordTranslationUnavailable)
                    }
                }
            } else {
                Text(
                    text = word.pt,
                    style = MaterialTheme.typography.titleMedium,
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                )
                when {
                    word.isTranslating -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GradientCircularLoader(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                            MutedText(strings.wordTranslationInProgress)
                        }
                    }
                    word.ru != null -> {
                        Text(
                            text = word.ru,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PpText,
                        )
                        word.ptInfinitive?.let { inf ->
                            MutedText(strings.wordTranslationInfinitive(inf))
                        }
                    }
                    else -> MutedText(strings.wordTranslationUnavailable)
                }
                word.example
                    ?.takeIf { it.isNotBlank() && !it.equals(word.pt, ignoreCase = true) }
                    ?.let { example ->
                        MutedText(strings.wordTranslationExample(example))
                    }
            }
        }

        if (word.isAdded) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = strings.wordTranslationAdded,
                tint = PpAccent,
                modifier = Modifier.size(22.dp),
            )
        } else if (canAddToDictionary) {
            IconButton(
                onClick = onAdd,
                enabled = !word.isTranslating && !word.ru.isNullOrBlank(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.wordTranslationAddToDictionary,
                    tint = PpAccent,
                )
            }
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.wordTranslationClose,
                tint = PpTextMuted,
            )
        }
    }
}

@Composable
fun CursorModeToggle(
    mode: SubtitleCursorMode,
    onModeChange: (SubtitleCursorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CursorModeButton(
            label = strings.cursorModeWord,
            selected = mode == SubtitleCursorMode.Tap,
            onClick = { onModeChange(SubtitleCursorMode.Tap) },
            modifier = Modifier.weight(1f),
        )
        CursorModeButton(
            label = strings.cursorModePhrase,
            selected = mode == SubtitleCursorMode.Select,
            onClick = { onModeChange(SubtitleCursorMode.Select) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CursorModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PpAccentSoft else PpSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (selected) PpAccent else PpTextMuted,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun InteractiveParagraph(
    text: String,
    cursorMode: SubtitleCursorMode,
    savedWords: Set<String>,
    textStyle: TextStyle,
    flowRowLineGap: androidx.compose.ui.unit.Dp,
    onWordClick: (ReadingWordToken) -> Unit,
    onPhraseSelected: (String) -> Unit,
    onPhraseSelectionChange: (String) -> Unit = {},
    onSelectionActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (cursorMode) {
        SubtitleCursorMode.Tap -> InteractiveWords(
            text = text,
            savedWords = savedWords,
            textStyle = textStyle,
            flowRowLineGap = flowRowLineGap,
            onWordClick = onWordClick,
            modifier = modifier,
        )
        SubtitleCursorMode.Select -> SelectableReadingText(
            text = text,
            textStyle = textStyle,
            onPhraseSelectionChange = onPhraseSelectionChange,
            onSelectionActiveChange = onSelectionActiveChange,
            modifier = modifier,
        )
    }
}

@Composable
private fun SelectableReadingText(
    text: String,
    textStyle: TextStyle,
    onPhraseSelectionChange: (String) -> Unit,
    onSelectionActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember(text) { mutableStateOf(TextFieldValue(text)) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val selectionActive = !fieldValue.selection.collapsed

    DisposableEffect(selectionActive) {
        val previousHaptic = view.isHapticFeedbackEnabled
        if (selectionActive) {
            view.isHapticFeedbackEnabled = false
        }
        onDispose {
            view.isHapticFeedbackEnabled = previousHaptic
        }
    }

    val selectedPhrase = remember(fieldValue, text) {
        PhraseSelectionLimits.extractPhrase(text, fieldValue.selection)
    }

    LaunchedEffect(selectionActive) {
        onSelectionActiveChange(selectionActive)
        if (selectionActive) {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(selectedPhrase) {
        if (selectedPhrase.isNotEmpty()) {
            onPhraseSelectionChange(selectedPhrase)
        }
    }

    val blockListScrollWhileSelecting = remember(selectionActive) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!selectionActive || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                return Offset(0f, available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!selectionActive || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                return Offset(0f, available.y)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(blockListScrollWhileSelecting),
    ) {
        val selectionColors = TextSelectionColors(
            handleColor = PpAccent,
            backgroundColor = PpAccentSoft,
        )
        CompositionLocalProvider(
            LocalTextSelectionColors provides selectionColors,
            LocalTextStyle provides TextStyle.Default,
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val clampedSelection = PhraseSelectionLimits.clampSelectionRange(
                        text = text,
                        selection = TextRange(
                            start = newValue.selection.start.coerceIn(0, text.length),
                            end = newValue.selection.end.coerceIn(0, text.length),
                        ),
                    )
                    fieldValue = TextFieldValue(text = text, selection = clampedSelection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            keyboardController?.hide()
                        }
                    },
                textStyle = textStyle,
                cursorBrush = SolidColor(PpAccent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }
}

/** Preview of the selected phrase: fixed height of exactly [PhrasePreviewMaxLines] text lines. */
@Composable
fun PhrasePreviewBar(
    phrase: String,
    textStyle: TextStyle,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val previewHeight = threeLineTextHeight(textStyle)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(previewHeight),
        ) {
            Text(
                text = phrase,
                style = textStyle,
                color = PpText,
                maxLines = PhrasePreviewMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onTranslate) {
            Text(strings.wordTranslationTranslateButton, color = PpAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PhraseSelectionActions(
    phrase: String,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhrasePreviewBar(
        phrase = phrase,
        textStyle = MaterialTheme.typography.bodyMedium,
        onTranslate = onTranslate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveWords(
    text: String,
    savedWords: Set<String>,
    textStyle: TextStyle,
    flowRowLineGap: androidx.compose.ui.unit.Dp,
    onWordClick: (ReadingWordToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(text) { tokenizeReadingText(text) }
    val rowGap = flowRowLineGap

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(rowGap),
    ) {
        tokens.forEach { token ->
            if (token.isWord) {
                val saved = savedWords.contains(WordNormalizer.normalize(token.cleanPt))
                Text(
                    text = token.display,
                    modifier = Modifier.clickable {
                        onWordClick(
                            ReadingWordToken(
                                cleanPt = token.cleanPt,
                                start = token.start,
                                end = token.end,
                            ),
                        )
                    },
                    style = textStyle.copy(
                        color = if (saved) PpAccent else PpText,
                        fontWeight = if (saved) FontWeight.SemiBold else FontWeight.Normal,
                        textDecoration = if (saved) TextDecoration.Underline else TextDecoration.None,
                    ),
                )
            } else {
                Text(
                    text = token.display,
                    style = textStyle,
                )
            }
        }
    }
}

private data class ReadingToken(
    val display: String,
    val isWord: Boolean,
    val cleanPt: String = "",
    val start: Int = 0,
    val end: Int = 0,
)

private fun tokenizeReadingText(text: String): List<ReadingToken> {
    val regex = Regex("""(\S+|\s+)""")
    return regex.findAll(text).map { match ->
        val raw = match.value
        val start = match.range.first
        val end = match.range.last + 1
        if (raw.isBlank()) {
            ReadingToken(display = raw, isWord = false, start = start, end = end)
        } else {
            val clean = raw
                .trim(',', '.', '!', '?', ';', ':', '"', '\'', ')', '»', '…', '—')
                .trim('(', '«', '"', '\'')
            ReadingToken(
                display = raw,
                isWord = clean.any { it.isLetter() },
                cleanPt = clean,
                start = start,
                end = end,
            )
        }
    }.toList()
}

fun splitBookParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .let { normalized ->
            val doubleBreak = normalized.split(Regex("""\n\s*\n+"""))
                .map { it.trim().replace(Regex("""\s+"""), " ") }
                .filter { it.isNotEmpty() }
            if (doubleBreak.size > 1) {
                doubleBreak
            } else {
                normalized.lineSequence()
                    .map { it.trim().replace(Regex("""\s+"""), " ") }
                    .filter { it.isNotEmpty() }
                    .toList()
            }
        }
