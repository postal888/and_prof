package com.profconq.app.youtube

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class YouTubeNowPlaying(
    val live: Boolean = false,
    val playing: Boolean = false,
    val paused: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val artwork: String? = null,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
)

interface YouTubePlaybackCommands {
    fun play()
    fun pause()
    fun playPause()
    fun seekBy(deltaSec: Float)
    fun seekTo(ms: Long)
    fun stop()
}

object YouTubeNowPlayingHub {
    private val _state = MutableStateFlow(YouTubeNowPlaying())
    val state: StateFlow<YouTubeNowPlaying> = _state.asStateFlow()

    private val _openVideo = MutableStateFlow(false)
    val openVideo: StateFlow<Boolean> = _openVideo.asStateFlow()

    private val _pendingVideoId = MutableStateFlow<String?>(null)
    val pendingVideoId: StateFlow<String?> = _pendingVideoId.asStateFlow()

    @Volatile
    var commands: YouTubePlaybackCommands? = null
        private set

    @Volatile
    private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun bind(commands: YouTubePlaybackCommands) {
        this.commands = commands
    }

    fun unbind(commands: YouTubePlaybackCommands) {
        if (this.commands === commands) this.commands = null
    }

    fun requestOpenVideo() {
        _openVideo.value = true
    }

    fun consumeOpenVideo() {
        _openVideo.value = false
    }

    fun requestOpenVideoId(videoId: String) {
        if (videoId.isBlank()) return
        _pendingVideoId.value = videoId.trim()
        _openVideo.value = true
    }

    fun consumePendingVideoId(): String? {
        val id = _pendingVideoId.value
        _pendingVideoId.value = null
        return id
    }

    fun publish(ui: YouTubeUiState) {
        val videoId = ui.videoId
        val live = videoId != null && (ui.isPlaying || ui.isPaused)
        val durationMs = when {
            ui.durationSec > 0f -> (ui.durationSec * 1000f).toLong()
            else -> parseYouTubeClockToMs(ui.durationLabel)
        }.coerceAtLeast(0L)
        val elapsedMs = (ui.playbackSec * 1000f).toLong().coerceAtLeast(0L)
        val next = YouTubeNowPlaying(
            live = live,
            playing = ui.isPlaying,
            paused = ui.isPaused,
            title = ui.selectedVideoTitle.orEmpty(),
            subtitle = ui.videoChannel,
            artwork = ui.videoThumbnailUrl
                ?: videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" },
            elapsedMs = if (durationMs > 0L) elapsedMs.coerceAtMost(durationMs) else elapsedMs,
            durationMs = durationMs,
        )
        val wasLive = _state.value.live
        _state.value = next
        val ctx = app ?: return
        val intent = Intent(ctx, YouTubePlaybackService::class.java)
        when {
            next.live && !wasLive -> ContextCompat.startForegroundService(ctx, intent)
            !next.live && wasLive -> ctx.stopService(intent)
        }
    }

    fun clear() {
        val wasLive = _state.value.live
        _state.value = YouTubeNowPlaying()
        val ctx = app ?: return
        if (wasLive) ctx.stopService(Intent(ctx, YouTubePlaybackService::class.java))
    }
}

fun parseYouTubeClockToMs(text: String?): Long {
    val value = text?.trim().orEmpty()
    if (value.isEmpty() || value.equals("SHORTS", ignoreCase = true)) return 0L
    val parts = value.split(':')
    if (parts.isEmpty() || parts.any { it.toLongOrNull() == null }) return 0L
    val seconds = when (parts.size) {
        3 -> parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toLong()
        2 -> parts[0].toLong() * 60L + parts[1].toLong()
        1 -> parts[0].toLong()
        else -> return 0L
    }
    return (seconds * 1000L).coerceAtLeast(0L)
}
