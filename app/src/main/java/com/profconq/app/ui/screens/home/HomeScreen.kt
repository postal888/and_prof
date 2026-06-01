package com.profconq.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.TodayPlan
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.TodayStatsRow
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted

@Composable
fun HomeScreen(
    todayPlan: TodayPlan,
    collections: List<Collection>,
    onContinueStudy: () -> Unit,
    onOpenTab: (MainTab) -> Unit,
    onOpenDictionary: () -> Unit,
    onDeleteCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasStudyCards = collections.any { it.cards.isNotEmpty() }
    val strings = LocalUiStrings.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
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
            Button(
                onClick = onContinueStudy,
                enabled = hasStudyCards,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PpAccent,
                    disabledContainerColor = PpTextMuted.copy(alpha = 0.2f),
                ),
            ) {
                Text(
                    text = strings.homeContinueReview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        item {
            QuickActionsGrid(
                onOpenTab = onOpenTab,
                onOpenDictionary = onOpenDictionary,
            )
        }

        item {
            SectionTitle(title = strings.homeMyCollections)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                collections.forEach { collection ->
                    CollectionRow(
                        collection = collection,
                        onDelete = { onDeleteCollection(collection.id) },
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun QuickActionsGrid(
    onOpenTab: (MainTab) -> Unit,
    onOpenDictionary: () -> Unit,
) {
    val strings = LocalUiStrings.current
    val actions = listOf(
        strings.tabRead to { onOpenTab(MainTab.Reader) },
        strings.tabVideo to { onOpenTab(MainTab.Practice) },
        strings.tabDictionary to { onOpenTab(MainTab.Dictionary) },
        strings.tabStudy to { onOpenTab(MainTab.Study) },
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
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CollectionRow(
    collection: Collection,
    onDelete: () -> Unit,
) {
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PpHeading,
                )
                if (!collection.description.isNullOrBlank()) {
                    MutedText(
                        text = collection.description,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Text(
                text = collection.cards.size.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = PpAccent,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = "×",
                color = PpDanger,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
