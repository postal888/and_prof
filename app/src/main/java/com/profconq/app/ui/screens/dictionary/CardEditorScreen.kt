package com.profconq.app.ui.screens.dictionary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.profconq.app.media.AudioLabelsCodec
import com.profconq.app.media.AudioPathsCodec
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.WordCard
import com.profconq.app.media.AudioRecorder
import com.profconq.app.media.deleteAudioFile
import com.profconq.app.media.rememberRecordAudioPermission
import com.profconq.app.media.showAudioExportChooser
import com.profconq.app.ui.components.AudioRecordingsList
import com.profconq.app.ui.components.AudioTrimBottomSheet
import com.profconq.app.ui.components.AudioTrimResult
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.components.OverlayBottomBar
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import java.io.File
import java.io.FileOutputStream

@Composable
fun CardEditorScreen(
    collection: Collection?,
    card: WordCard?,
    onBack: () -> Unit,
    onSave: (WordCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    if (collection == null || card == null) {
        Column(modifier = modifier.fillMaxSize().portScreenBackground().padding(PortLayout.Gutter)) {
            Text(strings.cardEditorNotFound, color = PpText)
        }
        return
    }

    var pt by rememberSaveable(card.id) { mutableStateOf(card.pt) }
    var ru by rememberSaveable(card.id) { mutableStateOf(card.ru) }
    var example by rememberSaveable(card.id) { mutableStateOf(card.example.orEmpty()) }
    var exampleRu by rememberSaveable(card.id) { mutableStateOf(card.exampleTranslation.orEmpty()) }
    var imagePath by rememberSaveable(card.id) { mutableStateOf(card.imagePath) }
    val audioPathsSaver = Saver<List<String>, String>(
        save = { paths -> AudioPathsCodec.encode(paths).orEmpty() },
        restore = { encoded -> AudioPathsCodec.decode(encoded.ifBlank { null }) },
    )
    var audioPaths by rememberSaveable(card.id, stateSaver = audioPathsSaver) {
        mutableStateOf(card.resolvedAudioPaths())
    }
    val audioLabelsSaver = Saver<Map<String, String>, String>(
        save = { labels -> AudioLabelsCodec.encode(labels).orEmpty() },
        restore = { encoded -> AudioLabelsCodec.decode(encoded.ifBlank { null }) },
    )
    var audioLabels by rememberSaveable(card.id, stateSaver = audioLabelsSaver) {
        mutableStateOf(card.audioLabels)
    }
    var recording by rememberSaveable(card.id) { mutableStateOf(false) }
    var audioTrimRequest by remember(card.id) { mutableStateOf<Pair<String, Boolean>?>(null) }
    val recorder = remember { AudioRecorder(context) }
    val audioPermission = rememberRecordAudioPermission()
    val studyLangs = LocalStudyLanguagePrefs.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val dest = File(context.filesDir, "images/card-${card.id}-${System.currentTimeMillis()}.jpg")
        dest.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        imagePath = dest.absolutePath
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground(),
    ) {
        TabScreenHeader(
            title = strings.cardEditorTitle,
            subtitle = collection.title,
            onBack = onBack,
            modifier = Modifier.padding(horizontal = PortLayout.Gutter),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PortLayout.Gutter)
                .padding(top = PortLayout.HeaderToContent, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

        field(strings.studyLanguageName(studyLangs.source), pt, multiLine = true) { pt = it }
        field(strings.studyLanguageName(studyLangs.target), ru, multiLine = true) { ru = it }
        field(strings.dictionaryWordExampleLabel, example, multiLine = true) { example = it }
        field(strings.dictionaryWordExampleRuLabel, exampleRu, multiLine = true) { exampleRu = it }

        imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { imagePicker.launch("image/*") }) {
                Text(strings.cardEditorAddImage, color = PpText)
            }
            if (imagePath != null) {
                Button(onClick = { imagePath = null }) {
                    Text(strings.commonRemove, color = PpText)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    audioPermission.runWithPermission {
                        if (recording) {
                            val path = recorder.stop()
                            if (path != null) audioTrimRequest = path to true
                            recording = false
                        } else {
                            try {
                                recorder.start("card-${card.id}")
                                recording = true
                            } catch (_: Exception) {
                                recording = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recording) PpAccent else PpSurfaceInput,
                ),
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(if (recording) strings.dictionaryStop else strings.cardEditorRecordWord, color = PpText, modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (audioPaths.isNotEmpty()) {
            AudioRecordingsList(
                audioPaths = audioPaths,
                titleForPath = { path, index ->
                    card.copy(audioLabels = audioLabels)
                        .audioTitle(path, index, strings::dictionaryCardRecordingDefault)
                },
                onShare = { path -> shareAudio(context, path, strings.audioExportLabels()) },
                onTrim = { path -> audioTrimRequest = path to false },
                onTitleChange = { path, title ->
                    audioLabels = audioLabels.toMutableMap().apply {
                        if (title.isBlank()) remove(path) else put(path, title)
                    }
                },
            )
        }
        }

        OverlayBottomBar {
            GradientPrimaryButton(
                text = strings.save,
                onClick = {
                    onSave(
                        card.copy(
                            pt = pt.trim(),
                            ru = ru.trim(),
                            example = example.trim().ifEmpty { null },
                            exampleTranslation = exampleRu.trim().ifEmpty { null },
                            imagePath = imagePath,
                            audioPaths = audioPaths,
                            audioPath = audioPaths.firstOrNull(),
                            audioLabels = AudioLabelsCodec.retainOnly(audioLabels, audioPaths),
                        ),
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    audioTrimRequest?.let { (trimPath, fresh) ->
        AudioTrimBottomSheet(
            audioPath = trimPath,
            filePrefix = "card-${card.id}",
            onDismiss = {
                if (fresh) {
                    deleteAudioFile(trimPath)
                    audioPaths = audioPaths.filter { it != trimPath }
                    audioLabels = audioLabels.filterKeys { it != trimPath }
                }
                audioTrimRequest = null
            },
            onResult = { result ->
                when (result) {
                    is AudioTrimResult.Saved -> {
                        if (fresh) {
                            if (result.path != trimPath) deleteAudioFile(trimPath)
                            if (result.path !in audioPaths) {
                                audioPaths = audioPaths + result.path
                            }
                        } else {
                            trimPath.let { old ->
                                if (old != result.path) deleteAudioFile(old)
                            }
                            audioPaths = audioPaths.map { if (it == trimPath) result.path else it }
                            audioLabels = audioLabels.toMutableMap().apply {
                                remove(trimPath)?.let { put(result.path, it) }
                            }
                        }
                    }
                    AudioTrimResult.Deleted -> {
                        deleteAudioFile(trimPath)
                        audioPaths = audioPaths.filter { it != trimPath }
                        audioLabels = audioLabels.filterKeys { it != trimPath }
                    }
                }
                audioTrimRequest = null
            },
        )
    }
}

@Composable
private fun field(
    label: String,
    value: String,
    multiLine: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MutedText(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (multiLine) Modifier.heightIn(min = 96.dp, max = 280.dp) else Modifier,
                ),
            minLines = if (multiLine) 3 else 1,
            maxLines = if (multiLine) 12 else 1,
            singleLine = !multiLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (multiLine) ImeAction.Default else ImeAction.Next,
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PpAccent,
                unfocusedBorderColor = PpBorder,
                focusedContainerColor = PpSurfaceInput,
                unfocusedContainerColor = PpSurfaceInput,
                cursorColor = PpAccent,
                focusedTextColor = PpText,
                unfocusedTextColor = PpText,
            ),
        )
    }
}

private fun shareAudio(context: android.content.Context, path: String, labels: com.profconq.app.media.AudioExportLabels) {
    showAudioExportChooser(context, path, labels)
}
