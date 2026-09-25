package com.profconq.app.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import com.profconq.app.MainActivity
import com.profconq.app.ProfconqApplication
import com.profconq.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSessionCompat
    private var collectJob: Job? = null
    private var lastArtKey: String? = null
    private var lastArt: Bitmap? = null
    private var fallbackArt: Bitmap? = null
    private var lastMetaKey: String? = null
    private var lastNotifKey: String? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        fallbackArt = BitmapFactory.decodeResource(resources, R.drawable.profconq_logo)
        session = MediaSessionCompat(this, SESSION_TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() = StudioNowPlayingHub.commands?.playPause() ?: Unit
                    override fun onPause() = StudioNowPlayingHub.commands?.playPause() ?: Unit
                    override fun onSkipToNext() = StudioNowPlayingHub.commands?.next() ?: Unit
                    override fun onSkipToPrevious() = StudioNowPlayingHub.commands?.prev() ?: Unit
                    override fun onStop() = StudioNowPlayingHub.commands?.stop() ?: Unit
                },
            )
            isActive = true
        }
        startInForeground(StudioNowPlayingHub.state.value, fallbackArt)
        collectJob = scope.launch {
            StudioNowPlayingHub.state.collectLatest { now ->
                if (!now.live) {
                    stopSelf()
                    return@collectLatest
                }
                val art = loadArt(now.artwork)
                publish(now, art)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> StudioNowPlayingHub.commands?.playPause()
            ACTION_NEXT -> StudioNowPlayingHub.commands?.next()
            ACTION_PREV -> StudioNowPlayingHub.commands?.prev()
            ACTION_STOP -> StudioNowPlayingHub.commands?.stop()
        }
        startInForeground(StudioNowPlayingHub.state.value, lastArt ?: fallbackArt)
        if (!StudioNowPlayingHub.state.value.live) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        scope.cancel()
        runCatching { session.isActive = false }
        runCatching { session.release() }
        lastArt = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun loadArt(key: String?): Bitmap? {
        val source = key?.takeIf { it.isNotBlank() }
        if (source == lastArtKey && lastArt != null) return lastArt
        if (source == null) {
            lastArtKey = null
            lastArt = fallbackArt
            return fallbackArt
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val result = ImageLoader(this@StudioPlaybackService).execute(
                    ImageRequest.Builder(this@StudioPlaybackService)
                        .data(source)
                        .size(ART_SIZE)
                        .allowHardware(false)
                        .build(),
                )
                result.drawable?.let { drawable ->
                    val w = drawable.intrinsicWidth.coerceAtLeast(1)
                    val h = drawable.intrinsicHeight.coerceAtLeast(1)
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(out)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    out
                }
            }.getOrNull()
        }
        lastArtKey = source
        lastArt = bitmap ?: fallbackArt
        return lastArt
    }

    private fun publish(now: StudioNowPlaying, art: Bitmap?) {
        val artwork = art ?: fallbackArt
        val duration = now.durationMs.coerceAtLeast(0L)
        val position = now.elapsedMs.coerceIn(0L, if (duration > 0L) duration else now.elapsedMs)
        val metaKey = listOf(now.title, now.subtitle, now.collection, now.index, now.total, duration, now.artwork).joinToString("|")
        if (metaKey != lastMetaKey) {
            lastMetaKey = metaKey
            val strings = (application as ProfconqApplication).uiStrings()
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, now.title.ifBlank { getString(R.string.app_name) })
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, now.subtitle.ifBlank { now.collection })
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, now.collection.ifBlank { strings.tabStudio })
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, now.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, now.subtitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, now.collection)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, now.index.toLong())
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, now.total.toLong())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .build(),
            )
        }
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP
        val state = when {
            now.playing -> PlaybackStateCompat.STATE_PLAYING
            now.paused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, if (now.playing) 1f else 0f, SystemClock.elapsedRealtime())
                .build(),
        )
        val notifKey = listOf(now.title, now.subtitle, now.playing, now.paused, now.index, now.artwork).joinToString("|")
        if (notifKey != lastNotifKey) {
            lastNotifKey = notifKey
            startInForeground(now, artwork)
        }
    }

    private fun startInForeground(now: StudioNowPlaying, art: Bitmap?) {
        val notification = buildNotification(now, art)
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(now: StudioNowPlaying, art: Bitmap?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_STUDIO, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val strings = (application as ProfconqApplication).uiStrings()
        val playIcon = if (now.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val progress = if (now.total > 0) "${now.index} / ${now.total}" else ""
        val text = listOf(now.subtitle, progress, now.collection).filter { it.isNotBlank() }.joinToString(" · ")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_studio)
            .setContentTitle(now.title.ifBlank { strings.tabStudio })
            .setContentText(text)
            .setLargeIcon(art)
            .setContentIntent(open)
            .setDeleteIntent(actionIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(now.live)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setColor(0xFF0A1E45.toInt())
            .addAction(android.R.drawable.ic_media_previous, strings.notifyPrevious, actionIntent(ACTION_PREV))
            .addAction(playIcon, if (now.playing) strings.commonPause else strings.commonPlay, actionIntent(ACTION_PLAY_PAUSE))
            .addAction(android.R.drawable.ic_media_next, strings.notifyNext, actionIntent(ACTION_NEXT))
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(actionIntent(ACTION_STOP)),
            )
            .build()
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, StudioPlaybackService::class.java).setAction(action)
        val code = action.hashCode()
        return PendingIntent.getService(
            this,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            (application as ProfconqApplication).uiStrings().tabStudio,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_UPDATE = "com.profconq.app.studio.UPDATE"
        const val ACTION_PLAY_PAUSE = "com.profconq.app.studio.PLAY_PAUSE"
        const val ACTION_NEXT = "com.profconq.app.studio.NEXT"
        const val ACTION_PREV = "com.profconq.app.studio.PREV"
        const val ACTION_STOP = "com.profconq.app.studio.STOP"
        private const val CHANNEL_ID = "studio_now_playing"
        private const val SESSION_TAG = "profconq.studio"
        private const val NOTIFICATION_ID = 7101
        private const val ART_SIZE = 512
    }
}
