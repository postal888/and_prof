package com.profconq.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.WordCard
import com.profconq.app.studio.studioIsPhraseTag
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.UiStrings

object StudioCardColors {
    val Surface = Color(0xFF10192B)
    val Border = Color(0xFF2A3A58)
    val Text = Color(0xFFE8EEF8)
    val Muted = Color(0xFF93A4BF)
    val Cyan = Color(0xFF00D2FF)
    val HitBg = Color(0x2600D2FF)
    val TagBg = Color(0xFFF3E7C9)
    val TagInk = Color(0xFF8A6A1F)
    val TagBorder = Color(0x478A6A1F)
    val DirBg = Color(0x2614C8E8)
    val DirBorder = Color(0x6600D2FF)
}

enum class StudioCardFace {
    Front,
    Back,
}

fun DrawScope.drawStudioNotebook() {
    drawRect(StudioCardColors.Surface)
    val step = 12.dp.toPx()
    val line = Color.White.copy(alpha = 0.045f)
    var x = 0f
    while (x < size.width) {
        drawLine(color = line, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += step
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x3300D2FF), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.38f,
        ),
    )
}

@Composable
fun StudioTagChip(tag: String, strings: UiStrings, modifier: Modifier = Modifier) {
    Text(
        text = strings.studioTagLabel(tag),
        color = StudioCardColors.TagInk,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(StudioCardColors.TagBg, RoundedCornerShape(999.dp))
            .border(1.dp, StudioCardColors.TagBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun StudioDirChip(
    dirPtToRu: Boolean,
    sourceLang: Int,
    targetLang: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val chipModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    val strings = LocalUiStrings.current
    Text(
        text = if (dirPtToRu) {
            strings.studioDirLabel(sourceLang, targetLang)
        } else {
            strings.studioDirLabel(targetLang, sourceLang)
        },
        color = StudioCardColors.Cyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = chipModifier
            .background(StudioCardColors.DirBg, RoundedCornerShape(999.dp))
            .border(1.dp, StudioCardColors.DirBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun StudioCardContent(card: WordCard, reveal: Int, dirPtToRu: Boolean) {
    StudioCardLines(card = card, dirPtToRu = dirPtToRu, reveal = reveal, face = null)
}

@Composable
fun StudioFlipCardContent(card: WordCard, dirPtToRu: Boolean, showBack: Boolean) {
    StudioCardLines(
        card = card,
        dirPtToRu = dirPtToRu,
        reveal = if (showBack) 3 else 0,
        face = if (showBack) StudioCardFace.Back else StudioCardFace.Front,
    )
}

@Composable
private fun StudioCardLines(
    card: WordCard,
    dirPtToRu: Boolean,
    reveal: Int,
    face: StudioCardFace?,
) {
    val pt = card.pt.trim().ifEmpty { card.example.orEmpty() }
    val ru = card.ru.trim().ifEmpty { card.exampleTranslation.orEmpty() }
    val ex = card.example.orEmpty().trim()
    val exRu = card.exampleTranslation.orEmpty().trim()
    val isPhrase = studioIsPhraseTag(card.partOfSpeech)
    fun visible(minReveal: Int): Boolean = when (face) {
        StudioCardFace.Front -> minReveal == 0
        StudioCardFace.Back -> minReveal >= 1
        null -> reveal >= minReveal
    }
    fun emphasize(minReveal: Int): Boolean = when (face) {
        StudioCardFace.Front -> minReveal == 0
        StudioCardFace.Back -> minReveal == 1
        null -> reveal == minReveal
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isPhrase) {
            val first = if (dirPtToRu) ex.ifBlank { pt } else exRu.ifBlank { ru }
            val second = if (dirPtToRu) exRu.ifBlank { ru } else ex.ifBlank { pt }
            if (face != StudioCardFace.Back) {
                StudioLine(
                    text = first,
                    visible = visible(0),
                    emphasize = emphasize(0),
                    style = StudioLineStyle.Phrase,
                    highlight = if (dirPtToRu) card.pt else "",
                )
            }
            if (second.isNotBlank() && face != StudioCardFace.Front) {
                StudioLine(
                    text = second,
                    visible = visible(1),
                    emphasize = emphasize(1),
                    style = StudioLineStyle.PhraseRu,
                    highlight = if (!dirPtToRu) card.pt else "",
                )
            }
        } else {
            val main = if (dirPtToRu) pt else ru
            val tr = if (dirPtToRu) ru else pt
            if (face != StudioCardFace.Back) {
                StudioLine(main, visible(0), emphasize(0), StudioLineStyle.Word)
            }
            if (tr.isNotBlank() && face != StudioCardFace.Front) {
                StudioLine(tr.uppercase(), visible(1), emphasize(1), StudioLineStyle.Translation)
            }
            if (ex.isNotBlank() && !ex.equals(pt, ignoreCase = true) && face != StudioCardFace.Front) {
                StudioLine(ex, visible(2), emphasize(2), StudioLineStyle.Example, highlight = card.pt)
            }
            if (exRu.isNotBlank() && !exRu.equals(ru, ignoreCase = true) && face != StudioCardFace.Front) {
                StudioLine(exRu, visible(3), emphasize(3), StudioLineStyle.Example)
            }
        }
    }
}

private enum class StudioLineStyle { Word, Translation, Example, Phrase, PhraseRu }

@Composable
private fun StudioLine(
    text: String,
    visible: Boolean,
    emphasize: Boolean,
    style: StudioLineStyle,
    highlight: String = "",
) {
    val alpha = if (!visible) 0.34f else 1f
    val color = when (style) {
        StudioLineStyle.Word, StudioLineStyle.Phrase -> StudioCardColors.Text
        StudioLineStyle.Translation -> StudioCardColors.Cyan
        StudioLineStyle.Example, StudioLineStyle.PhraseRu -> StudioCardColors.Muted
    }
    val size = when (style) {
        StudioLineStyle.Word -> 26.sp
        StudioLineStyle.Translation -> 16.sp
        StudioLineStyle.Phrase -> 20.sp
        else -> 16.sp
    }
    val weight = when (style) {
        StudioLineStyle.Word, StudioLineStyle.Translation -> FontWeight.ExtraBold
        StudioLineStyle.Phrase -> FontWeight.Bold
        else -> FontWeight.Medium
    }
    Text(
        text = highlightExample(text, highlight, StudioCardColors.Cyan, StudioCardColors.HitBg),
        color = color,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = if (style == StudioLineStyle.Translation) 0.8.sp else 0.sp,
        textAlign = TextAlign.Center,
        lineHeight = if (style == StudioLineStyle.Word) 32.sp else 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha * if (emphasize || !visible) 1f else 0.92f),
    )
}

private fun highlightExample(
    text: String,
    word: String,
    color: Color,
    background: Color,
): AnnotatedString {
    val needle = word.trim()
    if (needle.isEmpty() || text.isEmpty()) return AnnotatedString(text)
    val match = Regex(Regex.escape(needle), RegexOption.IGNORE_CASE).find(text)
        ?: return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, match.range.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, background = background)) {
            append(match.value)
        }
        append(text.substring(match.range.last + 1))
    }
}
