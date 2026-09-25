package com.profconq.app.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.DailyStudyActivity
import com.profconq.app.data.model.LastStudyTestResult
import com.profconq.app.data.model.StudyDayBucket
import com.profconq.app.data.model.StudyDayStats
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.progress.ProgressTracker
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.glassCard
import com.profconq.app.ui.components.portClickable
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpGlassBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StudyActivityCalendar(
    dailyActivities: List<DailyStudyActivity>,
    dayBuckets: Map<String, StudyDayBucket>,
    extraActiveDays: Set<String> = emptySet(),
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val locale = strings.javaLocale
    val today = remember { LocalDate.now() }
    val todayKey = remember(today) { ProgressTracker.dateKey(today) }
    val accent = rememberAccentGradientBrush()
    val activeDays = remember(dailyActivities, dayBuckets, extraActiveDays) {
        buildSet {
            dailyActivities.forEach { day ->
                if (day.cardsStudied > 0 || day.minutesActive > 0) add(day.dateKey)
            }
            dayBuckets.forEach { (key, bucket) ->
                if (bucket.hasTests || bucket.matchGames > 0) add(key)
            }
            addAll(extraActiveDays)
        }
    }
    val weekdays = remember(locale) {
        (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) }
    }
    val cells = remember(month) { monthCells(month) }
    val monthTitle = remember(month, locale) {
        val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }} ${month.year}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onMonthChange(month.minusMonths(1)) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = PpNeonCyan,
                )
            }
            Text(
                text = monthTitle,
                color = PpHeading,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { if (month < YearMonth.from(today)) onMonthChange(month.plusMonths(1)) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (month < YearMonth.from(today)) PpNeonCyan else PpTextMuted,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.uppercase(locale),
                    color = PpTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val key = date?.let { ProgressTracker.dateKey(it) }
                    val active = key != null && key in activeDays
                    val isToday = key == todayKey
                    val future = date != null && date.isAfter(today)
                    CalendarDayCell(
                        day = date?.dayOfMonth,
                        active = active,
                        today = isToday,
                        enabled = date != null && !future,
                        accent = accent,
                        onClick = { if (key != null) onOpenDay(key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            text = strings.studyCalendarLegend,
            color = PpTextMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun StudyDayStatsScreen(
    stats: StudyDayStats,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val locale = strings.javaLocale
    val title = remember(stats.dateKey, locale) {
        val date = LocalDate.parse(stats.dateKey)
        date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }
    val words = remember(stats.tests.words) {
        stats.tests.words.sortedWith(
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
            title = strings.studyDayStatsTitle,
            subtitle = title,
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))
        if (!stats.hasActivity) {
            PortCard(modifier = Modifier.fillMaxWidth()) {
                MutedText(strings.studyDayNoActivity)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (stats.tests.hasTests) {
                    item {
                        LastTestHeroCard(
                            result = LastStudyTestResult(
                                setId = "day:${stats.dateKey}",
                                setName = title,
                                kind = LastStudyTestResult.KIND_DAY,
                                correct = stats.tests.correct,
                                incorrect = stats.tests.incorrect,
                                at = ProgressTracker.millisAtStartOf(stats.dateKey),
                                words = stats.tests.words,
                            ),
                        )
                    }
                }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayMetricTile(
                        value = stats.cardsStudied,
                        label = strings.studyDayCards,
                        modifier = Modifier.weight(1f),
                    )
                    DayMetricTile(
                        value = stats.tests.choiceTests,
                        label = strings.studyDayTests,
                        modifier = Modifier.weight(1f),
                    )
                    DayMetricTile(
                        value = stats.tests.matchGames,
                        label = strings.studyDayGames,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (stats.tests.sessions.isNotEmpty()) {
                item {
                    Text(
                        text = strings.studyDaySessionsTitle,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                itemsIndexed(
                    stats.tests.sessions,
                    key = { index, session -> "${session.at}-$index-${session.kind}" },
                ) { index, session ->
                    val kindLabel = if (session.kind == LastStudyTestResult.KIND_MATCH) {
                        strings.studyTestMatchTitle
                    } else {
                        strings.studyTestChoiceTitle
                    }
                    PortCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.studyDaySessionLine(index + 1, kindLabel, session.correct, session.incorrect),
                            color = PpHeading,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (session.setName.isNotBlank()) {
                            MutedText(session.setName)
                        }
                    }
                }
            }
                if (words.isNotEmpty()) {
                    items(words, key = { it.wordId }) { word ->
                        WordStatRow(word)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int?,
    active: Boolean,
    today: Boolean,
    enabled: Boolean,
    accent: androidx.compose.ui.graphics.Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (day != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .then(
                        when {
                            active -> Modifier
                                .background(PpNeonGreen.copy(alpha = 0.12f), shape)
                                .border(1.5.dp, accent, shape)
                            today -> Modifier.border(1.dp, PpNeonCyan.copy(alpha = 0.7f), shape)
                            else -> Modifier
                        },
                    )
                    .then(if (enabled) Modifier.portClickable(onClick = onClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    color = when {
                        !enabled -> PpTextMuted.copy(alpha = 0.35f)
                        active || today -> PpHeading
                        else -> PpTextMuted
                    },
                    fontWeight = if (active || today) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DayMetricTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glassCard(cornerRadius = 16.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = value.toString(),
            color = PpHeading,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
        )
        Text(
            text = label,
            color = PpTextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PpGlassBorder.copy(alpha = 0.28f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (value > 0) 1f else 0f)
                    .fillMaxSize()
                    .background(rememberAccentGradientBrush()),
            )
        }
    }
}

private fun monthCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - 1).coerceIn(0, 6)
    val days = month.lengthOfMonth()
    val cells = MutableList(lead) { null as LocalDate? }
    repeat(days) { index -> cells += month.atDay(index + 1) }
    while (cells.size % 7 != 0) cells += null
    return cells
}

fun mergeStudyDayStats(
    dateKey: String,
    dailyActivities: List<DailyStudyActivity>,
    dayBuckets: Map<String, StudyDayBucket>,
): StudyDayStats {
    val room = dailyActivities.find { it.dateKey == dateKey }
    return StudyDayStats(
        dateKey = dateKey,
        cardsStudied = room?.cardsStudied ?: 0,
        minutesActive = room?.minutesActive ?: 0,
        tests = dayBuckets[dateKey] ?: StudyDayBucket(dateKey = dateKey),
    )
}
