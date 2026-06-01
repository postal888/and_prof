package com.profconq.app

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        super.onCreate(savedInstanceState)
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
}
