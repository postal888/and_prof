package com.profconq.app.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.ProgressSnapshot
import com.profconq.app.data.model.WeeklyActivityMetric
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpInfo
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.PpWarning
import com.profconq.app.ui.i18n.LocalUiStrings

@Composable
fun ProgressScreen(
    progress: ProgressSnapshot,
    onResetProgress: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.progressResetTitle) },
            text = { Text(strings.progressResetBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                ) {
                    Text(strings.progressResetConfirm, color = PpWarning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            if (onBack != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.commonBack,
                            tint = PpAccent,
                        )
                    }
                }
            }
            TabScreenHeader(
                title = strings.tabProgress,
                subtitle = "Bom dia, ${progress.userName} 👋",
            )
            ProgressHeader(
                userName = progress.userName,
                onResetClick = { showResetDialog = true },
            )
        }

        item { DailyMetricsRow(progress) }

        item { HeatmapCard(progress) }

        item { VocabularyTrendCard(progress) }

        item { WeeklyActivityCard(progress.weeklyActivities) }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ProgressHeader(userName: String, onResetClick: () -> Unit) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onResetClick,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(strings.progressResetButton, style = MaterialTheme.typography.labelMedium, color = PpTextMuted)
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PpAccent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userName.take(2).uppercase(),
                    color = PpHeading,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DailyMetricsRow(progress: ProgressSnapshot) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiniStatCard(
            icon = { Icon(Icons.Default.LocalFireDepartment, null, tint = PpWarning) },
            value = "${progress.streakDays} дн",
            label = "Streak",
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            icon = { Icon(Icons.Default.TrackChanges, null, tint = PpAccent) },
            value = "${progress.dailyGoalCurrent}/${progress.dailyGoalTarget}",
            label = strings.progressGoalLabel,
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            icon = { Icon(Icons.Default.Schedule, null, tint = PpInfo) },
            value = "${progress.minutesToday} мин",
            label = strings.progressTodayLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStatCard(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    PortCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon()
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = PpTextMuted,
            )
        }
    }
}

@Composable
private fun HeatmapCard(progress: ProgressSnapshot) {
    val strings = LocalUiStrings.current
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title = strings.progressActivity365)
            HeatmapGrid(levels = progress.heatmap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeatStat("${progress.streakDays} дн", "streak")
                HeatStat("${progress.heatmapRecordStreak} дн", strings.progressHeatRecord)
                HeatStat("${progress.heatmapActiveDaysPercent}%", strings.progressHeatDaysInYear)
                HeatStat("${progress.heatmapAverageScore}", strings.progressHeatAverage)
            }
        }
    }
}

@Composable
private fun HeatStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = PpHeading, style = MaterialTheme.typography.labelLarge)
        Text(text = label, color = PpTextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeatmapGrid(levels: List<Int>) {
    val rows = levels.chunked(7).takeLast(8)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatmapColor(level)),
                    )
                }
            }
        }
    }
}

private fun heatmapColor(level: Int): Color = when (level) {
    0 -> Color(0xFF12161F)
    1 -> Color(0xFF14532D)
    2 -> Color(0xFF166534)
    3 -> Color(0xFF16A34A)
    else -> Color(0xFF4ADE80)
}

@Composable
private fun VocabularyTrendCard(progress: ProgressSnapshot) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    SectionTitle(title = LocalUiStrings.current.progressKnownWords)
                    Text(
                        text = "${progress.knownWords}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PpHeading,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "+${progress.vocabularyWeeklyGain}",
                    color = PpAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LineTrendChart(points = progress.vocabularyTrend, modifier = Modifier.fillMaxWidth().height(90.dp))
        }
    }
}

@Composable
private fun LineTrendChart(points: List<Int>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val max = points.maxOrNull()?.coerceAtLeast(1) ?: 1
    val lineColor = PpAccent
    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val pathY = points.mapIndexed { index, value ->
            Offset(
                x = stepX * index,
                y = size.height - (value.toFloat() / max * size.height * 0.85f),
            )
        }
        for (i in 0 until pathY.lastIndex) {
            drawLine(
                color = lineColor,
                start = pathY[i],
                end = pathY[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(activities: List<WeeklyActivityMetric>) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title = LocalUiStrings.current.progressWeeklyActivity)
            activities.forEach { activity ->
                val percent = ((activity.current / activity.target.coerceAtLeast(1f)) * 100f).toInt().coerceIn(0, 100)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(activity.label, color = PpText, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${formatActivityValue(activity)} / ${formatActivityTarget(activity)} ($percent%)",
                            color = PpTextMuted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { activity.current / activity.target.coerceAtLeast(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = Color(activity.colorArgb),
                        trackColor = PpSurfaceInput,
                    )
                }
            }
        }
    }
}

private fun formatActivityValue(activity: WeeklyActivityMetric): String =
    if (activity.unit == "ч") "${activity.current}" else activity.current.toInt().toString()

private fun formatActivityTarget(activity: WeeklyActivityMetric): String =
    if (activity.unit == "ч") "${activity.target}${activity.unit}" else activity.target.toInt().toString()
