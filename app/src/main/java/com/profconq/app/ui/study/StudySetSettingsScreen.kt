package com.profconq.app.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.OutlinedAccentButton
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted

@Composable
fun StudySetSettingsScreen(
    setId: String,
    repository: ProfconqRepository,
    onBack: () -> Unit,
    onAddFromDictionary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudySetSettingsViewModel = viewModel(
        factory = StudySetSettingsViewModelFactory(repository, setId),
    ),
) {
    val strings = LocalUiStrings.current
    val setName by viewModel.setName.collectAsState()
    val words by viewModel.words.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PpAccent)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp, end = 4.dp),
            ) {
                Text(
                    text = strings.studySetSettingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                )
                if (setName.isNotBlank()) {
                    MutedText(
                        text = setName,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        MutedText(
            text = strings.studySetSettingsHint,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
        )

        OutlinedAccentButton(
            text = strings.studySetSettingsAddFromDictionary,
            onClick = onAddFromDictionary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (words.isEmpty()) {
            PortCard(modifier = Modifier.fillMaxWidth()) {
                MutedText(strings.studySetSettingsEmpty)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(words, key = { it.id }) { word ->
                    StudySetWordRow(
                        word = word,
                        onCheckedChange = { checked ->
                            if (!checked) viewModel.removeWord(word.id)
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StudySetWordRow(
    word: com.profconq.app.data.model.WordCard,
    onCheckedChange: (Boolean) -> Unit,
) {
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = true,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = PpAccent),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(word.pt, color = PpHeading, fontWeight = FontWeight.Medium)
                Text(word.ru, color = PpTextMuted, style = MaterialTheme.typography.bodySmall)
                word.example?.let { ex ->
                    Text(ex, color = PpText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
