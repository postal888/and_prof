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

data class AudioExportLabels(
    val chooserTitle: String,
    val m4aOption: String,
    val mp3Option: String = "MP3",
    val creatingMp3: String,
    val mp3Failed: (String?) -> String,
)

fun showAudioExportChooser(
    context: Context,
    path: String,
    labels: AudioExportLabels,
) {
    val activity = context.findActivity()
    if (activity == null) {
        shareAudioFile(context, path, AudioShareFormat.M4A, labels.chooserTitle)
        return
    }
    val items = arrayOf(labels.m4aOption, labels.mp3Option)
    android.app.AlertDialog.Builder(activity)
        .setTitle(labels.chooserTitle)
        .setItems(items) { _, which ->
            when (which) {
                0 -> shareAudioFile(context, path, AudioShareFormat.M4A, labels.chooserTitle)
                1 -> exportAndShareMp3(activity, path, labels)
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
    labels: AudioExportLabels,
) {
    val scope = (activity as? androidx.activity.ComponentActivity)?.lifecycleScope
        ?: return shareAudioFile(activity, sourcePath, labels.chooserTitle)

    Toast.makeText(activity, labels.creatingMp3, Toast.LENGTH_SHORT).show()
    scope.launch {
        try {
            val mp3 = withContext(Dispatchers.IO) {
                AudioMp3Exporter.exportToMp3(activity, sourcePath)
            }
            shareAudioFile(activity, mp3.absolutePath, AudioShareFormat.MP3, labels.chooserTitle)
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                labels.mp3Failed(e.message),
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
