package com.proficon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.theme.PpBorder
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurface
import com.proficon.app.ui.theme.PpTextMuted

@Composable
fun PortCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PpSurface)
            .border(1.dp, PpBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}

@Composable
fun TodayStatsRow(
    dueCount: Int,
    newCount: Int,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    PortCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatCell(value = dueCount.toString(), label = strings.statDueLabel)
            StatCell(value = newCount.toString(), label = strings.statNewLabel)
            StatCell(value = streak.toString(), label = strings.statStreakLabel)
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PpTextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = PpTextMuted,
        )
        BrandAccentDivider(modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = PpTextMuted,
        modifier = modifier,
    )
}

@Composable
fun AccentHighlight(text: String) {
    Text(
        text = text,
        color = PpAccent,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    ProficonBrandHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        logoSize = logoSize,
    )
}
