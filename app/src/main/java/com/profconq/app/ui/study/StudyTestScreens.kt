package com.profconq.app.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ProfconqApplication
import com.profconq.app.data.model.LastStudyTestResult
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.model.putAnswer
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpTextMuted
import kotlinx.coroutines.delay

enum class StudyTestFormat { Choice, Match }

@Composable
fun TestFormatScreen(
    setName: String,
    onBack: () -> Unit,
    onChoose: (StudyTestFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
    ) {
        TabScreenHeader(
            title = strings.studyTestTitle,
            subtitle = setName.ifBlank { strings.tabStudy },
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))
        PortCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChoose(StudyTestFormat.Choice) },
        ) {
            Text(strings.studyTestChoiceTitle, color = PpHeading, fontWeight = FontWeight.SemiBold)
            MutedText(strings.studyTestChoiceHint)
        }
        Spacer(modifier = Modifier.height(10.dp))
        PortCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChoose(StudyTestFormat.Match) },
        ) {
            Text(strings.studyTestMatchTitle, color = PpHeading, fontWeight = FontWeight.SemiBold)
            MutedText(strings.studyTestMatchHint)
        }
    }
}

@Composable
fun MultipleChoiceScreen(
    setId: String,
    setName: String,
    repository: ProfconqRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val resultStore = (LocalContext.current.applicationContext as ProfconqApplication).studyTestResultStore
    BackHandler(onBack = onBack)
    var cards by remember { mutableStateOf<List<WordCard>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    val wordStats = remember { mutableStateMapOf<String, StudyTestWordStat>() }
    var persisted by remember { mutableStateOf(false) }

    fun nextQuestion(pool: List<WordCard>, at: Int) {
        val card = pool.getOrNull(at) ?: return
        val wrong = pool.filter { it.id != card.id }.map { it.ru }.distinct().shuffled().take(3)
        options = (wrong + card.ru).shuffled()
        picked = null
    }

    LaunchedEffect(setId) {
        val ids = repository.getWordIdsInStudySet(setId)
        val pool = repository.getCardsByIds(ids).shuffled()
        cards = pool
        index = 0
        correctCount = 0
        wrongCount = 0
        wordStats.clear()
        loaded = true
        if (pool.isNotEmpty()) nextQuestion(pool, 0)
    }

    LaunchedEffect(picked) {
        if (picked == null) return@LaunchedEffect
        delay(650)
        val next = index + 1
        index = next
        if (next < cards.size) nextQuestion(cards, next)
    }

    val card = cards.getOrNull(index)
    val done = cards.isNotEmpty() && index >= cards.size

    LaunchedEffect(done) {
        if (!done || persisted) return@LaunchedEffect
        persisted = true
        resultStore.save(
            LastStudyTestResult(
                setId = setId,
                setName = setName,
                kind = LastStudyTestResult.KIND_CHOICE,
                correct = correctCount,
                incorrect = wrongCount,
                at = System.currentTimeMillis(),
                words = wordStats.values.toList(),
            ),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
    ) {
        TabScreenHeader(
            title = setName.ifBlank { strings.studyTestChoiceTitle },
            subtitle = if (done || card == null) strings.studyTestChoiceTitle
            else "${index + 1} / ${cards.size}",
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))
        when {
            !loaded -> { }
            cards.isEmpty() -> MutedText(strings.studyTestNeedWords)
            done -> {
                val total = cards.size
                val accuracy = if (total > 0) (correctCount * 100) / total else 0
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        strings.studyTestDone,
                        color = PpHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TestStatRow(strings.studyTestCorrect, correctCount.toString(), PpNeonGreen)
                    Spacer(modifier = Modifier.height(10.dp))
                    TestStatRow(strings.studyTestIncorrect, wrongCount.toString(), PracticeSessionColors.Danger)
                    Spacer(modifier = Modifier.height(10.dp))
                    TestStatRow(strings.studyTestTotal, total.toString(), PpHeading)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        strings.studyTestAccuracy(accuracy),
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress = { if (total > 0) correctCount.toFloat() / total else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(6.dp),
                        color = PpNeonGreen,
                        trackColor = PracticeSessionColors.BorderStrong,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                GradientPrimaryButton(strings.studyBackToMenu, onBack, Modifier.fillMaxWidth())
            }
            card != null -> {
                PortCard(modifier = Modifier.fillMaxWidth()) {
                    Text(card.pt, color = PpHeading, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    card.example?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = PpTextMuted, fontStyle = FontStyle.Italic)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { option ->
                    val isCorrect = option == card.ru
                    val show = picked != null
                    val color = when {
                        show && isCorrect -> PpNeonGreen
                        show && picked == option -> PracticeSessionColors.Danger
                        else -> PpAccent.copy(alpha = 0.35f)
                    }
                    Text(
                        text = option,
                        color = if (show && isCorrect) PpNeonGreen else PpHeading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, color, RoundedCornerShape(10.dp))
                            .clickable(enabled = picked == null) {
                                picked = option
                                if (isCorrect) correctCount++ else wrongCount++
                                wordStats.putAnswer(card, isCorrect)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TestStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PpTextMuted, fontSize = 16.sp)
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
