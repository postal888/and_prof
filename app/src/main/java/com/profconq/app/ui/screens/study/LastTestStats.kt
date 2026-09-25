package com.profconq.app.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.LastStudyTestResult
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.glassCard
import com.profconq.app.ui.components.portClickable
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpGlassBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LastTestHeroCard(
    result: LastStudyTestResult,
    onOpen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val dateLabel = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(result.at))
    val kind = when (result.kind) {
        LastStudyTestResult.KIND_MATCH -> strings.studyTestMatchTitle
        LastStudyTestResult.KIND_DAY -> strings.studyDayTests
        else -> strings.studyTestChoiceTitle
    }
    val correctShare = if (result.total > 0) result.correct.toFloat() / result.total else 0f
    val incorrectShare = if (result.total > 0) result.incorrect.toFloat() / result.total else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .then(if (onOpen != null) Modifier.portClickable(onClick = onOpen) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.setName.ifBlank { strings.studyTestTitle },
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MutedText("$kind · $dateLabel")
            }
            if (onOpen != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = strings.studyLastTestHint,
                    tint = PpTextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccuracyRing(percent = result.accuracy)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultPill(
                    value = result.correct,
                    label = strings.studyTestCorrect,
                    color = PpNeonGreen,
                )
                ResultPill(
                    value = result.incorrect,
                    label = strings.studyTestIncorrect,
                    color = PpDanger,
                )
                ResultPill(
                    value = result.wordRuns,
                    label = strings.studyTestRuns,
                    color = PpNeonCyan,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(PpGlassBorder.copy(alpha = 0.28f)),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (correctShare > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(correctShare)
                                .fillMaxHeight()
                                .background(PpNeonGreen),
                        )
                    }
                    if (incorrectShare > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(incorrectShare)
                                .fillMaxHeight()
                                .background(PpDanger),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.studyTestAccuracy(result.accuracy),
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${result.correct}/${result.total}",
                    color = PpTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (onOpen != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strings.studyLastTestHint,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PpNeonGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun LastTestWordsScreen(
    result: LastStudyTestResult,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val words = remember(result.words) {
        result.words.sortedWith(
            compareByDescending<StudyTestWordStat> { it.incorrect }
                .thenByDescending { it.attempts }
                .thenBy { it.pt.lowercase(Locale.getDefault()) },
        )
    }
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
    ) {
        TabScreenHeader(
            title = strings.studyLastTestWordsTitle,
            subtitle = result.setName.ifBlank { strings.studyLastTestTitle },
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))
        LastTestHeroCard(result = result)
        Spacer(modifier = Modifier.height(16.dp))
        if (words.isEmpty()) {
            PortCard(modifier = Modifier.fillMaxWidth()) {
                MutedText(strings.studyLastTestWordsEmpty)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(words, key = { it.wordId }) { word ->
                    WordStatRow(word)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AccuracyRing(percent: Int, modifier: Modifier = Modifier) {
    val strings = LocalUiStrings.current
    val sweep = (percent.coerceIn(0, 100) / 100f) * 360f
    val ringBrush = rememberAccentGradientBrush()
    val trackColor = PpGlassBorder.copy(alpha = 0.35f)
    Box(
        modifier = modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                color = PpHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            )
            Text(
                text = strings.studyTestAccuracyLabel,
                color = PpTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ResultPill(
    value: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = value.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}

@Composable
internal fun WordStatRow(word: StudyTestWordStat) {
    val strings = LocalUiStrings.current
    val share = if (word.attempts > 0) word.correct.toFloat() / word.attempts else 0f
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.pt,
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = word.ru,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${word.correct} ${strings.studyTestCorrect.lowercase(Locale.getDefault())}",
                    color = PpNeonGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${word.incorrect} ${strings.studyTestIncorrect.lowercase(Locale.getDefault())}",
                    color = PpDanger,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${word.effectiveRuns} ${strings.studyTestRuns.lowercase(Locale.getDefault())}",
                    color = PpNeonCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PpGlassBorder.copy(alpha = 0.28f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share)
                    .fillMaxHeight()
                    .background(PpNeonGreen),
            )
        }
    }
}
