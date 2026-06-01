package com.profconq.app.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.profconq.app.data.model.WordCard

@Composable
fun PracticeFlashCard(
    card: WordCard,
    direction: StudyDirection,
    isFlipped: Boolean,
    isExampleExpanded: Boolean,
    collectionTitle: String?,
    onFlip: () -> Unit,
    onToggleExample: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cardFlip",
    )

    val frontPrimary = if (direction == StudyDirection.PT_TO_RU) card.pt else card.ru
    val backPrimary = if (direction == StudyDirection.PT_TO_RU) card.ru else card.pt
    val backSecondary = if (direction == StudyDirection.PT_TO_RU) card.pt else card.ru
    val chapterLabel = card.chapterOrTag?.takeIf { it.isNotBlank() }
        ?: collectionTitle?.let { "📖 $it" }
    val hasImage = !card.displayImage.isNullOrBlank()
    val hasExample = !card.example.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PracticeSessionColors.CardGradient)
            .border(1.dp, PracticeSessionColors.Border, RoundedCornerShape(22.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
    ) {
        if (rotation <= 90f) {
            CardFace(
                primaryText = frontPrimary,
                partOfSpeech = if (direction == StudyDirection.PT_TO_RU) card.partOfSpeech else null,
                card = card,
                chapterLabel = chapterLabel,
                hasImage = hasImage,
                hasExample = hasExample,
                isExampleExpanded = isExampleExpanded,
                isFlipped = false,
                direction = direction,
                onCardClick = onFlip,
                onToggleExample = onToggleExample,
                onPlayAudio = onPlayAudio,
                onToggleFavorite = onToggleFavorite,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
            ) {
                CardFace(
                    primaryText = frontPrimary,
                    partOfSpeech = null,
                    card = card,
                    chapterLabel = chapterLabel,
                    hasImage = hasImage,
                    hasExample = hasExample,
                    isExampleExpanded = isExampleExpanded,
                    isFlipped = true,
                    direction = direction,
                    onCardClick = onFlip,
                    onToggleExample = onToggleExample,
                    onPlayAudio = onPlayAudio,
                    onToggleFavorite = onToggleFavorite,
                    backTranslation = backPrimary,
                    backOriginal = backSecondary,
                    ipa = card.ipa,
                )
            }
        }
    }
}

@Composable
private fun CardFace(
    primaryText: String,
    partOfSpeech: String?,
    backTranslation: String? = null,
    backOriginal: String? = null,
    ipa: String? = null,
    card: WordCard,
    chapterLabel: String?,
    hasImage: Boolean,
    hasExample: Boolean,
    isExampleExpanded: Boolean,
    isFlipped: Boolean,
    direction: StudyDirection,
    onCardClick: () -> Unit,
    onToggleExample: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chapterLabel != null) {
                Text(
                    text = chapterLabel,
                    fontSize = 11.sp,
                    color = PracticeSessionColors.TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PracticeSessionColors.BgElev)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                            onClick = {},
                        ),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            Row {
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Прослушать",
                        tint = PracticeSessionColors.Accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Избранное",
                        tint = if (card.isFavorite) PracticeSessionColors.Accent else PracticeSessionColors.TextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(onClick = onCardClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isFlipped) {
                    Text(
                        text = primaryText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = PracticeSessionColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 44.sp,
                    )
                    partOfSpeech?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = PracticeSessionColors.TextMuted,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (isFlipped && !backTranslation.isNullOrBlank()) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(0.5f),
                        color = PracticeSessionColors.BorderStrong,
                    )
                    Text(
                        text = backTranslation,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PracticeSessionColors.Accent,
                        textAlign = TextAlign.Center,
                    )
                    ipa?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = PracticeSessionColors.TextFaint,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (direction == StudyDirection.PT_TO_RU) {
                        card.partOfSpeech?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = PracticeSessionColors.TextFaint,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        backOriginal?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = PracticeSessionColors.TextPrimary,
                                modifier = Modifier.padding(top = 6.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                        card.partOfSpeech?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = PracticeSessionColors.TextMuted,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            if (hasImage) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PracticeSessionColors.BgElev),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = card.displayImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        if (hasExample) {
            HorizontalDivider(color = PracticeSessionColors.Border)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (isExampleExpanded) "💡 Пример" else "💡 Показать пример (нужна подсказка)",
                    fontSize = 13.sp,
                    color = PracticeSessionColors.Accent,
                    modifier = Modifier
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                            onClick = onToggleExample,
                        )
                        .padding(vertical = 4.dp),
                )
                AnimatedVisibility(
                    visible = isExampleExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        val exampleWord = if (direction == StudyDirection.PT_TO_RU) card.pt else card.pt
                        HighlightedExampleText(
                            fullText = card.example.orEmpty(),
                            highlight = exampleWord,
                        )
                        card.sourceTitle?.takeIf { it.isNotBlank() }?.let { source ->
                            Text(
                                text = "📖 $source",
                                fontSize = 11.sp,
                                color = PracticeSessionColors.TextFaint,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (isFlipped && !card.exampleTranslation.isNullOrBlank()) {
                            Box(modifier = Modifier.padding(top = 8.dp)) {
                                HighlightedExampleText(
                                    fullText = card.exampleTranslation.orEmpty(),
                                    highlight = card.ru,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
