package com.profconq.app.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.SubtitleLanguage

@Composable
fun SessionHeader(
    setName: String,
    currentCard: Int,
    totalCards: Int,
    progressPercent: Int,
    direction: StudyDirection,
    onClose: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCards > 0) currentCard.toFloat() / totalCards else 0f
    var directionPulse by remember { mutableStateOf(false) }
    LaunchedEffect(direction) {
        directionPulse = true
        kotlinx.coroutines.delay(125)
        directionPulse = false
    }
    val directionScale by animateFloatAsState(
        targetValue = if (directionPulse) 0.9f else 1f,
        animationSpec = tween(125, easing = FastOutSlowInEasing),
        label = "directionScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedIconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp),
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = PracticeSessionColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = setName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PracticeSessionColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = PracticeSessionColors.Accent,
                trackColor = PracticeSessionColors.BorderStrong,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Карточка $currentCard из $totalCards",
                    fontSize = 11.sp,
                    color = PracticeSessionColors.TextMuted,
                )
                Text(
                    text = "$progressPercent%",
                    fontSize = 11.sp,
                    color = PracticeSessionColors.TextMuted,
                )
            }
        }

        DirectionToggle(
            direction = direction,
            onClick = onToggleDirection,
            modifier = Modifier.scale(directionScale),
        )
    }
}

@Composable
fun DirectionToggle(
    direction: StudyDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studyLangs = LocalStudyLanguagePrefs.current
    val forward = direction == StudyDirection.PT_TO_RU
    val fromCode = if (forward) studyLangs.source else studyLangs.target
    val toCode = if (forward) studyLangs.target else studyLangs.source
    val arrow = if (forward) "→" else "←"
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = PracticeSessionColors.BgElev,
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeSessionColors.BorderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(SubtitleLanguage.shortCode(fromCode), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PracticeSessionColors.Accent)
            Text(arrow, fontSize = 10.sp, color = PracticeSessionColors.TextMuted, modifier = Modifier.padding(horizontal = 1.dp))
            Text(SubtitleLanguage.shortCode(toCode), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PracticeSessionColors.Info)
        }
    }
}
