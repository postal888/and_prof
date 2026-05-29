package com.proficon.app.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.proficon.app.data.model.ProgressSnapshot
import com.proficon.app.data.model.TopicRetention
import com.proficon.app.data.model.WeeklyActivityMetric
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.PortCard
import com.proficon.app.ui.components.ProficonBrandHeader
import com.proficon.app.ui.components.SectionTitle
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.theme.PpGold
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpInfo
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
import com.proficon.app.ui.theme.PpTextMuted
import com.proficon.app.ui.theme.PpWarning
import kotlin.math.min

@Composable
fun ProgressScreen(
    progress: ProgressSnapshot,
    onOpenStudy: () -> Unit,
    onOpenPractice: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить прогресс?") },
            text = {
                Text(
                    "Будут обнулены streak, heatmap, недельная активность и счётчики целей. " +
                        "Карточки и словарь останутся без изменений.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                ) {
                    Text("Сбросить", color = PpWarning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
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
                            contentDescription = "Назад",
                            tint = PpAccent,
                        )
                    }
                }
            }
            ProficonBrandHeader(
                title = "Прогресс",
                subtitle = "Bom dia, ${progress.userName} 👋",
            )
            ProgressHeader(
                userName = progress.userName,
                onResetClick = { showResetDialog = true },
            )
        }

        item { ExamPrepCard(progress) }

        item { DailyMetricsRow(progress) }

        item { AiMentorCard(progress, onOpenPractice, onOpenStudy) }

        item { CefrDonutCard(progress) }

        item { HeatmapCard(progress) }

        item { VocabularyTrendCard(progress) }

        item { WeeklyActivityCard(progress.weeklyActivities) }

        item { StrengthsWeaknessesCard(progress.topicStats) }

        item { PeerComparisonCard(progress) }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ProgressHeader(userName: String, onResetClick: () -> Unit) {
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
                Text("Сброс", style = MaterialTheme.typography.labelMedium, color = PpTextMuted)
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
private fun ExamPrepCard(progress: ProgressSnapshot) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101820), Color(0xFF0A0D14)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = progress.examLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = PpGold,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MutedText("Подготовка к экзамену")
                }
                Text("🇧🇷", style = MaterialTheme.typography.titleLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricBlock("${progress.daysToExam}", "ДНЕЙ")
                MetricBlock("${progress.weeksToExam}", "НЕДЕЛЬ")
                MetricBlock(progress.currentLevel, "ТЕКУЩИЙ")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress.b2ReadinessPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = PpAccent,
                    trackColor = PpSurfaceInput,
                    strokeCap = StrokeCap.Round,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MutedText("Готовность к B2")
                    Text(
                        text = "${progress.b2ReadinessPercent}% • нужно ещё ${progress.wordsToB2} слова",
                        style = MaterialTheme.typography.labelMedium,
                        color = PpText,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(PpSurfaceInput)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (progress.onTrack) PpAccent else PpWarning),
                )
                Text(
                    text = if (progress.onTrack) "В графике подготовки" else "Нужно ускориться",
                    style = MaterialTheme.typography.labelMedium,
                    color = PpText,
                )
            }
        }
    }
}

@Composable
private fun MetricBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = PpHeading,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PpTextMuted,
        )
    }
}

@Composable
private fun DailyMetricsRow(progress: ProgressSnapshot) {
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
            label = "Цель",
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            icon = { Icon(Icons.Default.Schedule, null, tint = PpInfo) },
            value = "${progress.minutesToday} мин",
            label = "Сегодня",
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
private fun AiMentorCard(
    progress: ProgressSnapshot,
    onOpenPractice: () -> Unit,
    onOpenStudy: () -> Unit,
) {
    PortCard(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1420), RoundedCornerShape(10.dp)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PpInfo)
                Column {
                    Text(
                        text = "Eduardo • AI-наставник",
                        style = MaterialTheme.typography.titleMedium,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MutedText("Отчёт за неделю • обновлено сегодня")
                }
            }

            Text(
                text = buildAnnotatedString {
                    append("${progress.userName}, за неделю ты прошёл ")
                    withStyle(SpanStyle(color = PpInfo, fontWeight = FontWeight.SemiBold)) {
                        append("${progress.weeklyCards} карточек")
                    }
                    append(", retention ${progress.retentionPercent}% — лучше прошлой недели (${progress.previousRetentionPercent}%).\n\n")
                    append("Заметил: ты пропускаешь карточки по теме ")
                    withStyle(SpanStyle(color = PpInfo, fontWeight = FontWeight.SemiBold)) {
                        append(progress.weakTopic)
                    }
                    append(". Это нормально — для русскоязычных это сложная тема.\n\n")
                    append("Рекомендую посмотреть видео Casimiro — там много примеров conjuntivo в разговорной речи. До CELPE-Bras ${progress.daysToExam} дней, ты ")
                    withStyle(SpanStyle(color = PpInfo, fontWeight = FontWeight.SemiBold)) {
                        append("в хорошем темпе для B2")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PpText,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenPractice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.PlayCircleOutline, null, tint = PpInfo, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Посмотреть видео", color = PpText, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onOpenStudy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Тренировать ${progress.weakTopic}", color = PpText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CefrDonutCard(progress: ProgressSnapshot) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title = "CEFR ПРОГРЕСС")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    DonutChart(
                        progress = progress.b2ReadinessPercent / 100f,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${progress.b2ReadinessPercent}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = PpHeading,
                            fontWeight = FontWeight.Bold,
                        )
                        MutedText("B1 → B2")
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${progress.knownWords} / ${progress.b2WordTarget} слов",
                        style = MaterialTheme.typography.titleMedium,
                        color = PpHeading,
                    )
                    MutedText(
                        "При текущем темпе B2 к ${progress.predictedB2DateLabel} " +
                            "(${progress.daysBeforeExamAtPace} дней до экзамена).",
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(progress: Float, modifier: Modifier = Modifier) {
    val accent = PpAccent
    val warning = PpWarning
    Canvas(modifier = modifier) {
        val stroke = 12.dp.toPx()
        val diameter = min(size.width, size.height) - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Color(0xFF1A1F2B),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(accent, warning, accent)),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun HeatmapCard(progress: ProgressSnapshot) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title = "АКТИВНОСТЬ • 365 ДНЕЙ")
            HeatmapGrid(levels = progress.heatmap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeatStat("${progress.streakDays} дн", "streak")
                HeatStat("${progress.heatmapRecordStreak} дн", "рекорд")
                HeatStat("${progress.heatmapActiveDaysPercent}%", "дней в году")
                HeatStat("${progress.heatmapAverageScore}", "среднее")
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
                    SectionTitle(title = "ИЗВЕСТНЫЕ СЛОВА")
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
            SectionTitle(title = "НЕДЕЛЬНАЯ АКТИВНОСТЬ")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StrengthsWeaknessesCard(topics: List<TopicRetention>) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(title = "СИЛЬНОЕ И СЛАБОЕ")
                Text(
                    text = "AI анализ",
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(PpSurfaceInput)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = PpAccent,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                topics.forEach { topic ->
                    TopicChip(topic)
                }
            }
        }
    }
}

@Composable
private fun TopicChip(topic: TopicRetention) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(topic.title, color = PpHeading, style = MaterialTheme.typography.labelLarge)
        Text("Retention ${topic.retentionPercent}%", color = PpTextMuted, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { topic.retentionPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = Color(topic.colorArgb),
            trackColor = Color(0xFF151A24),
        )
    }
}

@Composable
private fun PeerComparisonCard(progress: ProgressSnapshot) {
    PortCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title = "СРАВНЕНИЕ С УРОВНЕМ")
            ComparisonRow(
                title = "Слов в неделю",
                badge = "Top ${progress.peerWordsRankTopPercent}%",
                youValue = "${progress.wordsPerWeek} сл/нед",
                avgValue = "Среднее B2: ${progress.avgB2WordsPerWeek} сл/нед",
                progress = (progress.wordsPerWeek.toFloat() / progress.avgB2WordsPerWeek.coerceAtLeast(1)).coerceIn(0.3f, 1f),
                color = PpAccent,
            )
            ComparisonRow(
                title = "Retention",
                badge = "Среднее",
                youValue = "Ты: ${progress.retentionPercent}%",
                avgValue = "Среднее B2: 82%",
                progress = (progress.retentionPercent / 100f).coerceIn(0.3f, 1f),
                color = PpInfo,
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    title: String,
    badge: String,
    youValue: String,
    avgValue: String,
    progress: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = PpHeading, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = badge,
                color = PpTextMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(PpSurfaceInput)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = color,
            trackColor = PpSurfaceInput,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(youValue, color = PpText, style = MaterialTheme.typography.labelMedium)
            Text(avgValue, color = PpTextMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}
