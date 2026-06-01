package com.profconq.app.ui.study

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun HighlightedExampleText(
    fullText: String,
    highlight: String,
    color: androidx.compose.ui.graphics.Color = PracticeSessionColors.TextPrimary,
    highlightColor: androidx.compose.ui.graphics.Color = PracticeSessionColors.Accent,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
) {
    val annotated = buildAnnotatedString {
        val start = fullText.indexOf(highlight, ignoreCase = true)
        if (highlight.isBlank() || start < 0) {
            append(fullText)
            return@buildAnnotatedString
        }
        val end = start + highlight.length
        append(fullText.substring(0, start))
        withStyle(
            SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.SemiBold,
                background = PracticeSessionColors.AccentSoft,
            ),
        ) {
            append(fullText.substring(start, end))
        }
        append(fullText.substring(end))
    }
    Text(text = annotated, color = color, fontSize = fontSize, lineHeight = 20.sp)
}
