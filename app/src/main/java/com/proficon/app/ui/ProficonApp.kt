package com.proficon.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proficon.app.data.repository.ProficonRepository
import com.proficon.app.ui.components.PortBottomNav
import com.proficon.app.ui.i18n.AppLanguage
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.i18n.UiStrings
import com.proficon.app.ui.navigation.MainTab
import com.proficon.app.ui.screens.dictionary.CardEditorScreen
import com.proficon.app.ui.screens.dictionary.DictionaryScreen
import com.proficon.app.ui.screens.home.HomeScreen
import com.proficon.app.ui.screens.profile.ProfileScreen
import com.proficon.app.ui.screens.progress.ProgressScreen
import com.proficon.app.ui.screens.reader.ReaderScreen
import com.proficon.app.ui.screens.study.StudyScreen
import com.proficon.app.ui.screens.youtube.YouTubeScreen
import com.proficon.app.ui.theme.PpBg
import com.proficon.app.ui.theme.PortTheme
import com.proficon.app.youtube.YouTubeViewModel
import com.proficon.app.youtube.YouTubeViewModelFactory

@Composable
fun ProficonApp(
    viewModel: MainViewModel,
    repository: ProficonRepository,
) {
    var activeTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showProgress by rememberSaveable { mutableStateOf(false) }
    var editorCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorCardId by rememberSaveable { mutableStateOf<String?>(null) }

    val collections by viewModel.collections.collectAsState()
    val dictionaryCollections by viewModel.dictionaryCollections.collectAsState()
    val dictionary by viewModel.dictionary.collectAsState()
    val todayPlan by viewModel.todayPlan.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val totalCards = collections.sumOf { it.cards.size }
    val youtubeViewModel: YouTubeViewModel = viewModel(factory = YouTubeViewModelFactory(repository))
    val progress by viewModel.progress.collectAsState()

    val editorCollection = editorCollectionId?.let { id -> collections.find { it.id == id } }
    val editorCard = editorCardId?.let { id -> editorCollection?.cards?.find { it.id == id } }
    val uiStrings = remember(settings.uiLanguage) {
        UiStrings(AppLanguage.fromStorage(settings.uiLanguage))
    }

    PortTheme(settings = settings) {
    CompositionLocalProvider(LocalUiStrings provides uiStrings) {
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

        if (showProgress) {
            ProgressScreen(
                progress = progress,
                onOpenStudy = {
                    showProgress = false
                    activeTab = MainTab.Study
                },
                onOpenPractice = {
                    showProgress = false
                    activeTab = MainTab.Practice
                },
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
                    onTabSelected = { activeTab = it },
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
                    collections = collections,
                    onMarkKnown = viewModel::markKnown,
                    onMarkRepeat = viewModel::markRepeat,
                    modifier = Modifier.padding(padding),
                )

                MainTab.Reader -> ReaderScreen(
                    repository = repository,
                    modifier = Modifier.padding(padding),
                )

                MainTab.Practice -> YouTubeScreen(
                    repository = repository,
                    viewModel = youtubeViewModel,
                    modifier = Modifier.padding(padding),
                )

                MainTab.Dictionary -> DictionaryScreen(
                    collections = dictionaryCollections,
                    onCreateCollection = { title, description, onCreated ->
                        viewModel.createManualCollection(title, description, onCreated)
                    },
                    onUpdateCollectionTitle = viewModel::updateCollectionTitle,
                    onUpdateCollectionDescription = viewModel::updateCollectionDescription,
                    onDeleteCollection = viewModel::deleteCollection,
                    onUpdateFolderAudio = viewModel::updateCollectionFolderAudio,
                    onAddCard = viewModel::addCardToCollection,
                    onUpdateCard = viewModel::updateCard,
                    onDeleteCard = viewModel::deleteCard,
                    onSetLearnMark = viewModel::setCardLearnMark,
                    onOpenCardEditor = { collectionId, cardId ->
                        editorCollectionId = collectionId
                        editorCardId = cardId
                    },
                    onBack = null,
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
                    onSubtitleLanguageChange = viewModel::setSubtitleLanguage,
                    onThemeModeChange = viewModel::setThemeMode,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
    }
}
