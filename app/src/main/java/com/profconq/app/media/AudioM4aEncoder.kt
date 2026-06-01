package com.profconq.app.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Encodes 16-bit interleaved PCM to M4A (AAC). */
object AudioM4aEncoder {
    private const val BIT_RATE = 128_000
    private const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC

    fun encodeToFile(pcm: PcmAudio, outputPath: String) {
        val channelCount = pcm.channelCount.coerceIn(1, 2)
        val sampleRate = pcm.sampleRate.coerceAtLeast(8_000)
        val mime = MIME
        val format = MediaFormat.createAudioFormat(mime, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerTrack = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var sampleIndex = 0
        val frameSize = channelCount
        val totalFrames = pcm.samples.size / frameSize

        try {
            while (true) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        inputBuffer.clear()
                        val capacityFrames = inputBuffer.remaining() / (2 * frameSize)
                        val framesToWrite = minOf(capacityFrames, totalFrames - sampleIndex)
                        if (framesToWrite <= 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            val byteCount = framesToWrite * frameSize * 2
                            val shortView = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val start = sampleIndex * frameSize
                            shortView.put(pcm.samples, start, framesToWrite * frameSize)
                            val presentationTimeUs = sampleIndex * 1_000_000L / sampleRate
                            codec.queueInputBuffer(inputIndex, 0, byteCount, presentationTimeUs, 0)
                            sampleIndex += framesToWrite
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (inputDone) break
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) continue
                        muxerTrack = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        if (!muxerStarted) {
                            muxerTrack = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            muxer.writeSampleData(muxerTrack, outputBuffer, bufferInfo)
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
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
        }
    }
}
