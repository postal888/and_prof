package com.profconq.app.ui.study

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.components.PortButtonShape
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.rememberPortPressScale
import com.profconq.app.ui.i18n.LocalUiStrings

@Composable
fun PracticeSessionBottomBar(
    onDontKnow: () -> Unit,
    onKnow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val view = LocalView.current
    fun haptic(action: () -> Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        action()
    }

    val fill = PracticeSessionColors.ButtonSurface
    val dontKnowSource = remember { MutableInteractionSource() }
    val knowSource = remember { MutableInteractionSource() }
    val dontKnowScale = rememberPortPressScale(dontKnowSource)
    val knowScale = rememberPortPressScale(knowSource)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PortLayout.Gutter, end = PortLayout.Gutter, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { haptic(onDontKnow) },
            interactionSource = dontKnowSource,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = dontKnowScale
                    scaleY = dontKnowScale
                }
                .weight(1f)
                .height(56.dp),
            shape = PortButtonShape,
            border = BorderStroke(1.5.dp, PracticeSessionColors.Danger),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = fill,
                contentColor = PracticeSessionColors.Danger,
            ),
        ) {
            Text(
                text = strings.practiceDontKnow,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PracticeSessionColors.Danger,
            )
        }
        OutlinedButton(
            onClick = { haptic(onKnow) },
            interactionSource = knowSource,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = knowScale
                    scaleY = knowScale
                }
                .weight(1f)
                .height(56.dp),
            shape = PortButtonShape,
            border = BorderStroke(1.5.dp, PracticeSessionColors.Accent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = fill,
                contentColor = PracticeSessionColors.Accent,
            ),
        ) {
            Text(
                text = strings.practiceKnow,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PracticeSessionColors.Accent,
            )
        }
    }
}
