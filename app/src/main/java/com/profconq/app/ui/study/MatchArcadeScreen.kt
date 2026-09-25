package com.profconq.app.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ProfconqApplication
import com.profconq.app.data.model.LastStudyTestResult
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.model.putAnswer
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.UiStrings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private data class HitFloater(val id: Int, val text: String)
private data class Spark(val ang: Float, val dist: Float, val size: Float, val hue: Int)

private const val COMBO_CAP = 8
private const val BOARD_SIZE = 5
private val EaseOut = FastOutSlowInEasing

private object MatchArcade {
    val Bg = Color(0xFF050B18)
    val Wash = Color(0xFF0D2048)
    val Dot = Color(0xFF1A2F55)
    val Arena = Color(0xFF0A1224)
    val Paper = Color(0xFF101E36)
    val PaperDeep = Color(0xFF0C172C)
    val Line = Color(0xFF2A3A58)
    val Cyan = Color(0xFF00D2FF)
    val Lime = Color(0xFF7EE887)
    val Miss = Color(0xFFEF4444)
    val Gold = Color(0xFFF4C430)
    val Text = Color(0xFFE8EEF8)
    val Muted = Color(0xFF8BA3C7)
    val Title = Color(0xFFFFFFFF)
    val InkPt = Color(0xFFD7F8F3)
    val InkRu = Color(0xFFE8FFE8)
    val InkOk = Color(0xFFD8F8B4)
}

@Composable
fun MatchingScreen(
    setId: String,
    setName: String,
    repository: ProfconqRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val resultStore = (LocalContext.current.applicationContext as ProfconqApplication).studyTestResultStore
    BackHandler(onBack = onBack)

    var pool by remember { mutableStateOf<List<WordCard>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("start") }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(2) }
    var board by remember { mutableIntStateOf(1) }
    var boards by remember { mutableIntStateOf(1) }
    var remaining by remember { mutableFloatStateOf(60f) }
    var pairs by remember { mutableStateOf(listOf<WordCard>()) }
    var matched by remember { mutableStateOf(setOf<String>()) }
    var gone by remember { mutableStateOf(setOf<String>()) }
    var pick by remember { mutableStateOf<Pair<String, String>?>(null) }
    var ptOrder by remember { mutableStateOf(listOf<WordCard>()) }
    var ruOrder by remember { mutableStateOf(listOf<WordCard>()) }
    var used by remember { mutableStateOf(setOf<String>()) }
    var hintIds by remember { mutableStateOf(setOf<String>()) }
    var badPicks by remember { mutableStateOf(setOf<Pair<String, String>>()) }
    var pickAt by remember { mutableStateOf(0L) }
    var fastStreak by remember { mutableIntStateOf(0) }
    var pairsDone by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    val wordStats = remember { mutableStateMapOf<String, StudyTestWordStat>() }
    var ended by remember { mutableStateOf("") }
    var scorePop by remember { mutableIntStateOf(0) }
    var streakPop by remember { mutableIntStateOf(0) }
    var timePop by remember { mutableIntStateOf(0) }
    var boardTick by remember { mutableIntStateOf(0) }
    var floaters by remember { mutableStateOf(listOf<HitFloater>()) }
    var floaterSeq by remember { mutableIntStateOf(0) }
    var arenaFlash by remember { mutableStateOf("") }
    var fxTick by remember { mutableIntStateOf(0) }
    var fxKind by remember { mutableStateOf("") }
    var comboYell by remember { mutableStateOf("") }

    LaunchedEffect(setId) {
        val ids = repository.getWordIdsInStudySet(setId)
        pool = repository.getCardsByIds(ids).shuffled()
        loaded = true
        phase = "start"
    }

    fun boardCount(source: List<WordCard>) =
        ((source.size + BOARD_SIZE - 1) / BOARD_SIZE).coerceAtLeast(1)

    fun startTime(source: List<WordCard>) =
        (boardCount(source) * 14).coerceIn(40, 90).toFloat()

    fun deal() {
        val fresh = pool.filter { it.id !in used }
        val src = if (fresh.isNotEmpty()) fresh else pool.also { used = emptySet() }
        pairs = src.take(BOARD_SIZE.coerceAtMost(src.size))
        used = used + pairs.map { it.id }
        matched = emptySet()
        gone = emptySet()
        pick = null
        ptOrder = pairs.shuffled()
        ruOrder = pairs.shuffled()
        boardTick += 1
    }

    var runSaved by remember { mutableStateOf(false) }

    fun start() {
        if (pool.size < 2) return
        used = emptySet()
        score = 0
        streak = 0
        best = 0
        hints = 2
        fastStreak = 0
        pairsDone = 0
        misses = 0
        wordStats.clear()
        ended = ""
        runSaved = false
        pickAt = 0L
        gone = emptySet()
        matched = emptySet()
        board = 1
        boards = boardCount(pool)
        remaining = startTime(pool)
        phase = "play"
        deal()
    }

    fun finish(reason: String) {
        ended = reason
        phase = "end"
        if (runSaved) return
        runSaved = true
        resultStore.save(
            LastStudyTestResult(
                setId = setId,
                setName = setName,
                kind = LastStudyTestResult.KIND_MATCH,
                correct = pairsDone,
                incorrect = misses,
                at = System.currentTimeMillis(),
                words = wordStats.values.toList(),
            ),
        )
    }

    fun nextBoard() {
        if (used.size >= pool.size) {
            finish("clear")
        } else {
            board += 1
            deal()
        }
    }

    fun scoreForHit(n: Int) = 10 + when {
        n > 0 && n % 50 == 0 -> 100
        n > 0 && n % 10 == 0 -> 10
        else -> 0
    }

    fun streakBonus(n: Int = streak) = when {
        n > 0 && n % 50 == 0 -> 100
        n > 0 && n % 10 == 0 -> 10
        else -> 0
    }

    fun mult() = when {
        fastStreak >= 6 -> 3
        fastStreak >= 3 -> 2
        else -> 1
    }

    fun bump(id: String, ok: Boolean) {
        val card = pool.find { it.id == id } ?: pairs.find { it.id == id } ?: return
        wordStats.putAnswer(card, ok)
    }

    fun choose(side: String, id: String) {
        if (phase != "play" || id in matched) return
        val cur = pick
        if (cur == null || cur.first == side) {
            pick = side to id
            pickAt = System.currentTimeMillis()
            return
        }
        if (cur.second == id) {
            val fast = pickAt > 0L && System.currentTimeMillis() - pickAt <= 2800
            fastStreak = if (fast) fastStreak + 1 else 0
            matched = matched + id
            pairsDone += 1
            bump(id, true)
            streak += 1
            best = maxOf(best, streak)
            val gain = scoreForHit(streak)
            score += gain
            remaining += 1f
            scorePop += 1
            streakPop += 1
            timePop += 1
            floaterSeq += 1
            floaters = (floaters + HitFloater(floaterSeq, "+$gain  +1${strings.studyTestMatchSec("")}")).takeLast(6)
            arenaFlash = "ok"
            val boardDone = matched.size >= pairs.size
            comboYell = when {
                boardDone -> strings.studyTestMatchClearYell
                streak == 3 -> strings.studyTestMatchComboYell(2)
                streak == 6 -> strings.studyTestMatchComboYell(3)
                streak > 0 && streak % 10 == 0 -> strings.studyTestMatchStreakYell(streak)
                else -> ""
            }
            fxKind = if (boardDone) "clear" else "ok"
            fxTick += 1
            pick = null
            pickAt = 0L
        } else {
            streak = 0
            fastStreak = 0
            misses += 1
            bump(cur.second, false)
            bump(id, false)
            score = (score - 5).coerceAtLeast(0)
            streakPop += 1
            arenaFlash = "bad"
            comboYell = ""
            fxKind = "bad"
            fxTick += 1
            badPicks = setOf(cur, side to id)
            pick = null
            pickAt = 0L
        }
    }

    if (phase == "play") {
        LaunchedEffect(phase, board, boardTick) {
            while (phase == "play") {
                delay(100)
                remaining -= 0.1f
                if (remaining <= 0f) {
                    finish("time")
                    break
                }
            }
        }
    }

    LaunchedEffect(matched, boardTick) {
        val pending = matched - gone
        if (pending.isEmpty()) return@LaunchedEffect
        delay(280)
        gone = gone + pending
    }

    LaunchedEffect(matched, pairs, phase, board, boardTick) {
        if (phase != "play" || pairs.isEmpty()) return@LaunchedEffect
        if (matched.size < pairs.size) return@LaunchedEffect
        delay(560)
        if (phase == "play" && matched.size >= pairs.size) nextBoard()
    }

    if (badPicks.isNotEmpty()) {
        LaunchedEffect(badPicks) {
            delay(440)
            badPicks = emptySet()
        }
    }
    if (hintIds.isNotEmpty()) {
        LaunchedEffect(hintIds) {
            delay(900)
            hintIds = emptySet()
        }
    }
    if (arenaFlash.isNotEmpty()) {
        LaunchedEffect(arenaFlash) {
            delay(220)
            arenaFlash = ""
        }
    }

    val comboFill by animateFloatAsState(
        (streak / COMBO_CAP.toFloat()).coerceIn(0f, 1f),
        tween(200, easing = EaseOut),
        label = "combo",
    )
    val extra = streakBonus()
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(arenaFlash) {
        if (arenaFlash != "bad") return@LaunchedEffect
        listOf(11f, -10f, 7f, -5f, 3f, 0f).forEach { x ->
            shakeX.snapTo(x)
            delay(26)
        }
    }
    val timePulse by rememberInfiniteTransition(label = "time").animateFloat(
        0.55f,
        1f,
        infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "tpulse",
    )

    MatchStage(modifier) {
        Column(Modifier.fillMaxSize()) {
            TabScreenHeader(
                title = setName.ifBlank { strings.studyTestMatchTitle },
                subtitle = "${strings.studyTestMatchHud} ⚡",
                onBack = onBack,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = PortLayout.HeaderToContent),
            ) {
                when {
                    !loaded -> { }
                    pool.size < 2 -> ArcadeOverlay {
                        Text(strings.studyTestNeedWords, color = MatchArcade.Muted, textAlign = TextAlign.Center)
                        GradientPrimaryButton(strings.studyTestDone, onBack, Modifier.fillMaxWidth())
                    }
                    phase == "start" -> ArcadeOverlay {
                        Text(
                            "${strings.studyTestMatchHud} ⚡",
                            color = MatchArcade.Title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(strings.studyTestMatchLead, color = MatchArcade.Muted, textAlign = TextAlign.Center)
                        GradientPrimaryButton(strings.studyTestMatchStart, onClick = ::start, modifier = Modifier.fillMaxWidth())
                    }
                    phase == "end" -> ArcadeOverlay {
                        Text(
                            if (ended == "clear") strings.studyTestMatchEndClear else strings.studyTestMatchEndTime,
                            color = MatchArcade.Title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            strings.studyTestAnswersLine(pairsDone, misses),
                            color = MatchArcade.Title,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            strings.studyTestMatchEndLine(pairsDone, score, best),
                            color = MatchArcade.Muted,
                            textAlign = TextAlign.Center,
                        )
                        GradientPrimaryButton(strings.studyTestMatchAgain, onClick = { phase = "start" }, modifier = Modifier.fillMaxWidth())
                    }
                    phase == "pause" -> ArcadeOverlay {
                        Text(
                            strings.studyTestMatchPause,
                            color = MatchArcade.Title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        )
                        Text(strings.studyTestMatchPauseLead, color = MatchArcade.Muted)
                        GradientPrimaryButton(strings.studyTestMatchResume, onClick = { phase = "play" }, modifier = Modifier.fillMaxWidth())
                    }
                    else -> PlayArena(
                        strings = strings,
                        remaining = remaining,
                        score = score,
                        streak = streak,
                        extra = extra,
                        comboFill = comboFill,
                        board = board,
                        boards = boards,
                        mult = mult(),
                        timePop = timePop,
                        scorePop = scorePop,
                        streakPop = streakPop,
                        timePulse = timePulse,
                        shakeX = shakeX.value,
                        arenaFlash = arenaFlash,
                        ptOrder = ptOrder,
                        ruOrder = ruOrder,
                        matched = matched,
                        gone = gone,
                        pick = pick,
                        hintIds = hintIds,
                        badPicks = badPicks,
                        hints = hints,
                        floaters = floaters,
                        fxTick = fxTick,
                        fxKind = fxKind,
                        comboYell = comboYell,
                        onChoose = ::choose,
                        onHint = {
                            if (hints > 0) {
                                val target = pick?.second?.takeIf { it !in matched }
                                    ?: pairs.firstOrNull { it.id !in matched }?.id
                                if (target != null) {
                                    hints -= 1
                                    hintIds = setOf(target)
                                }
                            }
                        },
                        onNewGame = { phase = "start" },
                        onPause = { phase = "pause" },
                        onFloaterGone = { goneId -> floaters = floaters.filter { it.id != goneId } },
                        onYellGone = { comboYell = "" },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayArena(
    strings: UiStrings,
    remaining: Float,
    score: Int,
    streak: Int,
    extra: Int,
    comboFill: Float,
    board: Int,
    boards: Int,
    mult: Int,
    timePop: Int,
    scorePop: Int,
    streakPop: Int,
    timePulse: Float,
    shakeX: Float,
    arenaFlash: String,
    ptOrder: List<WordCard>,
    ruOrder: List<WordCard>,
    matched: Set<String>,
    gone: Set<String>,
    pick: Pair<String, String>?,
    hintIds: Set<String>,
    badPicks: Set<Pair<String, String>>,
    hints: Int,
    floaters: List<HitFloater>,
    fxTick: Int,
    fxKind: String,
    comboYell: String,
    onChoose: (String, String) -> Unit,
    onHint: () -> Unit,
    onNewGame: () -> Unit,
    onPause: () -> Unit,
    onFloaterGone: (Int) -> Unit,
    onYellGone: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatBox(
                strings.studyTestMatchTime,
                strings.studyTestMatchSec("%.1f".format(remaining.coerceAtLeast(0f))),
                if (remaining <= 8f) MatchArcade.Miss else MatchArcade.Cyan,
                Modifier.weight(1f),
                popKey = timePop,
                urgent = remaining <= 8f,
                urgentPulse = timePulse,
            )
            StatBox(strings.studyTestMatchScore, "$score", MatchArcade.Gold, Modifier.weight(1f), popKey = scorePop)
            StatBox(strings.studyTestMatchStreak, "x$streak", MatchArcade.Lime, Modifier.weight(1f), popKey = streakPop)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeX }
                .clip(RoundedCornerShape(18.dp))
                .background(MatchArcade.Arena, RoundedCornerShape(18.dp))
                .border(1.dp, MatchArcade.Cyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .drawBehind {
                    val stepPx = 16.dp.toPx()
                    var x = 8f
                    while (x < size.width) {
                        var y = 8f
                        while (y < size.height) {
                            drawCircle(MatchArcade.Dot, 1.1f, Offset(x, y))
                            y += stepPx
                        }
                        x += stepPx
                    }
                    drawRoundRect(
                        Brush.radialGradient(
                            listOf(MatchArcade.Cyan.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.12f),
                            radius = size.maxDimension * 0.5f,
                        ),
                    )
                    if (arenaFlash == "ok") {
                        drawRoundRect(MatchArcade.Lime.copy(alpha = 0.14f))
                    } else if (arenaFlash == "bad") {
                        drawRoundRect(MatchArcade.Miss.copy(alpha = 0.2f))
                    }
                }
                .padding(10.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        strings.studyTestMatchBoard(board, boards),
                        color = MatchArcade.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .background(MatchArcade.PaperDeep, RoundedCornerShape(999.dp))
                            .border(1.dp, MatchArcade.Line, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                    MultChip(mult)
                }
                ComboTrack(
                    fill = comboFill,
                    label = if (extra > 0) strings.studyTestMatchComboBonus(streak, extra)
                    else strings.studyTestMatchCombo(streak),
                    hot = streak >= 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColHead(strings.studyTestMatchPtCol, MatchArcade.Cyan)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ptOrder.forEach { w ->
                                    WordBtn(
                                        text = w.pt,
                                        done = w.id in matched,
                                        gone = w.id in gone,
                                        on = pick?.first == "pt" && pick?.second == w.id,
                                        hint = w.id in hintIds,
                                        bad = ("pt" to w.id) in badPicks,
                                        pt = true,
                                    ) { onChoose("pt", w.id) }
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColHead(strings.studyTestMatchRuCol, MatchArcade.Lime)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ruOrder.forEach { w ->
                                    WordBtn(
                                        text = w.ru,
                                        done = w.id in matched,
                                        gone = w.id in gone,
                                        on = pick?.first == "ru" && pick?.second == w.id,
                                        hint = w.id in hintIds,
                                        bad = ("ru" to w.id) in badPicks,
                                        pt = false,
                                    ) { onChoose("ru", w.id) }
                                }
                            }
                        }
                    }
                }
            }
            FloaterLayer(floaters, onFloaterGone)
            FxLayer(fxTick, fxKind, comboYell, onYellGone)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArcadeChip(strings.studyTestMatchHint(hints), Modifier.weight(1f), onHint)
            GradientPrimaryButton(
                strings.studyTestMatchNewGame,
                onNewGame,
                modifier = Modifier.weight(1.2f),
                compact = true,
            )
            ArcadeChip(strings.studyTestMatchPause, Modifier.weight(1f), onPause)
        }
    }
}

@Composable
private fun MatchStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(MatchArcade.Bg),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(MatchArcade.Wash, MatchArcade.Bg),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.maxDimension * 0.72f,
                ),
            )
            val step = 16.dp.toPx()
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(MatchArcade.Dot, 1.2f, Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = PortLayout.Gutter, vertical = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun ArcadeOverlay(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MatchArcade.Bg.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MatchArcade.Paper, RoundedCornerShape(18.dp))
                .border(1.dp, MatchArcade.Cyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun ArcadeChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = text,
        color = MatchArcade.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MatchArcade.Paper)
            .border(1.dp, MatchArcade.Cyan.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

@Composable
private fun ComboTrack(fill: Float, label: String, hot: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val motion = rememberInfiniteTransition(label = "combo")
    val pulse by motion.animateFloat(
        0.7f,
        1f,
        infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "cpulse",
    )
    Box(
        modifier
            .height(28.dp)
            .clip(shape)
            .drawBehind {
                val h = size.height
                val r = h / 2f
                drawRoundRect(MatchArcade.PaperDeep, cornerRadius = CornerRadius(r))
                val w = size.width * fill.coerceIn(0f, 1f)
                if (w > 1f) {
                    val fillW = w.coerceAtLeast(h).coerceAtMost(size.width)
                    val cap = Path().apply {
                        addRoundRect(RoundRect(Rect(0f, 0f, fillW, h), CornerRadius(r)))
                    }
                    val bright = if (hot) 0.08f * pulse else 0f
                    drawPath(
                        cap,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF14532D),
                                MatchArcade.Lime.copy(alpha = 0.85f - bright * 0.2f),
                            ),
                            startX = 0f,
                            endX = fillW,
                        ),
                    )
                }
            }
            .border(1.dp, MatchArcade.Line, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            color = MatchArcade.Title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
            style = TextStyle(shadow = Shadow(Color(0xF0041016), Offset(0f, 1.2f), 6f)),
        )
    }
}

@Composable
private fun MultChip(mult: Int) {
    val hot = mult > 1
    val pulse by rememberInfiniteTransition(label = "mult").animateFloat(
        1f,
        if (hot) 1.08f else 1f,
        infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "mpulse",
    )
    Text(
        "⚡ x $mult",
        color = MatchArcade.Cyan,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .graphicsLayer {
                if (hot) {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
            .background(MatchArcade.Cyan.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .border(1.dp, MatchArcade.Cyan.copy(alpha = if (hot) 0.7f else 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun FxLayer(tick: Int, kind: String, yell: String, onYellGone: () -> Unit) {
    if (tick == 0) return
    val t = remember { Animatable(0f) }
    val sparks = remember(tick) {
        val n = if (kind == "clear") 28 else 16
        List(n) { i ->
            Spark(
                ang = ((i * 2.1f + tick) % 360) * (PI.toFloat() / 180f),
                dist = 64f + (i % 6) * 22f,
                size = 2.2f + (i % 4) * 0.7f,
                hue = i % 3,
            )
        }
    }
    LaunchedEffect(tick) {
        t.snapTo(0f)
        t.animateTo(1f, tween(if (kind == "clear") 620 else 500, easing = EaseOut))
    }
    val p = t.value
    Canvas(Modifier.fillMaxSize()) {
        if (p <= 0f || p >= 1f) return@Canvas
        val cx = size.width * 0.5f
        val cy = size.height * 0.58f
        val main = when (kind) {
            "bad" -> MatchArcade.Miss
            "clear" -> MatchArcade.Cyan
            else -> MatchArcade.Lime
        }
        listOf(0.05f, 0.22f, 0.4f).forEach { delayK ->
            val rp = ((p - delayK) / 0.72f).coerceIn(0f, 1f)
            if (rp > 0f) {
                drawCircle(
                    color = main.copy(alpha = (1f - rp) * 0.38f),
                    radius = 14.dp.toPx() + rp * size.minDimension * 0.46f,
                    center = Offset(cx, cy),
                    style = Stroke(width = (3.4.dp.toPx() * (1f - rp * 0.65f)).coerceAtLeast(1f)),
                )
            }
        }
        if (kind != "bad") {
            val bolt = (1f - p).coerceIn(0f, 1f)
            val mid = Color(0xFFE8FFE0).copy(alpha = bolt)
            drawLine(
                Brush.horizontalGradient(listOf(MatchArcade.Cyan.copy(alpha = 0f), mid, MatchArcade.Lime.copy(alpha = 0f))),
                Offset(size.width * 0.1f, cy),
                Offset(size.width * 0.9f, cy),
                strokeWidth = 5.dp.toPx() * bolt,
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = bolt * 0.55f), Color.Transparent)),
                Offset(size.width * 0.18f, cy),
                Offset(size.width * 0.82f, cy),
                strokeWidth = 1.6.dp.toPx(),
            )
        }
        sparks.forEach { s ->
            val travel = s.dist.dp.toPx() * p
            val c = when {
                kind == "bad" -> MatchArcade.Miss
                s.hue == 0 -> MatchArcade.Lime
                s.hue == 1 -> MatchArcade.Cyan
                else -> MatchArcade.Gold
            }
            drawCircle(
                c.copy(alpha = (1f - p).coerceIn(0f, 1f)),
                s.size.dp.toPx() * (1.2f - p * 0.55f),
                Offset(cx + cos(s.ang) * travel, cy + sin(s.ang) * travel),
            )
        }
    }
    if (yell.isNotEmpty()) {
        val yA = remember { Animatable(0f) }
        LaunchedEffect(tick, yell) {
            yA.snapTo(0f)
            yA.animateTo(1f, tween(140, easing = EaseOut))
            delay(480)
            yA.animateTo(0f, tween(200))
            onYellGone()
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                yell,
                color = if (kind == "bad") MatchArcade.Miss else Color(0xFFF7FFE8),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier.graphicsLayer {
                    val k = yA.value
                    alpha = k
                    scaleX = 0.72f + 0.28f * k
                    scaleY = 0.72f + 0.28f * k
                    translationY = -8.dp.toPx() * k
                },
                style = TextStyle(shadow = Shadow(Color(0xE0041016), Offset(0f, 3f), 12f)),
            )
        }
    }
}

@Composable
private fun FloaterLayer(items: List<HitFloater>, onGone: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        items.forEach { item ->
            key(item.id) {
                val rise = remember { Animatable(0f) }
                LaunchedEffect(item.id) {
                    rise.animateTo(1f, tween(700, easing = EaseOut))
                    onGone(item.id)
                }
                Text(
                    item.text,
                    color = MatchArcade.Lime,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = -48.dp.toPx() * rise.value
                        alpha = 1f - rise.value
                        scaleX = 1f + 0.12f * (1f - rise.value)
                        scaleY = 1f + 0.12f * (1f - rise.value)
                    },
                    style = TextStyle(shadow = Shadow(Color(0xCC041016), Offset(0f, 2f), 8f)),
                )
            }
        }
    }
}

@Composable
private fun ColHead(text: String, accent: Color) {
    Text(
        text,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    accent.copy(alpha = 0.16f),
                    Offset(-2f, 4f),
                    Size(size.width + 4f, size.height),
                    CornerRadius(10.dp.toPx()),
                )
            }
            .background(MatchArcade.Paper, RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    popKey: Int = 0,
    urgent: Boolean = false,
    urgentPulse: Float = 1f,
) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(popKey) {
        if (popKey == 0) return@LaunchedEffect
        pop.snapTo(1f)
        pop.animateTo(1.1f, tween(90, easing = EaseOut))
        pop.animateTo(1f, tween(140, easing = EaseOut))
    }
    val glow = if (urgent) 0.22f + 0.28f * urgentPulse else 0.18f
    Column(
        modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .drawBehind {
                drawRoundRect(
                    accent.copy(alpha = glow),
                    Offset(-3f, 2f),
                    Size(size.width + 6f, size.height + 4f),
                    CornerRadius(12.dp.toPx()),
                )
            }
            .background(MatchArcade.Paper, RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = if (urgent) 0.75f else 0.45f), RoundedCornerShape(12.dp))
            .drawBehind {
                drawCircle(accent.copy(alpha = 0.12f), size.maxDimension * 0.7f, Offset(size.width * 0.8f, size.height * 0.2f))
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label.uppercase(), color = MatchArcade.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun WordBtn(
    text: String,
    done: Boolean,
    gone: Boolean,
    on: Boolean,
    hint: Boolean,
    bad: Boolean,
    pt: Boolean,
    click: () -> Unit,
) {
    val accent = if (pt) MatchArcade.Cyan else MatchArcade.Lime
    val goneA by animateFloatAsState(if (gone) 0f else 1f, tween(220, easing = EaseOut), label = "goneA")
    val goneS by animateFloatAsState(if (gone) 0.96f else 1f, tween(220, easing = EaseOut), label = "goneS")
    val okWave = remember { Animatable(0f) }
    val badWave = remember { Animatable(0f) }
    val pickPop = remember { Animatable(1f) }
    LaunchedEffect(on) {
        if (!on) return@LaunchedEffect
        pickPop.snapTo(1f)
        pickPop.animateTo(1.08f, tween(70, easing = EaseOut))
        pickPop.animateTo(1f, tween(130, easing = EaseOut))
    }
    LaunchedEffect(done, gone) {
        if (done && !gone) {
            okWave.snapTo(0f)
            pickPop.snapTo(1.12f)
            pickPop.animateTo(1f, tween(220, easing = EaseOut))
            okWave.animateTo(1f, tween(280, easing = EaseOut))
        } else {
            okWave.snapTo(0f)
        }
    }
    LaunchedEffect(bad) {
        if (bad) {
            badWave.snapTo(0f)
            badWave.animateTo(1f, tween(420, easing = EaseOut))
        } else {
            badWave.snapTo(0f)
        }
    }
    val okPulse = blinkPulse(okWave.value)
    val badPulse = blinkPulse(badWave.value)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressS = if (pressed && !done) 0.97f else 1f
    val glow = when {
        gone -> 0f
        done -> 0.22f + 0.36f * okPulse
        bad -> 0.5f * badPulse
        hint || on -> 0.4f
        else -> 0.12f
    }
    val border = when {
        done -> MatchArcade.Lime
        bad && badPulse > 0.45f -> MatchArcade.Miss
        hint || on -> accent
        else -> MatchArcade.Line
    }
    val bg = when {
        done -> MatchArcade.Paper.lerp(MatchArcade.Lime, 0.12f + 0.24f * okPulse)
        bad && badPulse > 0.45f -> MatchArcade.Paper.lerp(MatchArcade.Miss, 0.28f)
        hint || on -> MatchArcade.Paper.lerp(accent, 0.16f)
        else -> MatchArcade.Paper
    }
    val ink = when {
        done -> MatchArcade.InkOk
        pt -> MatchArcade.InkPt
        else -> MatchArcade.InkRu
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                val punch = pickPop.value
                alpha = goneA
                scaleX = goneS * pressS * punch
                scaleY = goneS * pressS * punch
                translationX = if (bad) sin(badWave.value * 28f) * 7f * (1f - badWave.value) else 0f
            }
            .drawBehind {
                if (glow > 0.02f) {
                    val c = when {
                        done -> MatchArcade.Lime
                        bad -> MatchArcade.Miss
                        else -> accent
                    }
                    drawRoundRect(
                        c.copy(alpha = glow),
                        Offset(-4f, 2f),
                        Size(size.width + 8f, size.height + 6f),
                        CornerRadius(10.dp.toPx()),
                    )
                }
            }
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(
                enabled = !done && !gone,
                interactionSource = interaction,
                indication = null,
                onClick = click,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun blinkPulse(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return when {
        x < 0.22f -> x / 0.22f
        x < 0.45f -> 1f - (x - 0.22f) / 0.23f
        x < 0.68f -> (x - 0.45f) / 0.23f
        else -> (1f - (x - 0.68f) / 0.32f).coerceAtLeast(0f)
    }.coerceIn(0f, 1f)
}

private fun Color.lerp(other: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * k,
        green = green + (other.green - green) * k,
        blue = blue + (other.blue - blue) * k,
        alpha = alpha + (other.alpha - alpha) * k,
    )
}
