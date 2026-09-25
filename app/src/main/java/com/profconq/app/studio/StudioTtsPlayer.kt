package com.profconq.app.studio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.profconq.app.api.ProfconqApiClient
import com.profconq.app.data.model.WordCard
import com.profconq.app.media.AudioMp3Exporter
import com.profconq.app.media.AudioPcmDecoder
import com.profconq.app.media.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class StudioTtsPlayer(
    context: Context,
    private val apiClient: ProfconqApiClient? = null,
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "studio_tts").apply { mkdirs() }
    private val cardsDir = File(appContext.filesDir, "studio_cards").apply { mkdirs() }
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val ready = AtomicBoolean(false)
    private var initStarted = false
    private val generation = AtomicInteger(0)
    private val playbackPaused = AtomicBoolean(false)

    @Synchronized
    fun ensureStarted() {
        if (initStarted) return
        initStarted = true
        tts = TextToSpeech(appContext) { status ->
            ready.set(status == TextToSpeech.SUCCESS)
        }
    }

    fun pausePlayback() {
        playbackPaused.set(true)
        runCatching {
            val player = mediaPlayer ?: return
            if (player.isPlaying) player.pause()
        }
    }

    fun resumePlayback() {
        playbackPaused.set(false)
        runCatching {
            val player = mediaPlayer ?: return
            if (!player.isPlaying) player.start()
        }
    }

    fun stop() {
        playbackPaused.set(false)
        generation.incrementAndGet()
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.reset() }
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stop()
        runCatching {
            mediaPlayer?.release()
            tts?.shutdown()
        }
        mediaPlayer = null
        tts = null
        ready.set(false)
        initStarted = false
    }

    suspend fun playMp3(file: File): Boolean {
        val gen = generation.get()
        if (!file.isUsable()) return false
        return playFile(file, 1f, gen)
    }

    suspend fun speak(text: String, lang: String, speed: Float = 1f, voiceKey: String = StudioVoice.Antoni.key): Boolean {
        val gen = generation.get()
        val trimmed = studioTtsNormText(text)
        if (trimmed.isBlank()) return true
        val resolvedLang = studioTtsResolveLang(trimmed, lang)
        val voice = StudioVoice.fromKey(voiceKey)
        val rate = speed.coerceIn(0.5f, 2f)
        return playCached(trimmed, resolvedLang, voice, rate, gen)
    }

    suspend fun ensureClip(
        text: String,
        lang: String,
        voiceKey: String,
        force: Boolean = false,
    ): File? {
        val trimmed = studioTtsNormText(text)
        if (trimmed.isBlank()) return null
        val resolvedLang = studioTtsResolveLang(trimmed, lang)
        val voice = StudioVoice.fromKey(voiceKey)
        val file = cachedFile(voice.elevenLabsId, resolvedLang, trimmed)
        if (!force && file.isUsable()) return file
        val api = apiClient ?: return file.takeIf { it.isUsable() }
        val bytes = runCatching { api.fetchStudioTts(trimmed, resolvedLang, voice.elevenLabsId, force) }.getOrNull()
            ?: return file.takeIf { it.isUsable() }
        if (bytes.size < 200) return file.takeIf { it.isUsable() }
        writeCache(file, bytes)
        return file.takeIf { it.isUsable() }
    }

    fun hasClip(text: String, lang: String, voiceKey: String): Boolean {
        val trimmed = studioTtsNormText(text)
        if (trimmed.isBlank()) return true
        val resolvedLang = studioTtsResolveLang(trimmed, lang)
        val voice = StudioVoice.fromKey(voiceKey)
        return cachedFile(voice.elevenLabsId, resolvedLang, trimmed).isUsable()
    }

    fun clipsReady(cards: List<WordCard>, prefs: StudioPrefs): Boolean {
        val voiceKey = prefs.voice
        return studioCollectionAudioJobs(cards, prefs).all { hasClip(it.text, it.lang, voiceKey) }
    }

    fun cardMp3sReady(collectionId: String, hash: String, count: Int): Boolean {
        if (hash.isBlank() || count <= 0) return false
        return (0 until count).all { cardMp3File(collectionId, hash, it).isUsable() }
    }

    fun cardPlayDurationMs(card: WordCard, cardNum: Int, prefs: StudioPrefs): Long {
        val stages = buildStudioStages(card, cardNum, prefs)
        if (stages.isEmpty()) return 0L
        val voice = StudioVoice.fromKey(prefs.voice)
        var total = 0L
        stages.forEachIndexed { i, stage ->
            val trimmed = studioTtsNormText(stage.text)
            val lang = studioTtsResolveLang(trimmed, stage.lang)
            val file = cachedFile(voice.elevenLabsId, lang, trimmed)
            val raw = fileDurationMs(file).takeIf { it > 0L } ?: estimateSpeechMs(trimmed)
            total += (raw / prefs.speedForKind(stage.kind)).toLong().coerceAtLeast(200L)
            if (i != stages.lastIndex) {
                total += (pauseAfterStage(stage, prefs) * 1000f).toLong()
            }
        }
        return total.coerceAtLeast(1000L)
    }

    data class CollectionPlayTiming(
        val totalMs: Long,
        val startsMs: LongArray,
    )

    fun collectionPlayTiming(
        collectionId: String,
        hash: String,
        queue: List<WordCard>,
        sourceCards: List<WordCard>,
        prefs: StudioPrefs,
    ): CollectionPlayTiming {
        if (queue.isEmpty()) return CollectionPlayTiming(0L, longArrayOf())
        val pauseMs = (prefs.pauseBetweenCardsSec * 1000f).toLong().coerceAtLeast(0L)
        val starts = LongArray(queue.size)
        var acc = 0L
        queue.forEachIndexed { i, card ->
            starts[i] = acc
            val sourceIndex = sourceCards.indexOfFirst { it.id == card.id }.takeIf { it >= 0 } ?: i
            val file = cardMp3File(collectionId, hash, sourceIndex)
            val duration = fileDurationMs(file).takeIf { it > 0L }
                ?: cardPlayDurationMs(card, sourceIndex + 1, prefs)
            acc += duration.coerceAtLeast(400L)
            if (i != queue.lastIndex) acc += pauseMs
        }
        return CollectionPlayTiming(acc.coerceAtLeast(1000L), starts)
    }

    fun cardMp3File(collectionId: String, hash: String, index: Int): File {
        val dir = File(cardsDir, "${collectionId.trim()}/$hash")
        dir.mkdirs()
        return File(dir, "card_${index + 1}.mp3")
    }

    fun assembleCardMp3(
        clips: List<File>,
        dest: File,
        pauseAfterSec: List<Float> = emptyList(),
    ): Boolean {
        val usable = clips.filter { it.isUsable() }
        if (usable.isEmpty()) return false
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.tmp")
        val pcmOk = runCatching {
            val pcm = assemblePcm(usable, pauseAfterSec)
            AudioMp3Exporter.writePcmToMp3(pcm, tmp)
            tmp.isUsable()
        }.getOrDefault(false)
        if (!pcmOk) {
            tmp.outputStream().use { out ->
                usable.forEachIndexed { i, file ->
                    val bytes = file.readBytes()
                    val offset = if (i == 0) 0 else skipId3(bytes)
                    if (offset < bytes.size) out.write(bytes, offset, bytes.size - offset)
                }
            }
        }
        if (tmp.length() < 200) {
            tmp.delete()
            return false
        }
        if (!tmp.renameTo(dest)) {
            dest.writeBytes(tmp.readBytes())
            tmp.delete()
        }
        return dest.isUsable()
    }

    private fun assemblePcm(clips: List<File>, pauseAfterSec: List<Float>): PcmAudio {
        val decoded = clips.map { AudioPcmDecoder.decode(it.absolutePath) }
        val sampleRate = decoded.first().sampleRate
        val channels = decoded.first().channelCount.coerceIn(1, 2)
        require(decoded.all { it.sampleRate == sampleRate && it.channelCount.coerceIn(1, 2) == channels })
        val chunks = ArrayList<ShortArray>(decoded.size * 2)
        var total = 0
        decoded.forEachIndexed { i, pcm ->
            chunks += pcm.samples
            total += pcm.samples.size
            val pause = pauseAfterSec.getOrElse(i) { 0f }
            if (i != decoded.lastIndex && pause > 0f) {
                val silentFrames = (pause * sampleRate).toInt().coerceAtLeast(0)
                val silent = ShortArray(silentFrames * channels)
                chunks += silent
                total += silent.size
            }
        }
        val samples = ShortArray(total)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(samples, offset)
            offset += chunk.size
        }
        return PcmAudio(samples, sampleRate, channels)
    }

    private suspend fun playCached(
        text: String,
        lang: String,
        voice: StudioVoice,
        speed: Float,
        gen: Int,
    ): Boolean {
        val file = cachedFile(voice.elevenLabsId, lang, text)
        if (!file.isUsable()) return false
        if (gen != generation.get()) return true
        return playFile(file, speed, gen)
    }

    private suspend fun playFile(file: File, speed: Float, gen: Int): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (gen != generation.get()) {
                    if (cont.isActive) cont.resume(true)
                    return@suspendCancellableCoroutine
                }
                runCatching { mediaPlayer?.reset() }
                runCatching { mediaPlayer?.release() }
                val player = MediaPlayer()
                mediaPlayer = player
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                player.setOnCompletionListener {
                    if (cont.isActive) cont.resume(true)
                }
                player.setOnErrorListener { _, _, _ ->
                    if (cont.isActive) cont.resume(false)
                    true
                }
                cont.invokeOnCancellation {
                    runCatching { player.stop() }
                    runCatching { player.reset() }
                }
                runCatching {
                    player.setDataSource(file.absolutePath)
                    player.prepare()
                    applySpeed(player, speed)
                    if (gen != generation.get()) {
                        player.reset()
                        if (cont.isActive) cont.resume(true)
                    } else {
                        player.start()
                        if (playbackPaused.get()) {
                            runCatching { player.pause() }
                        }
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(false)
                }
            }
        }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT < 23 || speed == 1f) return
        runCatching {
            player.playbackParams = PlaybackParams().setSpeed(speed)
        }
    }

    private suspend fun speakSystem(text: String, lang: String, speed: Float, voice: StudioVoice) {
        ensureStarted()
        val engine = tts ?: return
        var waits = 0
        while (!ready.get() && waits < 40) {
            kotlinx.coroutines.delay(50)
            waits++
        }
        if (!ready.get() || text.isBlank()) return

        val locale = when (lang.lowercase()) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale.US
            "es" -> Locale("es", "ES")
            else -> Locale("pt", "BR")
        }
        engine.language = locale
        pickSystemVoice(engine, locale, voice)?.let { engine.voice = it }
        engine.setSpeechRate(speed)

        suspendCancellableCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
            cont.invokeOnCancellation { runCatching { engine.stop() } }
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS && cont.isActive) {
                cont.resume(Unit)
            }
        }
    }

    private fun pickSystemVoice(engine: TextToSpeech, locale: Locale, voice: StudioVoice): Voice? {
        val voices = runCatching { engine.voices }.getOrNull() ?: return null
        val localeVoices = voices.filter { it.locale.language.equals(locale.language, true) }
        val pool = localeVoices.ifEmpty { voices }
        val femaleRe = Regex("female|woman|femin|camila|maria|lucia|helena|zira|irina|milena", RegexOption.IGNORE_CASE)
        val maleRe = Regex("male|man|mascul|antoni|daniel|rafael|pavel", RegexOption.IGNORE_CASE)
        return if (voice.female) {
            pool.firstOrNull { femaleRe.containsMatchIn(it.name) }
        } else {
            pool.firstOrNull { maleRe.containsMatchIn(it.name) && !femaleRe.containsMatchIn(it.name) }
        } ?: pool.firstOrNull()
    }

    private fun fileDurationMs(file: File): Long {
        if (!file.isUsable()) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun estimateSpeechMs(text: String): Long {
        val n = text.trim().length
        if (n == 0) return 0L
        return (n * 70L).coerceAtLeast(400L)
    }

    private fun cachedFile(voiceId: String, lang: String, text: String): File {
        val key = "$voiceId\u0001$lang\u0001$text"
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$hex.mp3")
    }

    private fun File.isUsable(): Boolean = exists() && length() >= 200

    private fun skipId3(bytes: ByteArray): Int {
        if (bytes.size < 10) return 0
        if (bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) {
            return 0
        }
        val size = ((bytes[6].toInt() and 0x7f) shl 21) or
            ((bytes[7].toInt() and 0x7f) shl 14) or
            ((bytes[8].toInt() and 0x7f) shl 7) or
            (bytes[9].toInt() and 0x7f)
        return (10 + size).coerceAtMost(bytes.size)
    }

    private fun writeCache(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }
}
