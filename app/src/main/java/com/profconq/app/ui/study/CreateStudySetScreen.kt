package com.profconq.app.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.OverlayBottomBar
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateStudySetScreen(
    repository: ProfconqRepository,
    selectionStore: StudyWordSelectionStore,
    onBack: () -> Unit,
    onSelectWords: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val defaultSetName = remember(strings) {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        strings.defaultStudySetName(formatter.format(Date()))
    }
    val viewModel: CreateStudySetViewModel = viewModel(
        factory = CreateStudySetViewModelFactory(
            repository = repository,
            selectionStore = selectionStore,
            initialName = defaultSetName,
        ),
    )
    val state by viewModel.state.collectAsState()
    val canCreate = state.selectedWords.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground(),
    ) {
        TabScreenHeader(
            title = strings.studyNewSetTitle,
            subtitle = strings.tabStudy,
            onBack = onBack,
            modifier = Modifier.padding(horizontal = PortLayout.Gutter),
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PortLayout.Gutter)
                .padding(top = PortLayout.HeaderToContent),
            label = { Text(strings.dictionaryFolderNameLabel) },
            singleLine = true,
            colors = fieldColors(),
        )

        OutlinedButton(
            onClick = onSelectWords,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PortLayout.Gutter)
                .padding(top = 12.dp),
        ) {
            Text(strings.studySelectWords, color = PpAccent)
        }

        Text(
            text = strings.studySelectedWordsCount(state.selectedWords.size),
            style = MaterialTheme.typography.labelMedium,
            color = PpAccent,
            modifier = Modifier.padding(
                start = PortLayout.Gutter,
                end = PortLayout.Gutter,
                top = 12.dp,
                bottom = 8.dp,
            ),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = PortLayout.Gutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.selectedWords, key = { it.id }) { word ->
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(word.pt, color = PpHeading, style = MaterialTheme.typography.bodyLarge)
                            Text(word.ru, color = PpTextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.removeWord(word.id) }) {
                            Icon(Icons.Default.Close, contentDescription = strings.commonRemove, tint = PpTextMuted)
                        }
                    }
                }
            }
        }

        OverlayBottomBar {
            GradientPrimaryButton(
                text = strings.studyConfirmCreate,
                onClick = { viewModel.createSet(onCreated) },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PpAccent,
    unfocusedBorderColor = PpBorder,
    focusedContainerColor = PpSurfaceInput,
    unfocusedContainerColor = PpSurfaceInput,
    cursorColor = PpAccent,
    focusedTextColor = PpText,
    unfocusedTextColor = PpText,
)
