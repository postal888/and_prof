package com.proficon.app.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputPath: String? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(prefix: String): String {
        stop()
        val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(dir, "$safePrefix-${System.currentTimeMillis()}.m4a")
        outputPath = file.absolutePath

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(44_100)
        mediaRecorder.setAudioEncodingBitRate(128_000)
        mediaRecorder.setOutputFile(file.absolutePath)
        try {
            mediaRecorder.prepare()
            mediaRecorder.start()
        } catch (e: Exception) {
            mediaRecorder.release()
            recorder = null
            outputPath = null
            throw IllegalStateException("Не удалось начать запись: ${e.message}", e)
        }
        recorder = mediaRecorder
        return file.absolutePath
    }

    fun stop(): String? {
        val path = outputPath
        try {
            recorder?.stop()
        } catch (_: Exception) {
            /* ignore */
        }
        recorder?.release()
        recorder = null
        outputPath = null
        return path
    }
}
