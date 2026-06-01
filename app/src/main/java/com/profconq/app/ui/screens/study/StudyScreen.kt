package com.profconq.app.ui.screens.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.StudySet
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudyScreen(
    studySets: List<StudySet>,
    onCreateSet: () -> Unit,
    onOpenSet: (String) -> Unit,
    onOpenSetSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            TabScreenHeader(tab = MainTab.Study, subtitle = strings.studyScreenSubtitle)
        }

        item {
            OutlinedButton(
                onClick = onCreateSet,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, PpAccent),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PpSurfaceInput,
                    contentColor = PpAccent,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PpAccent)
                Text(
                    strings.studyCreateSet,
                    color = PpAccent,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        item {
            SectionTitle(title = strings.studyCollectionsSection)
            if (studySets.isEmpty()) {
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    MutedText(strings.studyCreateSetFromDictionary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    studySets.forEach { set ->
                        StudySetRow(
                            set = set,
                            onClick = { onOpenSet(set.id) },
                            onOpenSettings = { onOpenSetSettings(set.id) },
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StudySetRow(
    set: StudySet,
    onClick: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dateLabel = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(set.createdAt))
    val strings = LocalUiStrings.current
    val canStart = set.canStartSession
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                ) {
                    Text(
                        text = set.name,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    MutedText("$dateLabel · ${set.wordCount} слов")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.studySetSettingsGear,
                        tint = PpTextMuted,
                    )
                }
                Text(
                    text = if (canStart) "▶" else "↻",
                    color = PpAccent,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onClick),
                )
            }
            Column(modifier = Modifier.clickable(onClick = onClick)) {
                Text(
                    text = "🆕 ${set.newCount} новых  ·  🔁 ${set.dueCount} повтор  ·  ✓ ${set.masteredCount} осв.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PpTextMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = if (canStart) strings.studyStartSession else strings.studyRepeatSet,
                    style = MaterialTheme.typography.labelMedium,
                    color = PpAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (!canStart && set.wordCount > 0) {
                    Text(
                        text = strings.studyDoneTodayHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = PpTextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
