package com.profconq.app.media

import android.content.Context
import java.io.File

object AudioTrimExporter {
    fun trimAndSave(
        context: Context,
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        outputPrefix: String,
    ): String {
        val pcm = AudioPcmDecoder.decode(sourcePath)
        val duration = AudioPcmUtils.durationMs(pcm)
        val start = startMs.coerceIn(0L, duration)
        val end = endMs.coerceIn(start + AudioPcmUtils.MIN_TRIM_MS, duration)
        val trimmed = AudioPcmUtils.trim(pcm, start, end)

        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val safePrefix = outputPrefix.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        val outFile = File(dir, "$safePrefix-trim-${System.currentTimeMillis()}.m4a")
        AudioM4aEncoder.encodeToFile(trimmed, outFile.absolutePath)
        return outFile.absolutePath
    }
}

fun deleteAudioFile(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching { File(path).delete() }
}
