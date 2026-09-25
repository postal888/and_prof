package com.profconq.app.ui.screens.youtube

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.profconq.app.youtube.YouTubeNowPlayingHub
import com.profconq.app.youtube.YouTubePlaybackCommands
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText

@Composable
fun YouTubePlayerSection(
    videoId: String,
    initialSeekSec: Float,
    pendingSeekSec: Float,
    onSeekApplied: () -> Unit,
    onCurrentSec: (Float) -> Unit,
    compact: Boolean,
    backgroundPlaybackEnabled: Boolean = false,
    onPlayerState: (playing: Boolean, paused: Boolean) -> Unit = { _, _ -> },
    onDurationSec: (Float) -> Unit = {},
    onPlayerDetached: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    var playerError by remember(videoId) { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        key(videoId) {
            YouTubePlayerViewComposable(
                videoId = videoId,
                initialSeekSec = initialSeekSec,
                pendingSeekSec = pendingSeekSec,
                onSeekApplied = onSeekApplied,
                onCurrentSec = onCurrentSec,
                onError = { playerError = it },
                onPlaybackStarted = { playerError = null },
                backgroundPlaybackEnabled = backgroundPlaybackEnabled,
                onPlayerState = onPlayerState,
                onDurationSec = onDurationSec,
                onPlayerDetached = onPlayerDetached,
            )
        }

        if (playerError != null && !compact) {
            MutedText(strings.ytEmbeddedPlayerError(playerError!!))
        }

        if (!compact) {
            Button(
                onClick = {
                    val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
                    val youtubeIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                        setPackage("com.google.android.youtube")
                    }
                    try {
                        context.startActivity(youtubeIntent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(strings.ytOpenInYoutube, color = PpText)
            }
        }
    }
}

/** Placeholder with the same size as the embedded player (watch layout spacing). */
@Composable
fun YouTubePlayerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurface),
    )
}

@Composable
fun YouTubePlayerViewComposable(
    videoId: String,
    initialSeekSec: Float,
    pendingSeekSec: Float,
    onSeekApplied: () -> Unit,
    onCurrentSec: (Float) -> Unit,
    onError: (String) -> Unit,
    onPlaybackStarted: () -> Unit,
    backgroundPlaybackEnabled: Boolean = false,
    onPlayerState: (playing: Boolean, paused: Boolean) -> Unit = { _, _ -> },
    onDurationSec: (Float) -> Unit = {},
    onPlayerDetached: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    var playerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    val origin = remember { "https://${context.packageName}" }
    val resumeSec = initialSeekSec.coerceAtLeast(0f)

    LaunchedEffect(pendingSeekSec) {
        if (pendingSeekSec >= 0f) {
            playerRef?.seekTo(pendingSeekSec)
            onSeekApplied()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurface),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.enableBackgroundPlayback(backgroundPlaybackEnabled)
            },
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    enableBackgroundPlayback(backgroundPlaybackEnabled)
                    addFullscreenListener(
                        object : FullscreenListener {
                            override fun onEnterFullscreen(
                                fullscreenView: View,
                                exitFullscreen: () -> Unit,
                            ) {
                                // Library throws if no listener; block fullscreen to avoid crashes.
                                exitFullscreen()
                            }

                            override fun onExitFullscreen() = Unit
                        },
                    )

                    val options = IFramePlayerOptions.Builder()
                        .controls(1)
                        .fullscreen(0)
                        .rel(0)
                        .origin(origin)
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                playerRef = youTubePlayer
                                youTubePlayer.cueVideo(videoId, resumeSec)
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                onCurrentSec(second)
                            }

                            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                onDurationSec(duration)
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState,
                            ) {
                                when (state) {
                                    PlayerConstants.PlayerState.PLAYING,
                                    PlayerConstants.PlayerState.BUFFERING,
                                    -> {
                                        onPlaybackStarted()
                                        onPlayerState(true, false)
                                    }
                                    PlayerConstants.PlayerState.PAUSED -> onPlayerState(false, true)
                                    else -> onPlayerState(false, false)
                                }
                            }

                            override fun onError(
                                youTubePlayer: YouTubePlayer,
                                error: PlayerConstants.PlayerError,
                            ) {
                                onPlayerState(false, false)
                                val message = when (error) {
                                    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                                        strings.ytPlayerErrorNotEmbeddable
                                    PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                                        strings.ytPlayerErrorNotFound
                                    else -> strings.ytPlayerErrorGeneric(error.name)
                                }
                                onError(message)
                            }
                        },
                        options,
                    )

                    post {
                        (getChildAt(0) as? WebView)?.let { webView ->
                            webView.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                            }
                            webView.postDelayed({
                                webView.evaluateJavascript(
                                    """
                                    (function() {
                                      var s = document.createElement('style');
                                      s.textContent = '.ytp-fullscreen-button,.ytp-size-button{display:none!important;}';
                                      (document.head || document.documentElement).appendChild(s);
                                    })();
                                    """.trimIndent(),
                                    null,
                                )
                            }, 1500L)
                        }
                    }
                }
            },
        )
    }

    DisposableEffect(playerRef) {
        val player = playerRef
        if (player == null) {
            onDispose { }
        } else {
            val commands = object : YouTubePlaybackCommands {
                override fun play() {
                    player.play()
                }
                override fun pause() {
                    player.pause()
                }
                override fun playPause() {
                    if (YouTubeNowPlayingHub.state.value.playing) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
                override fun seekBy(deltaSec: Float) {
                    val now = YouTubeNowPlayingHub.state.value
                    val next = ((now.elapsedMs / 1000f) + deltaSec).coerceAtLeast(0f)
                    val max = if (now.durationMs > 0L) now.durationMs / 1000f else next
                    player.seekTo(next.coerceAtMost(max))
                }
                override fun seekTo(ms: Long) {
                    player.seekTo((ms / 1000f).coerceAtLeast(0f))
                }
                override fun stop() {
                    player.pause()
                    onPlayerDetached()
                }
            }
            YouTubeNowPlayingHub.bind(commands)
            onDispose {
                YouTubeNowPlayingHub.unbind(commands)
            }
        }
    }

    DisposableEffect(videoId) {
        onDispose {
            playerRef = null
            onPlayerDetached()
        }
    }
}

