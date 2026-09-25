package com.profconq.app.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.components.PortButtonShape
import com.profconq.app.ui.components.rememberPortPressScale

@Composable
fun PracticeOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PracticeSessionColors.Accent,
    fillColor: Color = PracticeSessionColors.ButtonSurface,
    height: Dp = 52.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPortPressScale(interactionSource)
    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .height(height),
        shape = PortButtonShape,
        border = BorderStroke(1.5.dp, accentColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = fillColor,
            contentColor = accentColor,
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
