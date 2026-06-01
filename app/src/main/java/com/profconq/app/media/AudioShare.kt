package com.profconq.app.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AudioShareFormat {
    M4A,
    MP3,
}

fun showAudioExportChooser(
    context: Context,
    path: String,
    chooserTitle: String = "Экспорт аудио",
) {
    val activity = context.findActivity()
    if (activity == null) {
        shareAudioFile(context, path, AudioShareFormat.M4A, chooserTitle)
        return
    }
    val items = arrayOf("M4A (оригинал)", "MP3")
    android.app.AlertDialog.Builder(activity)
        .setTitle(chooserTitle)
        .setItems(items) { _, which ->
            when (which) {
                0 -> shareAudioFile(context, path, AudioShareFormat.M4A, chooserTitle)
                1 -> exportAndShareMp3(activity, path, chooserTitle)
            }
        }
        .show()
}

fun shareAudioFile(
    context: Context,
    path: String,
    title: String = "Profconq audio",
) {
    shareAudioFile(context, path, AudioShareFormat.M4A, title)
}

fun shareAudioFile(
    context: Context,
    path: String,
    format: AudioShareFormat,
    title: String = "Profconq audio",
) {
    val file = when (format) {
        AudioShareFormat.M4A -> File(path)
        AudioShareFormat.MP3 -> File(path)
    }
    if (!file.exists()) return
    val mime = when (format) {
        AudioShareFormat.M4A -> "audio/mp4"
        AudioShareFormat.MP3 -> "audio/mpeg"
    }
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun exportAndShareMp3(
    activity: Activity,
    sourcePath: String,
    chooserTitle: String,
) {
    val scope = (activity as? androidx.activity.ComponentActivity)?.lifecycleScope
        ?: return shareAudioFile(activity, sourcePath, chooserTitle)

    Toast.makeText(activity, "Создание MP3…", Toast.LENGTH_SHORT).show()
    scope.launch {
        try {
            val mp3 = withContext(Dispatchers.IO) {
                AudioMp3Exporter.exportToMp3(activity, sourcePath)
            }
            shareAudioFile(activity, mp3.absolutePath, AudioShareFormat.MP3, chooserTitle)
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Не удалось создать MP3: ${e.message ?: "ошибка"}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
