package com.proficon.app.ui.screens.dictionary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.WordCard
import com.proficon.app.media.AudioRecorder
import com.proficon.app.media.rememberRecordAudioPermission
import com.proficon.app.media.showAudioExportChooser
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.ProficonLogo
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.theme.PpBorder
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
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
    if (collection == null || card == null) {
        Column(modifier = modifier.fillMaxSize().portScreenBackground().padding(14.dp)) {
            Text("Карточка не найдена", color = PpText)
        }
        return
    }

    var pt by rememberSaveable(card.id) { mutableStateOf(card.pt) }
    var ru by rememberSaveable(card.id) { mutableStateOf(card.ru) }
    var example by rememberSaveable(card.id) { mutableStateOf(card.example.orEmpty()) }
    var imagePath by rememberSaveable(card.id) { mutableStateOf(card.imagePath) }
    var audioPath by rememberSaveable(card.id) { mutableStateOf(card.audioPath) }
    var recording by rememberSaveable(card.id) { mutableStateOf(false) }
    val recorder = remember { AudioRecorder(context) }
    val audioPermission = rememberRecordAudioPermission()

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
            .portScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = PpText)
            }
            ProficonLogo(size = 36.dp)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text("Карточка", style = MaterialTheme.typography.titleLarge, color = PpHeading)
                MutedText(collection.title)
            }
        }

        field("PT слово", pt) { pt = it }
        field("RU перевод", ru) { ru = it }
        field("Пример", example, multiLine = true) { example = it }

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
                Text("Добавить картинку", color = PpText)
            }
            if (imagePath != null) {
                Button(onClick = { imagePath = null }) {
                    Text("Убрать", color = PpText)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    audioPermission.runWithPermission {
                        if (recording) {
                            audioPath = recorder.stop()
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
                Text(if (recording) "Стоп" else "Запись слова", color = PpText, modifier = Modifier.padding(start = 6.dp))
            }
            audioPath?.let { path ->
                Button(onClick = { shareAudio(context, path) }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Экспорт", color = PpText, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(
                    card.copy(
                        pt = pt.trim(),
                        ru = ru.trim(),
                        example = example.trim().ifEmpty { null },
                        imagePath = imagePath,
                        audioPath = audioPath,
                    ),
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
        ) {
            Text("Сохранить", color = PpHeading)
        }
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
            modifier = Modifier.fillMaxWidth(),
            minLines = if (multiLine) 2 else 1,
            maxLines = if (multiLine) 4 else 1,
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

private fun shareAudio(context: android.content.Context, path: String) {
    showAudioExportChooser(context, path)
}
