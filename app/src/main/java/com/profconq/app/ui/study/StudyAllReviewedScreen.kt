package com.profconq.app.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.i18n.LocalUiStrings

@Composable
fun StudyAllReviewedScreen(
    setName: String,
    wordCount: Int,
    masteredCount: Int,
    onPracticeFullSet: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StudyMilestoneIllustration(
            style = StudyMilestoneStyle.ScheduleClear,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = strings.studyAllReviewedTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PracticeSessionColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (setName.isNotBlank()) {
            Text(
                text = setName,
                fontSize = 14.sp,
                color = PracticeSessionColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
        if (masteredCount > 0) {
            Text(
                text = strings.studyMasteredInSet(masteredCount),
                fontSize = 14.sp,
                color = PracticeSessionColors.Accent,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = strings.studyAllReviewedHint,
            fontSize = 14.sp,
            color = PracticeSessionColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (wordCount > 0) {
            PracticeOutlinedButton(
                text = strings.studyPracticeFullSet(wordCount),
                onClick = onPracticeFullSet,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onBackToMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, PracticeSessionColors.BorderStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PracticeSessionColors.ButtonSurface,
                contentColor = PracticeSessionColors.TextMuted,
            ),
        ) {
            Text(
                text = strings.studyBackToMenu,
                fontWeight = FontWeight.SemiBold,
                color = PracticeSessionColors.TextMuted,
            )
        }
    }
}
