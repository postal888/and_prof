package com.profconq.app.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.OutlinedAccentButton
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.VocabLampsRow
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.data.model.VocabLearnMark
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted

@Composable
fun StudySetSettingsScreen(
    setId: String,
    repository: ProfconqRepository,
    onBack: () -> Unit,
    onAddFromDictionary: () -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: (collectionId: String, cardId: String) -> Unit,
    onDeleteSet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudySetSettingsViewModel = viewModel(
        factory = StudySetSettingsViewModelFactory(repository, setId),
    ),
) {
    val strings = LocalUiStrings.current
    val setName by viewModel.setName.collectAsState()
    val words by viewModel.words.collectAsState()
    var pendingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
    ) {
        TabScreenHeader(
            title = setName.ifBlank { strings.studySetSettingsTitle },
            subtitle = strings.studyWordsCount(words.size),
            onBack = onBack,
        )

        OutlinedAccentButton(
            text = strings.studySetSettingsAddFromDictionary,
            onClick = onAddFromDictionary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PortLayout.HeaderToContent, bottom = 8.dp),
        )
        TextButton(
            onClick = { pendingDelete = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = PpDanger,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.delete, color = PpDanger)
        }

        if (words.isEmpty()) {
            PortCard(modifier = Modifier.fillMaxWidth()) {
                MutedText(strings.studySetSettingsEmpty)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(words, key = { _, word -> word.id }) { index, word ->
                    DeckDictionaryCard(
                        index = index + 1,
                        card = word,
                        onRemoveFromDeck = { viewModel.removeWord(word.id) },
                        onSetLearnMark = onSetLearnMark,
                        onOpenCardEditor = {
                            onOpenCardEditor(word.collectionId, word.id)
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(strings.studyDeleteSetTitle, color = PpHeading) },
            text = {
                Text(
                    strings.studyDeleteSetMessage(setName.ifBlank { strings.studySetSettingsTitle }),
                    color = PpText,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = false
                        onDeleteSet()
                    },
                ) {
                    Text(strings.delete, color = PpDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) {
                    Text(strings.cancel, color = PpTextMuted)
                }
            },
            containerColor = PpSurface,
        )
    }
}

@Composable
private fun DeckDictionaryCard(
    index: Int,
    card: WordCard,
    onRemoveFromDeck: () -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: () -> Unit,
) {
    val strings = LocalUiStrings.current
    val tag = card.partOfSpeech?.trim().orEmpty()
    val example = card.example.orEmpty()
    val exampleRu = card.exampleTranslation.orEmpty()

    PortCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeckCardIndex(
                index = index,
                onRemove = onRemoveFromDeck,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCardEditor),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeckField(
                        text = card.pt,
                        placeholder = strings.dictionaryWordPtLabel,
                        emphasized = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (tag.isNotEmpty()) {
                        Text(
                            text = tag,
                            color = PpHeading,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(PpAccent.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                DeckField(
                    text = card.ru,
                    placeholder = strings.dictionaryWordRuLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCardEditor),
                )
                DeckField(
                    text = example,
                    placeholder = strings.dictionaryWordExampleLabel,
                    italic = true,
                    tinted = example.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCardEditor),
                )
                DeckField(
                    text = exampleRu,
                    placeholder = strings.dictionaryWordExampleRuLabel,
                    italic = true,
                    muted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCardEditor),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VocabLampsRow(
                        level = VocabLearnMark.level(card.learnMark, card.known, card.due),
                        onSelect = { onSetLearnMark(card.id, it.toString()) },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenCardEditor, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = strings.dictionaryCardEditor,
                            tint = PpTextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCardIndex(
    index: Int,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(32.dp)
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onRemove)
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = true,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = PpAccent,
                    uncheckedColor = PpTextMuted,
                    checkmarkColor = PpBrandNavy,
                ),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = index.toString(),
            color = PpTextMuted,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeckField(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    italic: Boolean = false,
    tinted: Boolean = false,
    muted: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    val border = if (tinted) PpNeonGreen.copy(alpha = 0.28f) else PpBorder
    val bg = if (tinted) PpNeonGreen.copy(alpha = 0.08f) else PpSurfaceInput.copy(alpha = 0.55f)
    Text(
        text = text.ifBlank { placeholder },
        color = when {
            text.isBlank() -> PpTextMuted.copy(alpha = 0.55f)
            muted -> PpTextMuted
            emphasized -> PpHeading
            else -> PpText
        },
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        style = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

