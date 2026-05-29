package com.proficon.app.media

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.concurrent.TimeUnit

class AudioPlayerController {
    private var mediaPlayer: MediaPlayer? = null
    private var loadedPath: String? = null

    var hasAudio by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var durationMs by mutableIntStateOf(0)
        private set
    var positionMs by mutableIntStateOf(0)
        private set

    fun load(path: String?) {
        release()
        if (path.isNullOrBlank()) {
            hasAudio = false
            return
        }
        val file = File(path)
        if (!file.exists()) {
            hasAudio = false
            return
        }
        try {
            val player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { onPlaybackCompleted() }
            }
            mediaPlayer = player
            loadedPath = path
            durationMs = player.duration.coerceAtLeast(0)
            positionMs = 0
            hasAudio = true
        } catch (_: Exception) {
            hasAudio = false
        }
    }

    private fun onPlaybackCompleted() {
        isPlaying = false
        positionMs = durationMs
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (isPlaying) {
            player.pause()
            isPlaying = false
            positionMs = player.currentPosition
        } else {
            player.start()
            isPlaying = true
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.pause()
                player.seekTo(0)
            } catch (_: Exception) {
                /* ignore */
            }
        }
        isPlaying = false
        positionMs = 0
    }

    fun seekTo(ms: Int) {
        val player = mediaPlayer ?: return
        val target = ms.coerceIn(0, durationMs.coerceAtLeast(0))
        player.seekTo(target)
        positionMs = target
    }

    fun refreshPosition() {
        val player = mediaPlayer ?: return
        if (isPlaying) {
            positionMs = player.currentPosition.coerceAtMost(durationMs)
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
            /* ignore */
        }
        mediaPlayer = null
        loadedPath = null
        isPlaying = false
        hasAudio = false
        durationMs = 0
        positionMs = 0
    }
}

fun formatAudioTime(ms: Int): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
