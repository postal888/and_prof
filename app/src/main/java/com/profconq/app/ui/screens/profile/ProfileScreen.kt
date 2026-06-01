package com.profconq.app.ui.screens.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.api.AccountInfo
import com.profconq.app.api.SyncPrimary
import com.profconq.app.data.WordLimitPolicy
import com.profconq.app.auth.AuthUser
import com.profconq.app.data.model.AppSettings
import com.profconq.app.data.model.AppThemeMode
import com.profconq.app.data.model.SubtitleFontSize
import com.profconq.app.data.model.TodayPlan
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.SubtitleLanguage
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpAccentSoft
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted

@Composable
private fun SyncPrimaryToggle(
    selected: SyncPrimary,
    onSelect: (SyncPrimary) -> Unit,
    enabled: Boolean,
    appLabel: String,
    siteLabel: String,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(PpSurfaceInput)
            .border(1.dp, PpBorder, shape),
    ) {
        listOf(SyncPrimary.APP to appLabel, SyncPrimary.SITE to siteLabel).forEach { (mode, label) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (isSelected) PpAccentSoft else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) PpAccent else PpTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

private val SwitchColumnWidth = 52.dp
private val SettingsItemSpacing = 16.dp

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
    onTranslationSourceLanguageChange: (Int) -> Unit,
    onTranslationTargetLanguageChange: (Int) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    authUser: AuthUser?,
    authBusy: Boolean,
    authError: String?,
    cloudAccount: AccountInfo?,
    localWordCount: Int,
    syncBusy: Boolean,
    syncMessage: String?,
    syncPrimary: SyncPrimary,
    promoBusy: Boolean = false,
    promoMessage: String? = null,
    onRedeemPromoCode: (String) -> Unit = {},
    onClearPromoMessage: () -> Unit = {},
    onSyncPrimaryChange: (SyncPrimary) -> Unit,
    onCreateGoogleSignInIntent: () -> Intent,
    onGoogleSignInResult: (Intent?) -> Unit,
    onSignOut: () -> Unit,
    onClearAuthError: () -> Unit,
    onMirrorSync: () -> Unit,
    onClearSyncMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val appLanguage = AppLanguage.fromStorage(settings.uiLanguage)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        onGoogleSignInResult(result.data)
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            TabScreenHeader(tab = MainTab.Profile, subtitle = strings.profileTagline)
        }

        item {
        SectionTitle(title = strings.profileAccountSection)
        PortCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val accountTitle = authUser?.displayName
                    ?: authUser?.email
                    ?: strings.profileSignedOut
                Text(
                    text = accountTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                )
                authUser?.email?.let {
                    MutedText(it)
                }
                authError?.let {
                    Text(
                        text = it,
                        color = PpDanger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { onClearAuthError() },
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (authUser == null) {
                            googleSignInLauncher.launch(onCreateGoogleSignInIntent())
                        } else {
                            onSignOut()
                        }
                    },
                    enabled = !authBusy,
                ) {
                    Text(
                        if (authBusy) strings.profileAuthLoading
                        else if (authUser == null) strings.profileSignInGoogle
                        else strings.profileSignOut,
                    )
                }
                val wordLimit = cloudAccount?.wordLimit ?: WordLimitPolicy.FREE_LIMIT
                val wordLimitLabel = if (wordLimit >= WordLimitPolicy.UNLIMITED / 2) "∞" else wordLimit.toString()
                if (authUser == null) {
                    MutedText("${strings.profileLocalWords}: $localWordCount / $wordLimitLabel")
                } else {
                    val wordCount = cloudAccount?.wordCount
                    if (wordCount != null) {
                        MutedText("${strings.profileCloudWords}: $wordCount / $wordLimitLabel")
                    } else {
                        MutedText("${strings.profileLocalWords}: $localWordCount / $wordLimitLabel")
                    }
                }
                if (authUser != null && cloudAccount?.isPremium != true) {
                    var promoInput by remember { mutableStateOf("") }
                    Text(
                        text = strings.profilePromoSection,
                        style = MaterialTheme.typography.labelMedium,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = promoInput,
                            onValueChange = { promoInput = it.uppercase() },
                            placeholder = { Text(strings.profilePromoPlaceholder) },
                            singleLine = true,
                            enabled = !promoBusy && !authBusy,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PpAccent,
                                unfocusedBorderColor = PpBorder,
                            ),
                        )
                        OutlinedButton(
                            onClick = {
                                onRedeemPromoCode(promoInput)
                                promoInput = ""
                            },
                            enabled = !promoBusy && !authBusy && promoInput.isNotBlank(),
                        ) {
                            Text(
                                if (promoBusy) strings.profileAuthLoading else strings.profilePromoApply,
                            )
                        }
                    }
                    promoMessage?.let {
                        Text(
                            text = it,
                            color = PpAccent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onClearPromoMessage() },
                        )
                    }
                }
                if (authUser == null) {
                    OutlinedButton(
                        onClick = { googleSignInLauncher.launch(onCreateGoogleSignInIntent()) },
                        enabled = !syncBusy && !authBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (authBusy) strings.profileAuthLoading
                            else strings.profileSyncCloudSignInFirst,
                        )
                    }
                    MutedText(strings.profileSyncCloudHint)
                } else {
                    MutedText(strings.profileSyncMirrorHint)
                    SyncPrimaryToggle(
                        selected = syncPrimary,
                        onSelect = onSyncPrimaryChange,
                        enabled = !syncBusy && !authBusy,
                        appLabel = strings.profileSyncPrimaryApp,
                        siteLabel = strings.profileSyncPrimarySite,
                    )
                    MutedText(
                        if (syncPrimary == SyncPrimary.APP) strings.profileSyncPrimaryAppHint
                        else strings.profileSyncPrimarySiteHint,
                    )
                    Button(
                        onClick = onMirrorSync,
                        enabled = !syncBusy && !authBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
                    ) {
                        Text(
                            if (syncBusy) strings.profileAuthLoading else strings.profileSyncMirror,
                            color = Color.White,
                        )
                    }
                }
                syncMessage?.let {
                    Text(
                        text = it,
                        color = PpAccent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClearSyncMessage() },
                    )
                }
            }
        }
        }

        item {
        SectionTitle(title = strings.profileStatsSection)
        PortCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(value = dictionaryCount.toString(), label = strings.profileInDictionary)
                ProfileStat(value = totalCards.toString(), label = strings.profileCards)
                ProfileStat(value = todayPlan.streak.toString(), label = "streak")
            }
        }
        }

        item {
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
        }

        item {
        SectionTitle(title = strings.profileSettings)
        PortCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileSettingLabel(strings.themeModeTitle)
                SegmentedChipsRow(
                    options = listOf(
                        AppThemeMode.Dark.storageCode to strings.themeDark,
                        AppThemeMode.Light.storageCode to strings.themeLight,
                    ),
                    selected = settings.themeMode.storageCode,
                    onSelect = { code -> onThemeModeChange(AppThemeMode.fromStorage(code)) },
                )

                ProfileSettingsDivider()

                ProfileSettingLabel(strings.uiLanguageTitle)
                SegmentedChipsRow(
                    options = AppLanguage.entries.map { it.storageCode to strings.uiLanguageLabel(it) },
                    selected = settings.uiLanguage,
                    onSelect = onUiLanguageChange,
                )

                ProfileSettingsDivider()

                ProfileSettingLabel(strings.translationLanguagesTitle)
                MutedText(
                    strings.translationLanguagesHint,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                SegmentedChipsRow(
                    options = SubtitleLanguage.all.map {
                        it to SubtitleLanguage.label(it, appLanguage)
                    },
                    selected = settings.translationSourceLanguage,
                    onSelect = onTranslationSourceLanguageChange,
                    compact = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedChipsRow(
                    options = SubtitleLanguage.all.map {
                        it to SubtitleLanguage.label(it, appLanguage)
                    },
                    selected = settings.translationTargetLanguage,
                    onSelect = onTranslationTargetLanguageChange,
                    compact = true,
                )

                ProfileSettingsDivider()

                ProfileToggleRow(
                    title = strings.settingChatGptTitle,
                    description = strings.settingChatGptSubtitle,
                    checked = settings.useChatGptTranslation,
                    onCheckedChange = onUseChatGptChange,
                )
                ProfileSettingsDivider()
                ProfileToggleRow(
                    title = strings.settingPhraseCopyTitle,
                    description = strings.settingPhraseCopySubtitle,
                    checked = settings.phraseCopyEnabled,
                    onCheckedChange = onPhraseCopyChange,
                )
                ProfileSettingsDivider()
                ProfileToggleRow(
                    title = strings.settingWordContextTitle,
                    description = strings.settingWordContextSubtitle,
                    checked = settings.wordContextExampleEnabled,
                    onCheckedChange = onWordContextExampleChange,
                )

                ProfileSettingsDivider()

                SubtitleFontSizeBlock(
                    level = settings.subtitleFontSizeLevel,
                    onLevelChange = onSubtitleFontSizeChange,
                )
            }
        }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ProfileSettingLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PpHeading,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ProfileSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = SettingsItemSpacing),
        color = PpBorder,
        thickness = 1.dp,
    )
}

@Composable
private fun SegmentedChipsRow(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    compact: Boolean = false,
) {
    val verticalPad = if (compact) 10.dp else 12.dp
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
                    .padding(horizontal = 4.dp, vertical = verticalPad),
                color = if (active) PpAccent else PpTextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 1,
            )
        }
    }
}

@Composable
private fun ProfileToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = PpTextMuted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier.width(SwitchColumnWidth),
            contentAlignment = Alignment.TopCenter,
        ) {
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
}

@Composable
private fun SubtitleFontSizeBlock(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.subtitleFontSizeTitle,
            style = MaterialTheme.typography.titleSmall,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = strings.subtitleFontSizeSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = PpTextMuted,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (clampedLevel > SubtitleFontSize.MIN_LEVEL) {
                        onLevelChange(clampedLevel - 1)
                    }
                },
                enabled = clampedLevel > SubtitleFontSize.MIN_LEVEL,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (clampedLevel > SubtitleFontSize.MIN_LEVEL) PpAccent else PpTextMuted,
                )
            }
            Text(
                text = strings.subtitleFontSizeLabel(clampedLevel),
                style = MaterialTheme.typography.labelMedium,
                color = PpAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(108.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp,
            )
            IconButton(
                onClick = {
                    if (clampedLevel < SubtitleFontSize.MAX_LEVEL) {
                        onLevelChange(clampedLevel + 1)
                    }
                },
                enabled = clampedLevel < SubtitleFontSize.MAX_LEVEL,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
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
                .padding(top = 10.dp)
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
