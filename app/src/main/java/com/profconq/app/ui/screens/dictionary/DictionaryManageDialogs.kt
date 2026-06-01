package com.profconq.app.ui.screens.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.Collection
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun FolderEditDialog(
    collection: Collection?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?) -> Unit,
) {
    val strings = LocalUiStrings.current
    val isEdit = collection != null
    var title by rememberSaveable(collection?.id) { mutableStateOf(collection?.title.orEmpty()) }
    var description by rememberSaveable(collection?.id) { mutableStateOf(collection?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) strings.dictionaryEditFolderTitle else strings.dictionaryNewFolderTitle,
                color = PpHeading,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.dictionaryFolderNameLabel) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.dictionaryFolderDescLabel) },
                    singleLine = false,
                    minLines = 2,
                    colors = dialogFieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        description.trim().ifEmpty { null },
                    )
                    onDismiss()
                },
                enabled = title.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            ) {
                Text(if (isEdit) strings.save else strings.dictionaryCreate, color = PpHeading)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}

@Composable
fun AddWordDialog(
    collectionTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (pt: String, ru: String, example: String?) -> Unit,
) {
    val strings = LocalUiStrings.current
    val studyLangs = LocalStudyLanguagePrefs.current
    var pt by rememberSaveable { mutableStateOf("") }
    var ru by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dictionaryAddWordTitle, color = PpHeading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(collectionTitle, color = PpTextMuted)
                OutlinedTextField(
                    value = pt,
                    onValueChange = { pt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.studyLanguageName(studyLangs.source)) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                )
                OutlinedTextField(
                    value = ru,
                    onValueChange = { ru = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.studyLanguageName(studyLangs.target)) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.dictionaryWordExampleLabel) },
                    minLines = 2,
                    colors = dialogFieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        pt,
                        ru,
                        example.trim().ifEmpty { null },
                    )
                    onDismiss()
                },
                enabled = pt.trim().isNotEmpty() && ru.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            ) {
                Text(strings.dictionaryAddWord, color = PpHeading)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}

@Composable
fun DeleteFolderDialog(
    collection: Collection,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalUiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dictionaryDeleteFolderTitle, color = PpHeading) },
        text = {
            Text(
                strings.dictionaryDeleteFolderMessage(collection.title, collection.cards.size),
                color = PpText,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(strings.delete, color = PpAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PpAccent,
    unfocusedBorderColor = PpBorder,
    focusedContainerColor = PpSurfaceInput,
    unfocusedContainerColor = PpSurfaceInput,
    cursorColor = PpAccent,
    focusedTextColor = PpText,
    unfocusedTextColor = PpText,
)
