package com.profconq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.rememberAccentGradientBrush

/** Square slot on the shelf — progress ring is a true circle. */
private val BookCoverSize = 56.dp
private val BookThumbSize = 34.dp
private val BookIconSize = 18.dp

/**
 * Book cover tile with optional circular progress ring (library list).
 * Fixed size — does not stretch when the title is long.
 */
@Composable
fun BookLibraryCover(
    progress: Float,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showProgressRing: Boolean = true,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val gradient = rememberAccentGradientBrush()
    Box(
        modifier = modifier.requiredSize(BookCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        if (showProgressRing && clamped > 0f && !isLoading) {
            CircularGradientProgress(
                progress = clamped,
                size = BookCoverSize,
                strokeWidth = 3.dp,
                showLabel = false,
            )
        }
        Box(
            modifier = Modifier
                .requiredSize(BookThumbSize)
                .clip(RoundedCornerShape(8.dp))
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(BookThumbSize - 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PpBrandNavy.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    GradientCircularLoader(size = 18.dp, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = PpHeading,
                        modifier = Modifier.size(BookIconSize),
                    )
                }
            }
        }
        if (showProgressRing && clamped > 0f && !isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .clip(CircleShape)
                    .background(PpBrandNavy.copy(alpha = 0.9f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "${(clamped * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PpHeading,
                )
            }
        }
    }
}
