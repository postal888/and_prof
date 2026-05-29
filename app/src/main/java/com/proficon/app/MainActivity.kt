package com.proficon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proficon.app.ui.MainViewModel
import com.proficon.app.ui.MainViewModelFactory
import com.proficon.app.ui.ProficonApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ProficonApplication
        val factory = MainViewModelFactory(app.repository)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)
            ProficonApp(viewModel = viewModel, repository = app.repository)
        }
    }
}
