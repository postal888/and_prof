package com.profconq.app.ui.study



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.MaterialTheme

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

fun SessionSummaryScreen(

    knownCount: Int,

    dontKnowCount: Int,

    lessonWordCount: Int,

    deckMasteredCount: Int,

    setTotalWords: Int,

    onBackToStudy: () -> Unit,

    modifier: Modifier = Modifier,

) {

    val strings = LocalUiStrings.current

    val total = lessonWordCount.coerceAtLeast(0)

    val known = knownCount.coerceIn(0, total)

    val dontKnow = dontKnowCount.coerceIn(0, (total - known).coerceAtLeast(0))

    val sessionPercent = if (total > 0) ((known * 100) / total).coerceIn(0, 100) else 0



    val deckTotal = setTotalWords.coerceAtLeast(total)

    val deckMastered = deckMasteredCount.coerceIn(0, deckTotal)

    val deckPercent = if (deckTotal > 0) ((deckMastered * 100) / deckTotal).coerceIn(0, 100) else 0



    Column(

        modifier = modifier

            .fillMaxSize()

            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center,

    ) {

        StudyMilestoneIllustration(style = StudyMilestoneStyle.SessionComplete)

        Spacer(modifier = Modifier.height(20.dp))

        Text(

            text = strings.practiceSummaryDone,

            style = MaterialTheme.typography.headlineMedium,

            fontWeight = FontWeight.Bold,

            color = PracticeSessionColors.TextPrimary,

        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(

            modifier = Modifier.fillMaxWidth(),

            verticalArrangement = Arrangement.spacedBy(12.dp),

        ) {

            SummaryStatRow(

                label = strings.practiceSummaryKnow,

                value = known.toString(),

                color = PracticeSessionColors.Accent,

            )

            SummaryStatRow(

                label = strings.practiceSummaryDontKnow,

                value = dontKnow.toString(),

                color = PracticeSessionColors.Danger,

            )

            SummaryStatRow(

                label = strings.practiceSummaryTotal,

                value = total.toString(),

                color = PracticeSessionColors.TextPrimary,

            )

        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(

            text = strings.practiceSummarySessionProgress(known, total, sessionPercent),

            fontSize = 14.sp,

            color = PracticeSessionColors.TextPrimary,

            textAlign = TextAlign.Center,

            fontWeight = FontWeight.SemiBold,

        )

        LinearProgressIndicator(

            progress = { if (total > 0) known.toFloat() / total else 0f },

            modifier = Modifier

                .fillMaxWidth()

                .padding(top = 12.dp)

                .height(6.dp),

            color = PracticeSessionColors.Accent,

            trackColor = PracticeSessionColors.BorderStrong,

        )

        Text(

            text = strings.practiceSummaryDeckMastery(deckMastered, deckTotal, deckPercent),

            fontSize = 13.sp,

            color = PracticeSessionColors.TextMuted,

            textAlign = TextAlign.Center,

            modifier = Modifier.padding(top = 16.dp),

        )

        Spacer(modifier = Modifier.height(32.dp))

        PracticeOutlinedButton(
            text = strings.studyBackToMenu,
            onClick = onBackToStudy,
        )

    }

}



@Composable

private fun SummaryStatRow(

    label: String,

    value: String,

    color: androidx.compose.ui.graphics.Color,

) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically,

    ) {

        Text(label, fontSize = 18.sp, color = PracticeSessionColors.TextMuted)

        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)

    }

}


