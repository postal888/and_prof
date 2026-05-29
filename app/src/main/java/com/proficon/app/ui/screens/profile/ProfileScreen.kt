package com.proficon.app.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proficon.app.data.model.AppSettings
import com.proficon.app.data.model.AppThemeMode
import com.proficon.app.data.model.SubtitleFontSize
import com.proficon.app.data.model.TodayPlan
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.PortCard
import com.proficon.app.ui.components.ProficonBrandHeader
import com.proficon.app.ui.components.SectionTitle
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.i18n.AppLanguage
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.i18n.SubtitleLanguage
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.theme.PpAccentSoft
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurface
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
import com.proficon.app.ui.theme.PpTextMuted

@Composable
fun ProfileScreen(
    todayPlan: TodayPlan,
    dictionaryCount: Int,
    totalCards: Int,
    settings: AppSettings,
    onOpenProgress: () -> Unit,
    onUseChatGptChange: (Boolean) -> Unit,
    onPhraseCopyChange: (Boolean) -> Unit,
    onWordContextExampleChange: (Boolean) -> Unit,
    onSubtitleFontSizeChange: (Int) -> Unit,
    onUiLanguageChange: (Int) -> Unit,
    onSubtitleLanguageChange: (Int) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val appLanguage = AppLanguage.fromStorage(settings.uiLanguage)
    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        ProficonBrandHeader(title = "Proficon", subtitle = strings.profileTagline)

        SectionTitle(title = strings.profileStatsSection)
        PortCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(value = dictionaryCount.toString(), label = "в словаре")
                ProfileStat(value = totalCards.toString(), label = "карточек")
                ProfileStat(value = todayPlan.streak.toString(), label = "streak")
            }
        }

        PortCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProgress),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = PpAccent,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.profileOpenProgress,
                        color = PpHeading,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MutedText(strings.profileProgressHint)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PpTextMuted,
                )
            }
        }

        SectionTitle(title = strings.profileSettings)
        PortCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ThemeModeRow(
                    selected = settings.themeMode,
                    onSelect = onThemeModeChange,
                )
                LanguageOptionRow(
                    title = strings.uiLanguageTitle,
                    options = AppLanguage.entries.map { it.storageCode to strings.uiLanguageLabel(it) },
                    selected = settings.uiLanguage,
                    onSelect = onUiLanguageChange,
                )
                LanguageOptionRow(
                    title = strings.subtitleLanguageTitle,
                    options = SubtitleLanguage.all.map {
                        it to SubtitleLanguage.label(it, appLanguage)
                    },
                    selected = settings.subtitleLanguage,
                    onSelect = onSubtitleLanguageChange,
                )
                SettingSwitchRow(
                    title = strings.settingChatGptTitle,
                    subtitle = strings.settingChatGptSubtitle,
                    checked = settings.useChatGptTranslation,
                    onCheckedChange = onUseChatGptChange,
                )
                SettingSwitchRow(
                    title = strings.settingPhraseCopyTitle,
                    subtitle = strings.settingPhraseCopySubtitle,
                    checked = settings.phraseCopyEnabled,
                    onCheckedChange = onPhraseCopyChange,
                )
                SettingSwitchRow(
                    title = strings.settingWordContextTitle,
                    subtitle = strings.settingWordContextSubtitle,
                    checked = settings.wordContextExampleEnabled,
                    onCheckedChange = onWordContextExampleChange,
                )
                SubtitleFontSizeRow(
                    level = settings.subtitleFontSizeLevel,
                    onLevelChange = onSubtitleFontSizeChange,
                )
            }
        }
        SectionTitle(title = strings.profileAboutSection)
        PortCard {
            MutedText(strings.profileAboutText)
        }
    }
}

@Composable
private fun ThemeModeRow(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    val strings = LocalUiStrings.current
    LanguageOptionRow(
        title = strings.themeModeTitle,
        options = listOf(
            AppThemeMode.Dark.storageCode to strings.themeDark,
            AppThemeMode.Light.storageCode to strings.themeLight,
        ),
        selected = selected.storageCode,
        onSelect = { code -> onSelect(AppThemeMode.fromStorage(code)) },
    )
}

@Composable
private fun LanguageOptionRow(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = PpHeading,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PpSurfaceInput)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (code, label) ->
                val active = selected == code
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) PpAccentSoft else PpSurface)
                        .clickable { onSelect(code) }
                        .padding(vertical = 8.dp),
                    color = if (active) PpAccent else PpTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PpHeading,
            )
            MutedText(subtitle)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PpHeading,
                checkedTrackColor = PpAccent,
                uncheckedThumbColor = PpTextMuted,
                uncheckedTrackColor = PpSurfaceInput,
            ),
        )
    }
}

@Composable
private fun SubtitleFontSizeRow(
    level: Int,
    onLevelChange: (Int) -> Unit,
) {
    val strings = LocalUiStrings.current
    val clampedLevel = level.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)
    val previewScale = SubtitleFontSize.scaleForLevel(clampedLevel)
    val previewStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = MaterialTheme.typography.bodyMedium.fontSize * previewScale,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * previewScale,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.subtitleFontSizeTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PpHeading,
                )
                MutedText(strings.subtitleFontSizeSubtitle)
            }
            IconButton(
                onClick = {
                    if (clampedLevel > SubtitleFontSize.MIN_LEVEL) {
                        onLevelChange(clampedLevel - 1)
                    }
                },
                enabled = clampedLevel > SubtitleFontSize.MIN_LEVEL,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Мельче",
                    tint = if (clampedLevel > SubtitleFontSize.MIN_LEVEL) PpAccent else PpTextMuted,
                )
            }
            Text(
                text = strings.subtitleFontSizeLabel(clampedLevel),
                style = MaterialTheme.typography.labelMedium,
                color = PpAccent,
                modifier = Modifier.width(92.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = {
                    if (clampedLevel < SubtitleFontSize.MAX_LEVEL) {
                        onLevelChange(clampedLevel + 1)
                    }
                },
                enabled = clampedLevel < SubtitleFontSize.MAX_LEVEL,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Крупнее",
                    tint = if (clampedLevel < SubtitleFontSize.MAX_LEVEL) PpAccent else PpTextMuted,
                )
            }
        }
        Text(
            text = "Olá, como você está?",
            style = previewStyle,
            color = PpText,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PpSurfaceInput)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = PpHeading,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PpTextMuted,
            textAlign = TextAlign.Center,
        )
    }
}
