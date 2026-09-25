package com.profconq.app.studio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.profconq.app.data.model.WordCard
import com.profconq.app.ui.studio.StudioUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StudioNowPlaying(
    val live: Boolean = false,
    val playing: Boolean = false,
    val paused: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val collection: String = "",
    val artwork: String? = null,
    val index: Int = 0,
    val total: Int = 0,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
)

interface StudioPlaybackCommands {
    fun playPause()
    fun next()
    fun prev()
    fun stop()
}

object StudioNowPlayingHub {
    private val _state = MutableStateFlow(StudioNowPlaying())
    val state: StateFlow<StudioNowPlaying> = _state.asStateFlow()

    private val _openStudio = MutableStateFlow(false)
    val openStudio: StateFlow<Boolean> = _openStudio.asStateFlow()

    @Volatile
    var commands: StudioPlaybackCommands? = null
        private set

    @Volatile
    private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun bind(commands: StudioPlaybackCommands) {
        this.commands = commands
    }

    fun unbind(commands: StudioPlaybackCommands) {
        if (this.commands === commands) this.commands = null
    }

    fun requestOpenStudio() {
        _openStudio.value = true
    }

    fun consumeOpenStudio() {
        _openStudio.value = false
    }

    fun publish(ui: StudioUiState) {
        val card = ui.current
        val dirPt = ui.prefs.dirPtToRu
        val live = ui.playing || (ui.paused && ui.queue.isNotEmpty())
        val next = StudioNowPlaying(
            live = live,
            playing = ui.playing && !ui.paused,
            paused = ui.paused && ui.queue.isNotEmpty(),
            title = card?.let { studioNowPlayingTitle(it, dirPt) }.orEmpty(),
            subtitle = card?.let { studioNowPlayingSubtitle(it, dirPt) }.orEmpty(),
            collection = ui.collectionTitle,
            artwork = card?.displayImage,
            index = if (ui.queue.isEmpty()) 0 else ui.index + 1,
            total = ui.queue.size,
            elapsedMs = ui.cardElapsedMs.coerceAtLeast(0L),
            durationMs = ui.cardDurationMs.coerceAtLeast(0L),
        )
        val wasLive = _state.value.live
        _state.value = next
        val ctx = app ?: return
        val intent = Intent(ctx, StudioPlaybackService::class.java)
        when {
            next.live && !wasLive -> ContextCompat.startForegroundService(ctx, intent)
            !next.live && wasLive -> ctx.stopService(intent)
        }
    }

    fun clear() {
        val wasLive = _state.value.live
        _state.value = StudioNowPlaying()
        val ctx = app ?: return
        if (wasLive) ctx.stopService(Intent(ctx, StudioPlaybackService::class.java))
    }
}

fun studioNowPlayingTitle(card: WordCard, dirPtToRu: Boolean): String {
    if (studioIsPhraseTag(card.partOfSpeech)) {
        val pt = card.example.orEmpty().ifBlank { card.pt }
        val ru = card.exampleTranslation.orEmpty().ifBlank { card.ru }
        return (if (dirPtToRu) pt else ru).trim().ifBlank { "—" }
    }
    val main = if (dirPtToRu) {
        card.pt.ifBlank { card.example.orEmpty() }
    } else {
        card.ru.ifBlank { card.exampleTranslation.orEmpty() }
    }
    return main.trim().ifBlank { "—" }
}

fun studioNowPlayingSubtitle(card: WordCard, dirPtToRu: Boolean): String {
    if (studioIsPhraseTag(card.partOfSpeech)) {
        val pt = card.example.orEmpty().ifBlank { card.pt }
        val ru = card.exampleTranslation.orEmpty().ifBlank { card.ru }
        return (if (dirPtToRu) ru else pt).trim()
    }
    val tr = if (dirPtToRu) {
        card.ru.ifBlank { card.exampleTranslation.orEmpty() }
    } else {
        card.pt.ifBlank { card.example.orEmpty() }
    }
    return tr.trim()
}
