package com.profconq.app.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.StudyDayIntensity
import com.profconq.app.data.model.StudyIntensitySnapshot
import com.profconq.app.data.model.StudyWordDayCell
import com.profconq.app.progress.ProgressTracker
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
import com.profconq.app.ui.theme.PpPurple
import com.profconq.app.ui.theme.PpTextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val StudyIntensityOverviewDays = 14
internal const val StudyIntensityWordDays = 30

@Composable
internal fun rememberIntensityDateKeys(count: Int): List<String> {
    val today = remember { LocalDate.now() }
    return remember(today, count) {
        (count - 1 downTo 0).map { offset ->
            ProgressTracker.dateKey(today.minusDays(offset.toLong()))
        }
    }
}

fun LazyListScope.studyIntensitySection(
    snapshot: StudyIntensitySnapshot,
    selectedDateKey: String,
    onSelectDay: (String) -> Unit,
    onOpenWord: (String) -> Unit,
) {
    val dateKeys = run {
        val today = LocalDate.now()
        (StudyIntensityOverviewDays - 1 downTo 0).map { offset ->
            ProgressTracker.dateKey(today.minusDays(offset.toLong()))
        }
    }
    val series = snapshot.series(dateKeys)
    val words = snapshot.wordsOn(selectedDateKey)
    item {
        StudyIntensityCard(
            series = series,
            selectedDateKey = selectedDateKey,
            onSelectDay = onSelectDay,
        )
    }
    item {
        val strings = LocalUiStrings.current
        val locale = strings.javaLocale
        val dayLabel = remember(selectedDateKey, locale) {
            runCatching {
                LocalDate.parse(selectedDateKey)
                    .format(DateTimeFormatter.ofPattern("d MMMM", locale))
            }.getOrDefault(selectedDateKey)
        }
        Text(
            text = strings.studyIntensityWordsTitle(dayLabel),
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (words.isEmpty()) {
            PortCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                MutedText(strings.studyIntensityWordsEmpty)
            }
        }
    }
    items(words, key = { it.wordId }) { word ->
        WordIntensityRow(word = word, onOpen = { onOpenWord(word.wordId) })
    }
}

@Composable
fun WordIntensityScreen(
    wordId: String,
    snapshot: StudyIntensitySnapshot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val dateKeys = rememberIntensityDateKeys(StudyIntensityWordDays)
    val series = remember(snapshot, wordId, dateKeys) { snapshot.wordSeries(wordId, dateKeys) }
    val label = series.lastOrNull { it.pt.isNotBlank() } ?: snapshot.wordCell(wordId)
    val points = remember(series) {
        series.map {
            StudyDayIntensity(
                dateKey = it.dateKey,
                testRuns = it.testRuns,
                cardRuns = it.cardRuns,
                studioRuns = it.studioRuns,
                correct = it.correct,
                incorrect = it.incorrect,
            )
        }
    }
    val totals = remember(series) {
        StudyWordDayCell(
            dateKey = "",
            wordId = wordId,
            pt = label?.pt.orEmpty(),
            ru = label?.ru.orEmpty(),
            testRuns = series.sumOf { it.testRuns },
            cardRuns = series.sumOf { it.cardRuns },
            studioRuns = series.sumOf { it.studioRuns },
            correct = series.sumOf { it.correct },
            incorrect = series.sumOf { it.incorrect },
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
            title = totals.pt.ifBlank { strings.studyIntensityWordTitle },
            subtitle = totals.ru.ifBlank { strings.studyIntensityTitle },
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))
        IntensityChartCard(
            series = points,
            selectedDateKey = null,
            onSelectDay = null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PortCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.studyIntensityModules(totals.testRuns, totals.cardRuns, totals.studioRuns),
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.studyIntensityErrorPct(totals.errorPercent),
                color = PpDanger,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StudyIntensityCard(
    series: List<StudyDayIntensity>,
    selectedDateKey: String,
    onSelectDay: (String) -> Unit,
) {
    val strings = LocalUiStrings.current
    val selected = series.find { it.dateKey == selectedDateKey } ?: series.lastOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.studyIntensityTitle,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
        MutedText(strings.studyIntensityHint)
        IntensityChartCard(
            series = series,
            selectedDateKey = selectedDateKey,
            onSelectDay = onSelectDay,
        )
        IntensityLegend()
        if (selected != null) {
            Text(
                text = strings.studyIntensityModules(selected.testRuns, selected.cardRuns, selected.studioRuns),
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = strings.studyIntensityErrorPct(selected.errorPercent),
                color = PpDanger,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun IntensityChartCard(
    series: List<StudyDayIntensity>,
    selectedDateKey: String?,
    onSelectDay: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tests = PpNeonCyan
    val cards = PpNeonGreen
    val studio = PpPurple
    val error = PpDanger
    val grid = PpGlassBorder.copy(alpha = 0.45f)
    val selectedColor = PpHeading.copy(alpha = 0.35f)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .then(
                    if (onSelectDay != null) {
                        Modifier.pointerInput(series) {
                            detectTapGestures { offset ->
                                if (series.isEmpty()) return@detectTapGestures
                                val i = ((offset.x / size.width) * series.size)
                                    .toInt()
                                    .coerceIn(0, series.lastIndex)
                                onSelectDay(series[i].dateKey)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
        if (series.isEmpty()) return@Canvas
        val maxRuns = series.maxOf { maxOf(it.testRuns, it.cardRuns, it.studioRuns, it.totalRuns) }
            .coerceAtLeast(1)
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val plotW = (right - left).coerceAtLeast(1f)
        val plotH = (bottom - top).coerceAtLeast(1f)
        val last = series.lastIndex.coerceAtLeast(1)
        fun xAt(index: Int) = left + plotW * (index.toFloat() / last)
        fun yRun(value: Int) = bottom - (value.toFloat() / maxRuns) * plotH
        fun yErr(percent: Int) = bottom - (percent.coerceIn(0, 100) / 100f) * plotH

        listOf(0.25f, 0.5f, 0.75f).forEach { step ->
            val y = top + plotH * (1f - step)
            drawLine(
                color = grid,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val selectedIndex = series.indexOfFirst { it.dateKey == selectedDateKey }
        if (selectedIndex >= 0) {
            val x = xAt(selectedIndex)
            drawLine(
                color = selectedColor,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 2.dp.toPx(),
            )
        }

        fun drawCounts(color: Color, values: List<Int>, dashed: Boolean = false) {
            if (values.size == 1) {
                drawCircle(color, 4.dp.toPx(), Offset(xAt(0), yRun(values[0])))
                return
            }
            val path = Path()
            values.forEachIndexed { index, value ->
                val point = Offset(xAt(index), yRun(value))
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                ),
            )
        }

        drawCounts(tests, series.map { it.testRuns })
        drawCounts(cards, series.map { it.cardRuns })
        drawCounts(studio, series.map { it.studioRuns })

        val errPath = Path()
        series.forEachIndexed { index, day ->
            val point = Offset(xAt(index), yErr(day.errorPercent))
            if (index == 0) errPath.moveTo(point.x, point.y) else errPath.lineTo(point.x, point.y)
        }
        drawPath(
            path = errPath,
            color = error,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            ),
        )

        if (selectedIndex >= 0) {
            val day = series[selectedIndex]
            val x = xAt(selectedIndex)
            drawCircle(tests, 4.dp.toPx(), Offset(x, yRun(day.testRuns)))
            drawCircle(cards, 4.dp.toPx(), Offset(x, yRun(day.cardRuns)))
            drawCircle(studio, 4.dp.toPx(), Offset(x, yRun(day.studioRuns)))
            drawCircle(error, 4.dp.toPx(), Offset(x, yErr(day.errorPercent)))
        }
        }
        if (series.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MutedText(series.first().dateKey.takeLast(5).replace("-", "."))
                MutedText(series.last().dateKey.takeLast(5).replace("-", "."))
            }
        }
    }
}

@Composable
private fun IntensityLegend() {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LegendDot(PpNeonCyan, strings.studyIntensityTests, Modifier.weight(1f))
        LegendDot(PpNeonGreen, strings.studyIntensityCards, Modifier.weight(1f))
        LegendDot(PpPurple, strings.studyIntensityStudio, Modifier.weight(1f))
        LegendDot(PpDanger, strings.studyIntensityError, Modifier.weight(1f))
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            color = PpTextMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WordIntensityRow(
    word: StudyWordDayCell,
    onOpen: () -> Unit,
) {
    val strings = LocalUiStrings.current
    PortCard(
        modifier = Modifier
            .fillMaxWidth()
            .portClickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.studyIntensityModules(word.testRuns, word.cardRuns, word.studioRuns),
                    color = PpHeading,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = strings.studyIntensityErrorPct(word.errorPercent),
                    color = PpDanger,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = word.totalRuns.toString(),
                    color = PpNeonCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                MutedText(strings.studyTestRuns.lowercase())
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PpTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
