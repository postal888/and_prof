package com.profconq.app.ui.study

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val buttonShape = RoundedCornerShape(16.dp)
    val fill = PracticeSessionColors.ButtonSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { haptic(onDontKnow) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = buttonShape,
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
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = buttonShape,
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
