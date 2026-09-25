package com.profconq.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.R
import com.profconq.app.ui.MainViewModel
import com.profconq.app.ui.MainViewModelFactory
import com.profconq.app.ui.ProfconqApp
import com.profconq.app.ui.theme.DarkPortPalette
import com.profconq.app.util.isEmulator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleStudioIntent(intent)
        handleVideoIntent(intent)
        handleTabIntent(intent)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setBackgroundDrawableResource(R.color.brand_navy)
        if (isEmulator()) {
            // x86 AVD GPUs often fail Compose GPU paths and show a black screen.
            window.decorView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        val app = application as ProfconqApplication
        val factory = MainViewModelFactory(
            app.repository,
            app.authManager,
            app.dictionarySyncService,
            app.profconqAdminSession,
            app.profconqSessionAuth,
            onSignOutCleanup = { app.profconqSessionAuth.clearSession() },
        )

        val screenBackground = DarkPortPalette.bg
        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = screenBackground,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBackground),
                ) {
                    ProfconqApp(
                        viewModel = viewModel,
                        repository = app.repository,
                        authManager = app.authManager,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStudioIntent(intent)
        handleVideoIntent(intent)
        handleTabIntent(intent)
    }

    private fun handleStudioIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_STUDIO, false) == true) {
            com.profconq.app.studio.StudioNowPlayingHub.requestOpenStudio()
        }
    }

    private fun handleVideoIntent(intent: Intent?) {
        if (intent == null) return
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)?.trim().orEmpty()
        if (videoId.isNotEmpty()) {
            com.profconq.app.youtube.YouTubeNowPlayingHub.requestOpenVideoId(videoId)
            return
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_VIDEO, false)) {
            com.profconq.app.youtube.YouTubeNowPlayingHub.requestOpenVideo()
        }
    }

    private fun handleTabIntent(intent: Intent?) {
        val tab = com.profconq.app.ui.navigation.TabOpenHub.parse(intent?.getStringExtra(EXTRA_OPEN_TAB))
        if (tab != null) {
            com.profconq.app.ui.navigation.TabOpenHub.request(tab)
        }
    }

    companion object {
        const val EXTRA_OPEN_STUDIO = "open_studio"
        const val EXTRA_OPEN_VIDEO = "open_video"
        const val EXTRA_OPEN_TAB = "open_tab"
        const val EXTRA_VIDEO_ID = "video_id"
    }
}
