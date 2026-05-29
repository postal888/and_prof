package com.proficon.app.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

fun hasRecordAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

class RecordAudioPermissionState(
    val granted: Boolean,
    val request: () -> Unit,
) {
    fun runWithPermission(onGranted: () -> Unit) {
        if (granted) onGranted() else request()
    }
}

@Composable
fun rememberRecordAudioPermission(): RecordAudioPermissionState {
    val context = LocalContext.current
    var granted by rememberSaveable {
        mutableStateOf(hasRecordAudioPermission(context))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
    }
    return RecordAudioPermissionState(
        granted = granted,
        request = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
    )
}
