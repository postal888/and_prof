package com.profconq.app.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.profconq.app.ProfconqApplication
import com.profconq.app.data.model.DailyStudyActivity
import com.profconq.app.data.model.StudySet
import com.profconq.app.progress.ProgressTracker
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortCompactHeight
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.portClickable
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

@Composable
fun StudyScreen(
    studySets: List<StudySet>,
    dailyActivities: List<DailyStudyActivity>,
    onCreateSet: () -> Unit,
    onOpenStudio: (String) -> Unit,
    onOpenCards: (String) -> Unit,
    onOpenTests: (String) -> Unit,
    onOpenSetSettings: (String) -> Unit,
    onRenameSet: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val resultStore = (LocalContext.current.applicationContext as ProfconqApplication).studyTestResultStore
    val intensityStore = (LocalContext.current.applicationContext as ProfconqApplication).studyIntensityStore
    val lastTest by resultStore.last.collectAsState()
    val dayBuckets by resultStore.days.collectAsState()
    val intensity by intensityStore.snapshot.collectAsState()
    var pendingDelete by remember { mutableStateOf<StudySet?>(null) }
    var renameTarget by remember { mutableStateOf<StudySet?>(null) }
    var showLastTestWords by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }
    var selectedChartDay by remember { mutableStateOf(ProgressTracker.todayKey()) }
    var selectedWordId by remember { mutableStateOf<String?>(null) }

    val selectedWord = selectedWordId
    if (selectedWord != null) {
        WordIntensityScreen(
            wordId = selectedWord,
            snapshot = intensity,
            onBack = { selectedWordId = null },
            modifier = modifier,
        )
        return
    }

    val selectedDay = selectedDayKey
    if (selectedDay != null) {
        StudyDayStatsScreen(
            stats = mergeStudyDayStats(selectedDay, dailyActivities, dayBuckets),
            onBack = { selectedDayKey = null },
            modifier = modifier,
        )
        return
    }

    if (showLastTestWords && lastTest != null) {
        LastTestWordsScreen(
            result = lastTest!!,
            onBack = { showLastTestWords = false },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
        verticalArrangement = Arrangement.spacedBy(PortLayout.HeaderToContent),
    ) {
        item {
            TabScreenHeader(tab = MainTab.Study, subtitle = strings.studyScreenSubtitle)
        }

        item {
            GradientPrimaryButton(
                text = strings.studyCreateSet,
                onClick = onCreateSet,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PpBrandNavy,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }

        item {
            SectionTitle(title = strings.studyCollectionsSection)
            if (studySets.isEmpty()) {
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    MutedText(strings.studyCreateSetFromDictionary)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    studySets.forEach { set ->
                        StudySetRow(
                            set = set,
                            onRename = { renameTarget = set },
                            onOpenStudio = { onOpenStudio(set.id) },
                            onOpenCards = { onOpenCards(set.id) },
                            onOpenTests = { onOpenTests(set.id) },
                            onOpenSettings = { onOpenSetSettings(set.id) },
                            onDelete = { pendingDelete = set },
                        )
                    }
                }
            }
        }

        lastTest?.let { result ->
            item {
                SectionTitle(title = strings.studyLastTestTitle)
                LastTestHeroCard(
                    result = result,
                    onOpen = { showLastTestWords = true },
                )
            }
        }

        item {
            SectionTitle(title = strings.studyCalendarTitle)
            StudyActivityCalendar(
                dailyActivities = dailyActivities,
                dayBuckets = dayBuckets,
                extraActiveDays = intensity.activeDays,
                month = calendarMonth,
                onMonthChange = { calendarMonth = it },
                onOpenDay = {
                    selectedChartDay = it
                    selectedDayKey = it
                },
            )
        }

        studyIntensitySection(
            snapshot = intensity,
            selectedDateKey = selectedChartDay,
            onSelectDay = { selectedChartDay = it },
            onOpenWord = { selectedWordId = it },
        )

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    pendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.studyDeleteSetTitle, color = PpHeading) },
            text = { Text(strings.studyDeleteSetMessage(set.name), color = PpText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resultStore.clearIfSet(set.id)
                        onDeleteSet(set.id)
                        pendingDelete = null
                    },
                ) {
                    Text(strings.delete, color = PpDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel, color = PpTextMuted)
                }
            },
            containerColor = PpSurface,
        )
    }

    renameTarget?.let { set ->
        var draft by remember(set.id) { mutableStateOf(set.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.studyRenameSetTitle, color = PpHeading) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PpText,
                        unfocusedTextColor = PpText,
                        cursorColor = PpAccent,
                        focusedBorderColor = PpAccent,
                        unfocusedBorderColor = PpTextMuted,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = draft.trim()
                        if (name.isNotEmpty()) onRenameSet(set.id, name)
                        renameTarget = null
                    },
                    enabled = draft.trim().isNotEmpty(),
                ) {
                    Text(strings.save, color = PpAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(strings.cancel, color = PpTextMuted)
                }
            },
            containerColor = PpSurface,
        )
    }
}

@Composable
private fun StudySetRow(
    set: StudySet,
    onRename: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenTests: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateLabel = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(set.createdAt))
    val strings = LocalUiStrings.current
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = set.name,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRename),
                    )
                    MutedText("$dateLabel · ${strings.studyWordsCount(set.wordCount)}")
                    Text(
                        text = "🆕 ${set.newCount}  ·  🔁 ${set.dueCount}  ·  ✓ ${set.masteredCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PpTextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.studySetSettingsGear,
                        tint = PpTextMuted,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = PpDanger,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeChip(
                    count = set.studioCount,
                    label = strings.studyModeStudio,
                    onClick = onOpenStudio,
                    modifier = Modifier.weight(1f),
                )
                ModeChip(
                    count = set.cardsCount,
                    label = strings.studyModeCards,
                    onClick = onOpenCards,
                    modifier = Modifier.weight(1f),
                )
                ModeChip(
                    count = set.testCount,
                    label = strings.studyModeTest,
                    onClick = onOpenTests,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = count > 0
    val shape = RoundedCornerShape(12.dp)
    val gradient = rememberAccentGradientBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (enabled) PpHeading else PpTextMuted,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PortCompactHeight)
                .clip(shape)
                .then(
                    if (enabled) {
                        Modifier.background(gradient, shape)
                    } else {
                        Modifier.border(1.dp, PpAccent.copy(alpha = 0.25f), shape)
                    },
                )
                .portClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) PpBrandNavy else PpTextMuted,
            )
        }
    }
}
