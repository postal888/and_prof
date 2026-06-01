package com.profconq.app.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.profconq.app.ui.i18n.LocalUiStrings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsStrip(
    knownCount: Int,
    dontKnowCount: Int,
    remaining: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatPill(
            icon = "✓",
            value = knownCount,
            label = strings.practiceStatKnow,
            accentColor = PracticeSessionColors.Accent,
            modifier = Modifier.weight(1f),
        )
        StatPill(
            icon = "✗",
            value = dontKnowCount,
            label = strings.practiceStatDontKnow,
            accentColor = PracticeSessionColors.Danger,
            modifier = Modifier.weight(1f),
        )
        StatPill(
            icon = "→",
            value = remaining,
            label = strings.practiceStatRemaining,
            accentColor = PracticeSessionColors.Info,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun StatPill(
    icon: String,
    value: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PracticeSessionColors.BgElev)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PracticeSessionColors.TextPrimary,
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = PracticeSessionColors.TextFaint,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
