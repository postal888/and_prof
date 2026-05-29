package com.proficon.app.media

import android.content.Context
import com.naman14.androidlame.LameBuilder
import java.io.File
import java.io.FileOutputStream

object AudioMp3Exporter {
    private const val BITRATE_KBPS = 128

    fun exportToMp3(context: Context, sourcePath: String): File {
        val source = File(sourcePath)
        require(source.exists()) { "Файл записи не найден" }

        val outDir = File(context.cacheDir, "export").apply { mkdirs() }
        val outFile = File(outDir, "${source.nameWithoutExtension}.mp3")
        if (outFile.exists() && outFile.lastModified() >= source.lastModified()) {
            return outFile
        }

        val pcm = AudioPcmDecoder.decode(sourcePath)
        writeMp3(pcm, outFile)
        return outFile
    }

    private fun writeMp3(pcm: PcmAudio, outFile: File) {
        val channels = pcm.channelCount.coerceIn(1, 2)
        val lame = LameBuilder()
            .setInSampleRate(pcm.sampleRate)
            .setOutChannels(channels)
            .setOutBitrate(BITRATE_KBPS)
            .setOutSampleRate(pcm.sampleRate)
            .build()

        val mp3Buffer = ByteArray(4096)
        FileOutputStream(outFile).use { output ->
            val frameCount = pcm.samples.size / channels
            var offset = 0
            while (offset < frameCount) {
                val chunkFrames = minOf(1152, frameCount - offset)
                val start = offset * channels
                val end = start + chunkFrames * channels
                val chunk = pcm.samples.copyOfRange(start, end)
                val encoded = lame.encodeBufferInterLeaved(chunk, chunkFrames, mp3Buffer)
                if (encoded > 0) {
                    output.write(mp3Buffer, 0, encoded)
                }
                offset += chunkFrames
            }
            val flush = lame.flush(mp3Buffer)
            if (flush > 0) {
                output.write(mp3Buffer, 0, flush)
            }
        }
        lame.close()
    }
}
