package com.profconq.app.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.profconq.app.data.model.WordCard
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.studio.StudioCardColors
import com.profconq.app.ui.studio.StudioDirChip
import com.profconq.app.ui.studio.StudioFlipCardContent
import com.profconq.app.ui.studio.StudioTagChip
import com.profconq.app.ui.studio.drawStudioNotebook

@Composable
fun PracticeFlashCard(
    card: WordCard,
    direction: StudyDirection,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cardFlip",
    )
    val dirPtToRu = direction == StudyDirection.PT_TO_RU
    val strings = LocalUiStrings.current
    val studyLangs = LocalStudyLanguagePrefs.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 16f * density
                }
                .shadow(
                    10.dp,
                    RoundedCornerShape(22.dp),
                    ambientColor = Color(0x66000000),
                    spotColor = Color(0x66000000),
                )
                .clip(RoundedCornerShape(22.dp))
                .drawBehind { drawStudioNotebook() }
                .border(1.dp, StudioCardColors.Border, RoundedCornerShape(22.dp))
                .clickable(onClick = onFlip)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StudioTagChip(
                        tag = card.partOfSpeech.orEmpty().ifBlank { "geral" },
                        strings = strings,
                    )
                    StudioDirChip(
                        dirPtToRu = dirPtToRu,
                        sourceLang = studyLangs.source,
                        targetLang = studyLangs.target,
                        onClick = onToggleDirection,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                        .graphicsLayer { if (rotation > 90f) rotationY = 180f },
                    contentAlignment = Alignment.Center,
                ) {
                    StudioFlipCardContent(
                        card = card,
                        dirPtToRu = dirPtToRu,
                        showBack = rotation > 90f,
                    )
                }
            }
        }
    }
}
