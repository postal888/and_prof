package com.profconq.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.StudySet
import com.profconq.app.data.model.TodayPlan
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.TodayStatsRow
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpGlassBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush

@Composable
fun HomeScreen(
    todayPlan: TodayPlan,
    studySets: List<StudySet>,
    studioCollectionCount: Int,
    onContinueStudy: () -> Unit,
    onOpenTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val studyCards = studySets.sumOf { it.wordCount }
    val tests = studySets.size
    val hasStudyCards = studyCards > 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
        verticalArrangement = Arrangement.spacedBy(PortLayout.HeaderToContent),
    ) {
        item {
            TabScreenHeader(tab = MainTab.Home, subtitle = strings.homeSubtitle)
        }

        item {
            TodayStatsRow(
                dueCount = todayPlan.dueCount,
                newCount = todayPlan.newCount,
                streak = todayPlan.streak,
            )
        }

        item {
            SectionTitle(title = strings.homeLibraryTitle)
            HomeLibraryStats(
                cards = studyCards,
                studio = studioCollectionCount,
                tests = tests,
                onOpenCards = { onOpenTab(MainTab.Study) },
                onOpenStudio = { onOpenTab(MainTab.Studio) },
                onOpenTests = { onOpenTab(MainTab.Study) },
            )
        }

        item {
            GradientPrimaryButton(
                text = strings.homeContinueReview,
                onClick = onContinueStudy,
                enabled = hasStudyCards,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            QuickActionsGrid(onOpenTab = onOpenTab)
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun HomeLibraryStats(
    cards: Int,
    studio: Int,
    tests: Int,
    onOpenCards: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenTests: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeStatTile(
            value = cards,
            label = strings.homeStatCards,
            progress = if (cards > 0) 1f else 0f,
            onClick = onOpenCards,
            modifier = Modifier.weight(1f),
        )
        HomeStatTile(
            value = studio,
            label = strings.homeStatStudio,
            progress = if (studio > 0) 1f else 0f,
            onClick = onOpenStudio,
            modifier = Modifier.weight(1f),
        )
        HomeStatTile(
            value = tests,
            label = strings.homeStatTests,
            progress = if (tests > 0) 1f else 0f,
            onClick = onOpenTests,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeStatTile(
    value: Int,
    label: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PortCard(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PpTextMuted,
        )
        val barProgress = progress.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PpGlassBorder.copy(alpha = 0.35f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(rememberAccentGradientBrush()),
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onOpenTab: (MainTab) -> Unit,
) {
    val strings = LocalUiStrings.current
    val actions = listOf(
        strings.tabStudy to { onOpenTab(MainTab.Study) },
        strings.tabStudio to { onOpenTab(MainTab.Studio) },
        strings.tabRead to { onOpenTab(MainTab.Reader) },
        strings.tabVideo to { onOpenTab(MainTab.Practice) },
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (label, onClick) ->
                    PortCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onClick),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PpHeading,
                        )
                    }
                }
            }
        }
    }
}
