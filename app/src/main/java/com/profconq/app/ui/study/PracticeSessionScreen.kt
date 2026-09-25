package com.profconq.app.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import com.profconq.app.ui.components.LoadingContent
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalUiStrings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ProfconqApplication
import androidx.compose.ui.platform.LocalContext

@Composable
fun PracticeSessionScreen(
    setId: String,
    sessionKey: Int,
    repository: ProfconqRepository,
    startWithFullSet: Boolean = false,
    onFinishLesson: () -> Unit,
    onEditWord: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val intensity = (LocalContext.current.applicationContext as ProfconqApplication).studyIntensityStore
    val viewModel: PracticeSessionViewModel = viewModel(
        key = "practice_session_${setId}_${sessionKey}_$startWithFullSet",
        factory = PracticeSessionViewModelFactory(repository, setId, startWithFullSet, intensity),
    )
    val state by viewModel.state.collectAsState()
    var showCardMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().portScreenBackground(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    val strings = LocalUiStrings.current
                    LoadingContent(
                        message = strings.loadingSession,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                state.isAllReviewedToday -> {
                    BackHandler(onBack = onFinishLesson)
                    StudyAllReviewedScreen(
                        setName = state.setName,
                        wordCount = state.setTotalWords,
                        masteredCount = state.deckMasteredCount,
                        onPracticeFullSet = viewModel::startFullSetReview,
                        onBackToMenu = onFinishLesson,
                    )
                }
                state.isComplete -> {
                    BackHandler(onBack = onFinishLesson)
                    SessionSummaryScreen(
                        knownCount = state.knownCount,
                        dontKnowCount = state.dontKnowCount,
                        lessonWordCount = state.lessonWordCount,
                        deckMasteredCount = state.deckMasteredCount,
                        setTotalWords = state.setTotalWords,
                        onBackToStudy = onFinishLesson,
                    )
                }
                state.current != null -> {
                    BackHandler(onBack = viewModel::showExitDialog)
                    val card = state.current!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        SessionHeader(
                            setName = state.setName,
                            currentCard = state.currentCardNumber,
                            totalCards = state.initialTotal,
                            progressPercent = state.progressPercent,
                            direction = state.direction,
                            onClose = viewModel::showExitDialog,
                            onToggleDirection = viewModel::toggleDirection,
                        )
                        StatsStrip(
                            knownCount = state.knownCount,
                            dontKnowCount = state.dontKnowCount,
                            remaining = state.remaining,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        PracticeFlashCard(
                            card = card,
                            direction = state.direction,
                            isFlipped = state.isFlipped,
                            onFlip = viewModel::flipCard,
                            onToggleDirection = viewModel::toggleDirection,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = PortLayout.Gutter)
                                .pointerInput(card.id) {
                                    detectTapGestures(
                                        onLongPress = { showCardMenu = true },
                                    )
                                },
                        )
                        PracticeSessionBottomBar(
                            onDontKnow = viewModel::rateDontKnow,
                            onKnow = viewModel::rateKnown,
                        )
                    }
                }
            }
        }
    }

    val strings = LocalUiStrings.current
    if (state.showExitDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExitDialog,
            title = { Text(strings.practiceExitSessionTitle, color = PracticeSessionColors.TextPrimary) },
            text = { Text(strings.practiceExitSessionMessage, color = PracticeSessionColors.TextMuted) },
            confirmButton = {
                TextButton(onClick = onFinishLesson) {
                    Text(strings.practiceExitConfirm, color = PracticeSessionColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExitDialog) {
                    Text(strings.cancel, color = PracticeSessionColors.TextMuted)
                }
            },
            containerColor = PracticeSessionColors.BgElev,
        )
    }

    if (showCardMenu && state.current != null) {
        val card = state.current!!
        AlertDialog(
            onDismissRequest = { showCardMenu = false },
            title = { Text(card.pt, color = PracticeSessionColors.TextPrimary) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showCardMenu = false
                            onEditWord?.invoke(card.collectionId, card.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.practiceEditWord, color = PracticeSessionColors.Accent)
                    }
                    TextButton(
                        onClick = {
                            showCardMenu = false
                            viewModel.removeCurrentFromSet()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.practiceRemoveFromSet, color = PracticeSessionColors.Danger)
                    }
                    TextButton(
                        onClick = { showCardMenu = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.practiceReportProblem, color = PracticeSessionColors.TextMuted)
                    }
                }
            },
            confirmButton = {},
            containerColor = PracticeSessionColors.BgElev,
        )
    }
}
