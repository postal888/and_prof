package com.profconq.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpSurfaceInput

@Composable
fun OutlinedAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PpAccent,
    containerColor: Color = PpSurfaceInput,
    enabled: Boolean = true,
) {
    PortOutlinedAction(
        text = text,
        onClick = onClick,
        modifier = modifier,
        accentColor = accentColor,
        containerColor = containerColor,
        enabled = enabled,
    )
}

@Composable
fun OutlinedDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.error,
    containerColor: Color = PpSurfaceInput,
    enabled: Boolean = true,
) {
    PortOutlinedAction(
        text = text,
        onClick = onClick,
        modifier = modifier,
        accentColor = borderColor,
        containerColor = containerColor,
        enabled = enabled,
    )
}

@Composable
private fun PortOutlinedAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    accentColor: Color,
    containerColor: Color,
    enabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPortPressScale(interactionSource, enabled)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(PortButtonHeight),
        shape = PortButtonShape,
        border = BorderStroke(1.5.dp, accentColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = accentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = accentColor.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    }
}
