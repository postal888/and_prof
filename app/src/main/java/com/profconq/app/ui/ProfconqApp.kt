package com.profconq.app.ui

import com.profconq.app.auth.FirebaseAuthManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.model.AppThemeMode
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.components.PortBottomNav
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
import com.profconq.app.ui.study.PracticeSessionScreen
import com.profconq.app.ui.study.StudySetSettingsScreen
import com.profconq.app.ui.study.StudyWordSelectionStore
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

    val selectionStore = remember { StudyWordSelectionStore() }

    val collections by viewModel.collections.collectAsState()
    val studySets by viewModel.studySets.collectAsState()
    val dictionaryCollections by viewModel.dictionaryCollections.collectAsState()
    val dictionary by viewModel.dictionary.collectAsState()
    val todayPlan by viewModel.todayPlan.collectAsState()
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
    val screenBg = when (settings.themeMode) {
        AppThemeMode.Light -> LightPortPalette.bg
        AppThemeMode.Dark -> DarkPortPalette.bg
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg),
    ) {
    CompositionLocalProvider(
        LocalUiStrings provides uiStrings,
        LocalStudyLanguagePrefs provides studyLanguagePrefs,
    ) {
        if (editorCollectionId != null && editorCardId != null) {
            CardEditorScreen(
                collection = editorCollection,
                card = editorCard,
                onBack = {
                    editorCollectionId = null
                    editorCardId = null
                },
                onSave = viewModel::updateCard,
                modifier = Modifier.fillMaxSize(),
            )
            return@CompositionLocalProvider
        }

        practiceSetId?.let { setId ->
            key(setId, practiceSessionNonce, practiceStartFullSet) {
                PracticeSessionScreen(
                    setId = setId,
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
            return@CompositionLocalProvider
        }

        studySetSettingsId?.let { setId ->
            if (!showDictionarySelection) {
                StudySetSettingsScreen(
                    setId = setId,
                    repository = repository,
                    onBack = { studySetSettingsId = null },
                    onAddFromDictionary = {
                        selectionStore.clear()
                        addWordsToStudySetId = setId
                        showDictionarySelection = true
                        activeTab = MainTab.Dictionary
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                return@CompositionLocalProvider
            }
        }

        if (showCreateStudySet && !showDictionarySelection) {
            CreateStudySetScreen(
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
            return@CompositionLocalProvider
        }

        if (showProgress) {
            ProgressScreen(
                progress = progress,
                onResetProgress = viewModel::resetProgressMetrics,
                onBack = { showProgress = false },
                modifier = Modifier.fillMaxSize(),
            )
            return@CompositionLocalProvider
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PpBg,
            bottomBar = {
                PortBottomNav(
                    activeTab = activeTab,
                    onTabSelected = { tab ->
                        if (showDictionarySelection && tab != MainTab.Dictionary) {
                            showDictionarySelection = false
                        }
                        activeTab = tab
                    },
                )
            },
        ) { padding ->
            when (activeTab) {
                MainTab.Home -> HomeScreen(
                    todayPlan = todayPlan,
                    collections = collections,
                    onContinueStudy = { activeTab = MainTab.Study },
                    onOpenTab = { activeTab = it },
                    onOpenDictionary = { activeTab = MainTab.Dictionary },
                    onDeleteCollection = viewModel::deleteCollection,
                    modifier = Modifier.padding(padding),
                )

                MainTab.Study -> StudyScreen(
                    studySets = studySets,
                    onCreateSet = {
                        activeTab = MainTab.Study
                        showCreateStudySet = true
                    },
                    onOpenSet = { setId ->
                        activeTab = MainTab.Study
                        val set = studySets.find { it.id == setId }
                        practiceStartFullSet = set?.canStartSession == false
                        practiceSessionNonce++
                        practiceSetId = setId
                    },
                    onOpenSetSettings = { setId ->
                        activeTab = MainTab.Study
                        studySetSettingsId = setId
                    },
                    modifier = Modifier.padding(padding),
                )

                MainTab.Reader -> ReaderScreen(
                    repository = repository,
                    authTokenProvider = authManager::getIdToken,
                    modifier = Modifier.padding(padding),
                )

                MainTab.Practice -> {
                    val youtubeViewModel: YouTubeViewModel = viewModel(
                        factory = YouTubeViewModelFactory(repository, authManager::getIdToken),
                    )
                    YouTubeScreen(
                        repository = repository,
                        authTokenProvider = authManager::getIdToken,
                        viewModel = youtubeViewModel,
                        modifier = Modifier.padding(padding),
                    )
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
                    modifier = Modifier.padding(padding),
                )

                MainTab.Profile -> ProfileScreen(
                    todayPlan = todayPlan,
                    dictionaryCount = dictionary.size,
                    totalCards = totalCards,
                    settings = settings,
                    onOpenProgress = { showProgress = true },
                    onUseChatGptChange = viewModel::setUseChatGptTranslation,
                    onPhraseCopyChange = viewModel::setPhraseCopyEnabled,
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
                    onSignOut = viewModel::signOut,
                    onClearAuthError = viewModel::clearAuthError,
                    onMirrorSync = viewModel::runCloudMirrorSync,
                    onClearSyncMessage = viewModel::clearSyncMessage,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
    }
    }
}
