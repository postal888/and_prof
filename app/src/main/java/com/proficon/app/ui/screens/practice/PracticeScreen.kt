package com.proficon.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.PortCard
import com.proficon.app.ui.components.ProficonBrandHeader
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.theme.PpHeading

@Composable
fun PracticeScreen(modifier: Modifier = Modifier) {
    val strings = LocalUiStrings.current
    val modules = listOf(
        strings.practiceModuleCelpeTitle to strings.practiceModuleCelpeDesc,
        strings.practiceModuleVerbsTitle to strings.practiceModuleVerbsDesc,
        strings.practiceModuleYoutubeTitle to strings.practiceModuleYoutubeDesc,
        strings.practiceModulePronunciationTitle to strings.practiceModulePronunciationDesc,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        ProficonBrandHeader(
            title = strings.tabPractice,
            subtitle = strings.practiceScreenSubtitle,
        )

        modules.forEach { (title, desc) ->
            PortCard(modifier = Modifier.fillMaxWidth()) {
                Text(title, color = PpHeading, fontWeight = FontWeight.SemiBold)
                MutedText(desc, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
