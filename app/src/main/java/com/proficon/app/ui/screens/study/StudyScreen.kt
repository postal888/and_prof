package com.proficon.app.ui.screens.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.WordCard
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.PortCard
import com.proficon.app.ui.components.ScreenHeader
import com.proficon.app.ui.components.SectionTitle
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurface
import com.proficon.app.ui.theme.PpTextMuted

@Composable
fun StudyScreen(
    collections: List<Collection>,
    onMarkKnown: (String) -> Unit,
    onMarkRepeat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCollectionId by remember(collections) {
        mutableStateOf(collections.firstOrNull()?.id.orEmpty())
    }
    val selected = collections.find { it.id == selectedCollectionId }
    val dueCards = selected?.cards?.filter { it.due }.orEmpty()
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
            ScreenHeader(title = strings.tabStudy, subtitle = strings.studyScreenSubtitle)
        }

        item {
            SectionTitle(title = strings.studyCollectionsSection)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                collections.forEach { collection ->
                    val selectedItem = collection.id == selectedCollectionId
                    PortCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCollectionId = collection.id },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = collection.title,
                                    color = if (selectedItem) PpAccent else PpHeading,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                MutedText(strings.studyWordsCount(collection.cards.size))
                            }
                            Text(
                                text = strings.studyDueCount(collection.cards.count { it.due }),
                                color = PpTextMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        item {
            if (dueCards.isEmpty()) {
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    MutedText(
                        text = if (selected == null) {
                            strings.studyEmptyNoCollection
                        } else {
                            strings.studyEmptyNoDue
                        },
                    )
                }
            } else {
                StudySession(
                    cards = dueCards,
                    onMarkKnown = onMarkKnown,
                    onMarkRepeat = onMarkRepeat,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StudySession(
    cards: List<WordCard>,
    onMarkKnown: (String) -> Unit,
    onMarkRepeat: (String) -> Unit,
) {
    var index by remember(cards) { mutableIntStateOf(0) }
    var flipped by remember(cards) { mutableStateOf(false) }
    val card = cards.getOrNull(index)
    val strings = LocalUiStrings.current

    if (card == null) {
        PortCard(modifier = Modifier.fillMaxWidth()) {
            Text(strings.studyAllDone, color = PpHeading, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "${index + 1} / ${cards.size}",
            color = PpTextMuted,
            style = MaterialTheme.typography.labelMedium,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PpSurface)
                .clickable { flipped = !flipped }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            if (rotation <= 90f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.pt, style = MaterialTheme.typography.headlineMedium, color = PpHeading)
                    card.example?.let {
                        MutedText(it, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                ) {
                    Text(card.ru, style = MaterialTheme.typography.headlineMedium, color = PpAccent)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    onMarkRepeat(card.id)
                    flipped = false
                    index = (index + 1).coerceAtMost(cards.lastIndex)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PpSurface),
            ) {
                Text(strings.studyAgain, color = PpHeading)
            }
            Button(
                onClick = {
                    onMarkKnown(card.id)
                    flipped = false
                    if (index < cards.lastIndex) index++ else index = cards.lastIndex
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            ) {
                Text(strings.studyKnown, color = PpHeading)
            }
        }
    }
}
