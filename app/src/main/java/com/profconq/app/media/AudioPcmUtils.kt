package com.profconq.app.media

import kotlin.math.abs
import kotlin.math.sqrt

object AudioPcmUtils {
    const val MIN_TRIM_MS = 300L

    fun durationMs(pcm: PcmAudio): Long {
        val frames = pcm.samples.size / pcm.channelCount.coerceAtLeast(1)
        return (frames * 1000L) / pcm.sampleRate.coerceAtLeast(1)
    }

    fun trim(pcm: PcmAudio, startMs: Long, endMs: Long): PcmAudio {
        val channels = pcm.channelCount.coerceAtLeast(1)
        val sampleRate = pcm.sampleRate.coerceAtLeast(1)
        val totalFrames = pcm.samples.size / channels
        val startFrame = ((startMs * sampleRate) / 1000L).toInt().coerceIn(0, totalFrames)
        val endFrame = ((endMs * sampleRate) / 1000L).toInt().coerceIn(startFrame, totalFrames)
        val startSample = startFrame * channels
        val endSample = endFrame * channels
        return pcm.copy(samples = pcm.samples.copyOfRange(startSample, endSample))
    }

    /** Normalized peak amplitudes in [0, 1] for waveform drawing. */
    fun waveformPeaks(pcm: PcmAudio, barCount: Int = 72): List<Float> {
        if (barCount <= 0) return emptyList()
        val channels = pcm.channelCount.coerceAtLeast(1)
        val frames = pcm.samples.size / channels
        if (frames == 0) return List(barCount) { 0.1f }
        val framesPerBar = (frames / barCount).coerceAtLeast(1)
        val peaks = MutableList(barCount) { 0f }
        var frameIndex = 0
        for (bar in 0 until barCount) {
            var max = 0f
            val barEnd = (frameIndex + framesPerBar).coerceAtMost(frames)
            var f = frameIndex
            while (f < barEnd) {
                var sumSq = 0f
                for (ch in 0 until channels) {
                    val sample = pcm.samples[f * channels + ch] / 32768f
                    sumSq += sample * sample
                }
                val amp = sqrt(sumSq / channels)
                if (amp > max) max = amp
                f++
            }
            peaks[bar] = max.coerceIn(0.05f, 1f)
            frameIndex = barEnd
        }
        return peaks
    }
}
