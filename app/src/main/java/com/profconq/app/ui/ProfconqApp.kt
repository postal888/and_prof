package com.profconq.app.ui

import com.profconq.app.auth.FirebaseAuthManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.model.AppThemeMode
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.FloatingNowPlayingOverlay
import com.profconq.app.ui.components.PortBottomNav
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.StudyLanguagePrefs
import com.profconq.app.ui.i18n.UiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.screens.dictionary.CardEditorScreen
import com.profconq.app.ui.screens.dictionary.DictionaryScreen
import com.profconq.app.ui.screens.home.HomeScreen
import com.profconq.app.ui.screens.profile.ProfileScreen
import com.profconq.app.ui.screens.progress.ProgressScreen
import com.profconq.app.ui.screens.reader.ReaderScreen
import com.profconq.app.ui.screens.study.StudyScreen
import com.profconq.app.ui.screens.youtube.YouTubeScreen
import com.profconq.app.ui.study.CreateStudySetScreen
import com.profconq.app.ui.study.MatchingScreen
import com.profconq.app.ui.study.MultipleChoiceScreen
import com.profconq.app.ui.study.PracticeSessionScreen
import com.profconq.app.ui.study.StudySetSettingsScreen
import com.profconq.app.ui.study.StudyTestFormat
import com.profconq.app.ui.study.StudyWordSelectionStore
import com.profconq.app.ui.study.TestFormatScreen
import com.profconq.app.ui.theme.DarkPortPalette
import com.profconq.app.ui.theme.LightPortPalette
import com.profconq.app.ui.theme.PpBg
import com.profconq.app.ui.theme.PortTheme
import com.profconq.app.youtube.YouTubeViewModel
import com.profconq.app.youtube.YouTubeViewModelFactory

@Composable
fun ProfconqApp(
    viewModel: MainViewModel,
    repository: ProfconqRepository,
    authManager: FirebaseAuthManager,
) {
    var activeTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showProgress by rememberSaveable { mutableStateOf(false) }
    var editorCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateStudySet by rememberSaveable { mutableStateOf(false) }
    var showDictionarySelection by rememberSaveable { mutableStateOf(false) }
    var practiceSetId by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceStartFullSet by rememberSaveable { mutableStateOf(false) }
    var practiceSessionNonce by remember { mutableIntStateOf(0) }
    var studySetSettingsId by rememberSaveable { mutableStateOf<String?>(null) }
    var addWordsToStudySetId by rememberSaveable { mutableStateOf<String?>(null) }
    var studioSetId by rememberSaveable { mutableStateOf<String?>(null) }
    var studioSetName by rememberSaveable { mutableStateOf("") }
    var testSetId by rememberSaveable { mutableStateOf<String?>(null) }
    var testFormat by rememberSaveable { mutableStateOf<String?>(null) }

    val selectionStore = remember { StudyWordSelectionStore() }
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.profconq.app.ProfconqApplication
    val studioTts = remember { com.profconq.app.studio.StudioTtsPlayer(app, app.profconqApiClient) }
    val studyScope = rememberCoroutineScope()

    val collections by viewModel.collections.collectAsState()
    val studySets by viewModel.studySets.collectAsState()
    val dictionaryCollections by viewModel.dictionaryCollections.collectAsState()
    val dictionary by viewModel.dictionary.collectAsState()
    val todayPlan by viewModel.todayPlan.collectAsState()
    val dailyActivities by viewModel.dailyActivities.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val totalCards = collections.sumOf { it.cards.size }
    val progress by viewModel.progress.collectAsState()
    val authUser by viewModel.authUser.collectAsState()
    val authBusy by viewModel.authBusy.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val cloudAccount by viewModel.cloudAccount.collectAsState()
    val syncBusy by viewModel.syncBusy.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val syncPrimary by viewModel.syncPrimary.collectAsState()
    val wordLimitMessage by viewModel.wordLimitMessage.collectAsState()
    val promoBusy by viewModel.promoBusy.collectAsState()
    val promoMessage by viewModel.promoMessage.collectAsState()
    val adminUsername by viewModel.adminUsername.collectAsState()
    val adminBusy by viewModel.adminBusy.collectAsState()
    val adminError by viewModel.adminError.collectAsState()
    val vocabularyWordCount = remember(collections, dictionary) {
        val ids = linkedSetOf<String>()
        collections.forEach { collection ->
            collection.cards.forEach { ids.add(it.id) }
        }
        dictionary.forEach { entry -> ids.add(entry.id) }
        ids.size
    }

    val editorCollection = editorCollectionId?.let { id -> collections.find { it.id == id } }
    val editorCard = editorCardId?.let { id -> editorCollection?.cards?.find { it.id == id } }
    val uiStrings = remember(settings.uiLanguage) {
        UiStrings(AppLanguage.fromStorage(settings.uiLanguage))
    }
    SideEffect {
        app.setUiLanguage(AppLanguage.fromStorage(settings.uiLanguage))
    }
    val studyLanguagePrefs = remember(
        settings.translationSourceLanguage,
        settings.translationTargetLanguage,
    ) {
        StudyLanguagePrefs(
            source = settings.translationSourceLanguage,
            target = settings.translationTargetLanguage,
        )
    }

    PortTheme(settings = settings) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .portScreenBackground(),
    ) {
    CompositionLocalProvider(
        LocalUiStrings provides uiStrings,
        LocalStudyLanguagePrefs provides studyLanguagePrefs,
    ) {
        val youtubeViewModel: YouTubeViewModel = viewModel(
            factory = YouTubeViewModelFactory(repository, authManager::getIdToken),
        )
        val youtubeState by youtubeViewModel.state.collectAsState()
        val studioNowPlaying by com.profconq.app.studio.StudioNowPlayingHub.state.collectAsState()
        val openStudioTab by com.profconq.app.studio.StudioNowPlayingHub.openStudio.collectAsState()
        val openVideoTab by com.profconq.app.youtube.YouTubeNowPlayingHub.openVideo.collectAsState()
        val openTabPending by com.profconq.app.ui.navigation.TabOpenHub.pending.collectAsState()
        val hasActiveVideo = youtubeState.videoId != null
        val youtubeBackground = settings.youtubeBackgroundPlayback
        val onVideoTab = activeTab == MainTab.Practice
        val onStudioTab = activeTab == MainTab.Studio
        androidx.compose.runtime.LaunchedEffect(openStudioTab) {
            if (openStudioTab) {
                activeTab = MainTab.Studio
                com.profconq.app.studio.StudioNowPlayingHub.consumeOpenStudio()
            }
        }
        androidx.compose.runtime.LaunchedEffect(openVideoTab) {
            if (openVideoTab) {
                activeTab = MainTab.Practice
                val pendingId = com.profconq.app.youtube.YouTubeNowPlayingHub.consumePendingVideoId()
                if (!pendingId.isNullOrBlank()) {
                    youtubeViewModel.loadVideo(pendingId)
                }
                com.profconq.app.youtube.YouTubeNowPlayingHub.consumeOpenVideo()
            }
        }
        androidx.compose.runtime.LaunchedEffect(openTabPending) {
            val tab = openTabPending ?: return@LaunchedEffect
            activeTab = tab
            com.profconq.app.ui.navigation.TabOpenHub.consume()
        }
        val persistentWatchPlayer = hasActiveVideo && (onVideoTab || youtubeBackground)
        val showCardEditor = editorCollectionId != null && editorCardId != null
        val showSetSettings = studySetSettingsId != null && !showDictionarySelection
        val showCreateSet = showCreateStudySet && !showDictionarySelection
        val showOverlay = showCardEditor || practiceSetId != null ||
            testSetId != null || showSetSettings || showCreateSet || showProgress

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PpBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                PortBottomNav(
                    activeTab = activeTab,
                    onTabSelected = { tab ->
                        editorCollectionId = null
                        editorCardId = null
                        studioSetId = null
                        studioSetName = ""
                        practiceSetId = null
                        practiceStartFullSet = false
                        testSetId = null
                        testFormat = null
                        studySetSettingsId = null
                        showCreateStudySet = false
                        showProgress = false
                        if (showDictionarySelection && tab != MainTab.Dictionary) {
                            showDictionarySelection = false
                            addWordsToStudySetId = null
                            selectionStore.clear()
                        }
                        activeTab = tab
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                showCardEditor -> CardEditorScreen(
                    collection = editorCollection,
                    card = editorCard,
                    onBack = {
                        editorCollectionId = null
                        editorCardId = null
                    },
                    onSave = viewModel::updateCard,
                    modifier = Modifier.fillMaxSize(),
                )
                practiceSetId != null -> key(
                    practiceSetId,
                    practiceSessionNonce,
                    practiceStartFullSet,
                ) {
                    PracticeSessionScreen(
                        setId = practiceSetId!!,
                        sessionKey = practiceSessionNonce,
                        startWithFullSet = practiceStartFullSet,
                        repository = repository,
                        onFinishLesson = {
                            practiceSetId = null
                            practiceStartFullSet = false
                            activeTab = MainTab.Study
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                testSetId != null -> {
                    val setId = testSetId!!
                    val setName = studySets.find { it.id == setId }?.name.orEmpty()
                    when (testFormat) {
                        "choice" -> key(setId, "choice") {
                            MultipleChoiceScreen(
                                setId = setId,
                                setName = setName,
                                repository = repository,
                                onBack = { testFormat = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        "match" -> key(setId, "match") {
                            MatchingScreen(
                                setId = setId,
                                setName = setName,
                                repository = repository,
                                onBack = { testFormat = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> TestFormatScreen(
                            setName = setName,
                            onBack = {
                                testSetId = null
                                testFormat = null
                            },
                            onChoose = { format ->
                                testFormat = when (format) {
                                    StudyTestFormat.Choice -> "choice"
                                    StudyTestFormat.Match -> "match"
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                showSetSettings -> StudySetSettingsScreen(
                    setId = studySetSettingsId!!,
                    repository = repository,
                    onBack = { studySetSettingsId = null },
                    onAddFromDictionary = {
                        selectionStore.clear()
                        addWordsToStudySetId = studySetSettingsId
                        showDictionarySelection = true
                        activeTab = MainTab.Dictionary
                    },
                    onSetLearnMark = viewModel::setCardLearnMark,
                    onOpenCardEditor = { collectionId, cardId ->
                        editorCollectionId = collectionId
                        editorCardId = cardId
                    },
                    onDeleteSet = {
                        val id = studySetSettingsId ?: return@StudySetSettingsScreen
                        viewModel.deleteStudySet(id)
                        studySetSettingsId = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                showCreateSet -> CreateStudySetScreen(
                    repository = repository,
                    selectionStore = selectionStore,
                    onBack = {
                        showCreateStudySet = false
                        selectionStore.clear()
                    },
                    onSelectWords = {
                        showDictionarySelection = true
                        activeTab = MainTab.Dictionary
                    },
                    onCreated = {
                        showCreateStudySet = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                showProgress -> ProgressScreen(
                    progress = progress,
                    onResetProgress = viewModel::resetProgressMetrics,
                    onBack = { showProgress = false },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> when (activeTab) {
                MainTab.Home -> HomeScreen(
                    todayPlan = todayPlan,
                    studySets = studySets,
                    studioCollectionCount = app.studioStore.loadCollections().size,
                    onContinueStudy = { activeTab = MainTab.Study },
                    onOpenTab = { activeTab = it },
                    modifier = Modifier.fillMaxSize(),
                )

                MainTab.Study -> StudyScreen(
                    studySets = studySets,
                    dailyActivities = dailyActivities,
                    onCreateSet = {
                        activeTab = MainTab.Study
                        showCreateStudySet = true
                    },
                    onOpenStudio = { setId ->
                        studyScope.launch {
                            val set = repository.getStudySet(setId)
                            val name = set?.name.orEmpty()
                            val ids = repository.getStudioWordIdsInStudySet(setId)
                            if (ids.isEmpty()) return@launch
                            val idSet = ids.toSet()
                            val collections = app.studioStore.loadCollections()
                            val existing = collections.find { it.wordIds.toSet() == idSet }
                                ?: collections.find { name.isNotBlank() && it.title == name }
                            if (existing != null) {
                                if (existing.wordIds.toSet() != idSet) {
                                    app.studioSyncService.updateCollectionWords(existing.id, ids)
                                }
                                app.studioStore.setActiveCollectionId(existing.id)
                            } else {
                                app.studioSyncService.createLocalCollection(
                                    name.ifBlank { uiStrings.tabStudio },
                                    ids,
                                )
                            }
                            studioSetName = name
                            studioSetId = setId
                            activeTab = MainTab.Studio
                        }
                    },
                    onOpenCards = { setId ->
                        activeTab = MainTab.Study
                        val set = studySets.find { it.id == setId }
                        practiceStartFullSet = set?.canStartSession == false
                        practiceSessionNonce++
                        practiceSetId = setId
                    },
                    onOpenTests = { setId ->
                        activeTab = MainTab.Study
                        testFormat = null
                        testSetId = setId
                    },
                    onOpenSetSettings = { setId ->
                        activeTab = MainTab.Study
                        studySetSettingsId = setId
                    },
                    onRenameSet = viewModel::renameStudySet,
                    onDeleteSet = viewModel::deleteStudySet,
                    modifier = Modifier.fillMaxSize(),
                )

                MainTab.Studio -> com.profconq.app.ui.studio.StudioScreen(
                    repository = repository,
                    store = app.studioStore,
                    syncService = app.studioSyncService,
                    tts = studioTts,
                    deckSetId = studioSetId.orEmpty().ifBlank { "studio_tab" },
                    deckTitle = studioSetName.ifBlank {
                        studySets.find { it.id == studioSetId }?.name.orEmpty()
                    },
                    onBack = null,
                    modifier = Modifier.fillMaxSize(),
                )

                MainTab.Reader -> ReaderScreen(
                    repository = repository,
                    authTokenProvider = authManager::getIdToken,
                    modifier = Modifier.fillMaxSize(),
                )

                MainTab.Practice -> {
                    if (!persistentWatchPlayer) {
                        YouTubeScreen(
                            repository = repository,
                            authTokenProvider = authManager::getIdToken,
                            viewModel = youtubeViewModel,
                            backgroundPlaybackEnabled = youtubeBackground || onVideoTab,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                MainTab.Dictionary -> DictionaryScreen(
                    collections = dictionaryCollections,
                    onCreateCollection = { title, description, onCreated ->
                        viewModel.createManualCollection(title, description, onCreated)
                    },
                    onUpdateCollectionTitle = viewModel::updateCollectionTitle,
                    onUpdateCollectionDescription = viewModel::updateCollectionDescription,
                    onDeleteCollection = viewModel::deleteCollection,
                    onAppendFolderAudio = viewModel::appendCollectionFolderAudio,
                    onSetFolderAudios = viewModel::setCollectionFolderAudios,
                    onSetFolderAudioLabel = viewModel::setCollectionFolderAudioLabel,
                    onAddCard = viewModel::addCardToCollection,
                    onUpdateCard = viewModel::updateCard,
                    onDeleteCard = viewModel::deleteCard,
                    onSetLearnMark = viewModel::setCardLearnMark,
                    onOpenCardEditor = { collectionId, cardId ->
                        editorCollectionId = collectionId
                        editorCardId = cardId
                    },
                    onBack = null,
                    selectionMode = showDictionarySelection,
                    selectionStore = selectionStore,
                    onSelectionDone = {
                        val targetSetId = addWordsToStudySetId
                        if (targetSetId != null) {
                            val ids = selectionStore.selectedIds.value
                            viewModel.addWordsToStudySet(targetSetId, ids)
                            selectionStore.clear()
                            addWordsToStudySetId = null
                            activeTab = MainTab.Study
                        }
                        showDictionarySelection = false
                    },
                    onSelectionCancel = {
                        if (addWordsToStudySetId != null) {
                            selectionStore.clear()
                            addWordsToStudySetId = null
                            activeTab = MainTab.Study
                        }
                        showDictionarySelection = false
                    },
                    wordLimitMessage = wordLimitMessage,
                    onDismissWordLimitMessage = viewModel::clearWordLimitMessage,
                    onSendToStudio = { ids ->
                        if (ids.isEmpty()) return@DictionaryScreen
                        studyScope.launch {
                            val name = uiStrings.dictionaryStudioCollection
                            val setId = repository.createStudySet(name, ids)
                            val set = repository.getStudySet(setId)
                            app.studioSyncService.createLocalCollection(
                                set?.name ?: name,
                                ids,
                            )
                            studioSetName = set?.name ?: name
                            studioSetId = setId
                            activeTab = MainTab.Studio
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                MainTab.Profile -> ProfileScreen(
                    todayPlan = todayPlan,
                    dictionaryCount = dictionary.size,
                    totalCards = totalCards,
                    settings = settings,
                    onOpenProgress = { showProgress = true },
                    onUseChatGptChange = viewModel::setUseChatGptTranslation,
                    onPhraseCopyChange = viewModel::setPhraseCopyEnabled,
                    onYoutubeBackgroundPlaybackChange = viewModel::setYoutubeBackgroundPlayback,
                    onWordContextExampleChange = viewModel::setWordContextExampleEnabled,
                    onSubtitleFontSizeChange = viewModel::setSubtitleFontSizeLevel,
                    onUiLanguageChange = viewModel::setUiLanguage,
                    onTranslationSourceLanguageChange = viewModel::setTranslationSourceLanguage,
                    onTranslationTargetLanguageChange = viewModel::setTranslationTargetLanguage,
                    onThemeModeChange = viewModel::setThemeMode,
                    authUser = authUser,
                    authBusy = authBusy,
                    authError = authError,
                    cloudAccount = cloudAccount,
                    localWordCount = vocabularyWordCount,
                    syncBusy = syncBusy,
                    syncMessage = syncMessage,
                    syncPrimary = syncPrimary,
                    promoBusy = promoBusy,
                    promoMessage = promoMessage,
                    onRedeemPromoCode = viewModel::redeemPromoCode,
                    onClearPromoMessage = viewModel::clearPromoMessage,
                    onSyncPrimaryChange = viewModel::setSyncPrimary,
                    onCreateGoogleSignInIntent = viewModel::createGoogleSignInIntent,
                    onGoogleSignInResult = viewModel::handleGoogleSignInResult,
                    onSignInWithEmail = viewModel::signInWithEmail,
                    onSignOut = viewModel::signOut,
                    onClearAuthError = viewModel::clearAuthError,
                    onMirrorSync = viewModel::runCloudMirrorSync,
                    onClearSyncMessage = viewModel::clearSyncMessage,
                    adminUsername = adminUsername,
                    adminBusy = adminBusy,
                    adminError = adminError,
                    onAdminSignIn = viewModel::signInAdmin,
                    onAdminSignOut = viewModel::signOutAdmin,
                    onClearAdminError = viewModel::clearAdminError,
                    modifier = Modifier.fillMaxSize(),
                )
                }
            }

            if (!showOverlay && persistentWatchPlayer) {
                YouTubeScreen(
                    repository = repository,
                    authTokenProvider = authManager::getIdToken,
                    viewModel = youtubeViewModel,
                    backgroundPlaybackEnabled = youtubeBackground || onVideoTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (onVideoTab) 1f else 0f)
                        .zIndex(if (onVideoTab) 1f else -1f),
                )
            }

            if (!showOverlay && (studioNowPlaying.live && !onStudioTab || hasActiveVideo && youtubeBackground && !onVideoTab)) {
                FloatingNowPlayingOverlay(
                    showStudio = studioNowPlaying.live && !onStudioTab,
                    studio = studioNowPlaying,
                    onOpenStudio = { activeTab = MainTab.Studio },
                    showYoutube = hasActiveVideo && youtubeBackground && !onVideoTab,
                    youtubeTitle = youtubeState.selectedVideoTitle,
                    onOpenYoutube = { activeTab = MainTab.Practice },
                    onDismissStudio = { com.profconq.app.studio.StudioNowPlayingHub.commands?.stop() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f),
                )
            }
            }
        }
    }
    }
    }
}
