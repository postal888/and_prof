package com.proficon.app.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PcmAudio(
    val samples: ShortArray,
    val sampleRate: Int,
    val channelCount: Int,
)

/** Decodes the first audio track of a media file (e.g. M4A/AAC) to interleaved 16-bit PCM. */
object AudioPcmDecoder {
    fun decode(filePath: String): PcmAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)
        var audioTrack = -1
        var inputFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrack = i
                inputFormat = format
                break
            }
        }
        if (audioTrack < 0 || inputFormat == null) {
            extractor.release()
            throw IllegalStateException("В файле нет аудиодорожки")
        }

        extractor.selectTrack(audioTrack)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalStateException("Неизвестный формат аудио")
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val pcmChunks = mutableListOf<ShortArray>()
        var totalSamples = 0

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        try {
            while (true) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (inputDone) break
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outputIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val pcm = bytesToShorts(outputBuffer, bufferInfo.size)
                            pcmChunks += pcm
                            totalSamples += pcm.size
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        if (totalSamples == 0) {
            throw IllegalStateException("Не удалось декодировать аудио")
        }

        val merged = ShortArray(totalSamples)
        var offset = 0
        for (chunk in pcmChunks) {
            chunk.copyInto(merged, offset)
            offset += chunk.size
        }
        return PcmAudio(merged, sampleRate, channelCount)
    }

    private fun bytesToShorts(buffer: ByteBuffer, size: Int): ShortArray {
        val shortCount = size / 2
        val out = ShortArray(shortCount)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < shortCount && buffer.remaining() >= 2) {
            out[i] = buffer.short
            i++
        }
        return out
    }
}
