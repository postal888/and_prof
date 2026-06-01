package com.profconq.app.ui.screens.practice

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
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpHeading

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
        TabScreenHeader(tab = MainTab.Practice, subtitle = strings.practiceScreenSubtitle)

        modules.forEach { (title, desc) ->
            PortCard(modifier = Modifier.fillMaxWidth()) {
                Text(title, color = PpHeading, fontWeight = FontWeight.SemiBold)
                MutedText(desc, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
