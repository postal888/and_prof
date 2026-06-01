package com.profconq.app.data.repository

import com.profconq.app.data.local.AppStateDao
import com.profconq.app.data.local.AppStateEntity
import com.profconq.app.data.local.CardEntity
import com.profconq.app.data.local.CollectionDao
import com.profconq.app.data.local.CollectionEntity
import com.profconq.app.data.local.CollectionWithCards
import com.profconq.app.data.local.DictionaryDao
import com.profconq.app.data.local.DictionaryEntity
import com.profconq.app.data.local.DailyActivityDao
import com.profconq.app.data.local.DailyActivityEntity
import com.profconq.app.data.local.ReaderBookDao
import com.profconq.app.data.local.ReaderBookEntity
import com.profconq.app.data.local.ReviewStateDao
import com.profconq.app.data.local.StudySetDao
import com.profconq.app.data.local.StudySetEntity
import com.profconq.app.data.local.StudySetWordEntity
import com.profconq.app.study.SrsEngine
import com.profconq.app.data.local.YouTubeWatchHistoryDao
import com.profconq.app.data.local.YouTubeWatchHistoryEntity
import com.profconq.app.data.model.StudySet
import com.profconq.app.data.model.ReaderBook
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.SubtitleLanguage
import com.profconq.app.ui.i18n.UiStrings
import com.profconq.app.data.model.AppSettings
import com.profconq.app.data.model.AppThemeMode
import com.profconq.app.data.model.SubtitleFontSize
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.DictionaryEntry
import com.profconq.app.data.model.ProgressSnapshot
import com.profconq.app.data.model.ReaderLineSpacing
import com.profconq.app.reader.ReaderFontSize
import com.profconq.app.data.model.TodayPlan
import com.profconq.app.progress.ProgressCalculator
import com.profconq.app.progress.ProgressKeys
import com.profconq.app.progress.ProgressTracker
import com.profconq.app.media.AudioLabelsCodec
import com.profconq.app.media.AudioPathsCodec
import com.profconq.app.api.DictionarySyncWord
import com.profconq.app.api.SyncPrimary
import com.profconq.app.api.WebVocabWord
import org.json.JSONArray
import org.json.JSONObject
import com.profconq.app.data.WordLimitPolicy
import com.profconq.app.data.WordLimitReachedException
import com.profconq.app.data.model.WordCard
import com.profconq.app.reader.ReaderDemoBooks
import com.profconq.app.youtube.YouTubeWatchHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

object WordNormalizer {
    fun normalize(text: String): String =
        text.trim()
            .trim(',', '.', '!', '?', ';', ':', '"', '\'', ')', '»', '…', '—', '(', '«')
            .lowercase()
}

object SyncKeys {
    const val DICTIONARY_SINCE = "sync.dictionary_since"
    const val IS_PREMIUM = "account.is_premium"
    const val WORD_LIMIT = "account.word_limit"
    const val SYNC_PRIMARY = "sync.primary"
}

object AppSettingsKeys {
    const val CHATGPT_TRANSLATION = "settings.chatgpt"
    const val DICTIONARY_COPY = "settings.dict_copy"
    const val WORD_CONTEXT_EXAMPLE = "settings.word_context"
    const val SUBTITLE_FONT = "settings.subtitle_font"
    const val UI_LANGUAGE = "settings.ui_language"
    const val SUBTITLE_LANGUAGE = "settings.subtitle_language"
    const val TRANSLATION_TARGET_LANGUAGE = "settings.translation_target_language"
    const val READER_LINE_SPACING = "settings.reader_line_spacing"
    const val READER_FONT = "settings.reader_font"
    const val THEME_MODE = "settings.theme_mode"
}

class ProfconqRepository(
    private val collectionDao: CollectionDao,
    private val studySetDao: StudySetDao,
    private val reviewStateDao: ReviewStateDao,
    private val dictionaryDao: DictionaryDao,
    private val appStateDao: AppStateDao,
    private val readerBookDao: ReaderBookDao,
    private val dailyActivityDao: DailyActivityDao,
    private val youtubeWatchHistoryDao: YouTubeWatchHistoryDao,
) {
    val collections: Flow<List<Collection>> =
        collectionDao.observeCollectionsWithCards().map { rows -> rows.map { it.toModel() } }

    val studySets: Flow<List<StudySet>> =
        studySetDao.observeAllSets().map { rows ->
            val now = System.currentTimeMillis()
            rows.map { entity ->
                val stats = reviewStateDao.getSetStats(entity.id, now)
                entity.toStudySet(
                    newCount = stats.newCount,
                    dueCount = stats.dueCount,
                    masteredCount = stats.masteredCount,
                )
            }
        }.flowOn(Dispatchers.IO)

    val dictionaryCollections: Flow<List<Collection>> =
        collections.map { cols ->
            cols.filter {
                it.sourceType == "youtube" ||
                    it.sourceType == "reader" ||
                    it.sourceType == "manual" ||
                    it.sourceType == "sync" ||
                    it.videoId != null
            }
                .sortedByDescending { it.cards.maxOfOrNull { card -> card.id.hashCode() } ?: 0 }
        }

    val readerBooks: Flow<List<ReaderBook>> =
        readerBookDao.observeBooks().map { rows -> rows.map { it.toModel() } }

    val youtubeWatchHistory: Flow<List<YouTubeWatchHistoryItem>> =
        youtubeWatchHistoryDao.observeRecent().map { rows -> rows.map { it.toModel() } }

    val dictionary: Flow<List<DictionaryEntry>> =
        dictionaryDao.observeDictionary().map { rows ->
            rows.map {
                DictionaryEntry(
                    id = it.id,
                    pt = it.pt,
                    ru = it.ru,
                    example = it.example,
                    collectionId = it.collectionId,
                    videoId = it.videoId,
                    addedAt = it.addedAt,
                )
            }
        }

    val settings: Flow<AppSettings> = combine(
        combine(
            appStateDao.observeInt(AppSettingsKeys.CHATGPT_TRANSLATION),
            appStateDao.observeInt(AppSettingsKeys.DICTIONARY_COPY),
            appStateDao.observeInt(AppSettingsKeys.SUBTITLE_FONT),
            appStateDao.observeInt(AppSettingsKeys.WORD_CONTEXT_EXAMPLE),
        ) { chatGpt, dictCopy, subtitleFont, wordContext ->
            Triple(chatGpt, dictCopy, subtitleFont to wordContext)
        },
        combine(
            combine(
                appStateDao.observeInt(AppSettingsKeys.UI_LANGUAGE),
                appStateDao.observeInt(AppSettingsKeys.SUBTITLE_LANGUAGE),
                appStateDao.observeInt(AppSettingsKeys.TRANSLATION_TARGET_LANGUAGE),
            ) { uiLanguage, sourceLang, targetLang ->
                Triple(uiLanguage, sourceLang, targetLang)
            },
            combine(
                appStateDao.observeInt(AppSettingsKeys.READER_LINE_SPACING),
                appStateDao.observeInt(AppSettingsKeys.READER_FONT),
                appStateDao.observeInt(AppSettingsKeys.THEME_MODE),
            ) { readerLineSpacing, readerFont, themeMode ->
                Triple(readerLineSpacing, readerFont, themeMode)
            },
        ) { studyLangs, readerPrefs ->
            val (uiLanguage, sourceLang, targetLang) = studyLangs
            val (readerLineSpacing, readerFont, themeMode) = readerPrefs
            Triple(uiLanguage, sourceLang to targetLang, Triple(readerLineSpacing, readerFont, themeMode))
        },
    ) { base, localeAndReader ->
        val (chatGpt, dictCopy, subtitlePrefs) = base
        val (subtitleFont, wordContext) = subtitlePrefs
        val (uiLanguage, studyLangs, readerPrefs) = localeAndReader
        val (sourceLang, targetLang) = studyLangs
        val (readerLineSpacing, readerFont, themeMode) = readerPrefs
        val subtitleLevel = (subtitleFont ?: SubtitleFontSize.DEFAULT_LEVEL)
            .coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)
        AppSettings(
            useChatGptTranslation = (chatGpt ?: 1) != 0,
            phraseCopyEnabled = (dictCopy ?: 1) != 0,
            wordContextExampleEnabled = (wordContext ?: 1) != 0,
            themeMode = AppThemeMode.fromStorage(themeMode),
            subtitleFontSizeLevel = subtitleLevel,
            readerFontSizeLevel = (readerFont ?: ReaderFontSize.fromSubtitleLevel(subtitleLevel))
                .coerceIn(ReaderFontSize.MIN_LEVEL, ReaderFontSize.MAX_LEVEL),
            uiLanguage = uiLanguage ?: AppLanguage.EN.storageCode,
            translationSourceLanguage = SubtitleLanguage.fromStorage(sourceLang),
            translationTargetLanguage = SubtitleLanguage.fromStorage(targetLang ?: SubtitleLanguage.RU),
            readerLineSpacingPercent = (readerLineSpacing ?: ReaderLineSpacing.DEFAULT_PERCENT)
                .coerceIn(ReaderLineSpacing.MIN_PERCENT, ReaderLineSpacing.MAX_PERCENT),
        )
    }

    suspend fun setUseChatGptTranslation(enabled: Boolean) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.CHATGPT_TRANSLATION,
                intValue = if (enabled) 1 else 0,
            ),
        )
    }

    suspend fun setPhraseCopyEnabled(enabled: Boolean) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.DICTIONARY_COPY,
                intValue = if (enabled) 1 else 0,
            ),
        )
    }

    suspend fun setWordContextExampleEnabled(enabled: Boolean) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.WORD_CONTEXT_EXAMPLE,
                intValue = if (enabled) 1 else 0,
            ),
        )
    }

    suspend fun setSubtitleFontSizeLevel(level: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.SUBTITLE_FONT,
                intValue = level.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL),
            ),
        )
    }

    suspend fun setUiLanguage(languageCode: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.UI_LANGUAGE,
                intValue = languageCode.coerceIn(0, 2),
            ),
        )
    }

    suspend fun setTranslationSourceLanguage(languageCode: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.SUBTITLE_LANGUAGE,
                intValue = SubtitleLanguage.fromStorage(languageCode),
            ),
        )
    }

    suspend fun setTranslationTargetLanguage(languageCode: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.TRANSLATION_TARGET_LANGUAGE,
                intValue = SubtitleLanguage.fromStorage(languageCode),
            ),
        )
    }

    suspend fun setThemeMode(mode: com.profconq.app.data.model.AppThemeMode) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.THEME_MODE,
                intValue = mode.storageCode,
            ),
        )
    }

    suspend fun setReaderFontSizeLevel(level: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.READER_FONT,
                intValue = level.coerceIn(ReaderFontSize.MIN_LEVEL, ReaderFontSize.MAX_LEVEL),
            ),
        )
    }

    suspend fun setReaderLineSpacingPercent(percent: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.READER_LINE_SPACING,
                intValue = percent.coerceIn(ReaderLineSpacing.MIN_PERCENT, ReaderLineSpacing.MAX_PERCENT),
            ),
        )
    }

    fun observeSavedWordsForVideo(videoId: String): Flow<Set<String>> =
        collectionDao.observeCollectionsWithCards().map { rows ->
            rows
                .filter { it.collection.videoId == videoId }
                .flatMap { it.cards }
                .map { WordNormalizer.normalize(it.pt) }
                .filter { it.isNotEmpty() }
                .toSet()
        }

    fun observeSavedWordsForBook(bookId: String): Flow<Set<String>> =
        collectionDao.observeCollectionsWithCards().map { rows ->
            rows
                .filter { it.collection.id == readerCollectionId(bookId) }
                .flatMap { it.cards }
                .map { WordNormalizer.normalize(it.pt) }
                .filter { it.isNotEmpty() }
                .toSet()
        }

    suspend fun importReaderBook(title: String, content: String, sourceUri: String? = null): String {
        val strings = uiStrings()
        val cleanTitle = title.trim().ifBlank { strings.untitled }
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) throw IllegalStateException(strings.readerEmptyImportFile)

        val bookId = "book-${System.currentTimeMillis()}"
        readerBookDao.upsert(
            ReaderBookEntity(
                id = bookId,
                title = cleanTitle,
                content = cleanContent,
                sourceUri = sourceUri,
            ),
        )
        return bookId
    }

    suspend fun deleteReaderBook(bookId: String) {
        if (bookId == ReaderDemoBooks.DEMO_ID) return
        readerBookDao.deleteBook(bookId)
        collectionDao.deleteCollection(readerCollectionId(bookId))
    }

    suspend fun updateReaderScrollParagraph(bookId: String, paragraph: Int) {
        readerBookDao.updateScrollParagraph(bookId, paragraph.coerceAtLeast(0))
    }

    suspend fun setReaderBookmark(bookId: String, paragraph: Int) {
        readerBookDao.updateBookmark(bookId, paragraph.coerceAtLeast(0))
    }

    suspend fun clearReaderBookmark(bookId: String) {
        readerBookDao.updateBookmark(bookId, null)
    }

    suspend fun getReaderBook(bookId: String): ReaderBook? =
        readerBookDao.getBook(bookId)?.toModel()

    val todayPlan: Flow<TodayPlan> = combine(collections, appStateDao.observeInt(ProgressKeys.STREAK)) { cols, streak ->
        val cards = cols.flatMap { it.cards }
        TodayPlan(
            dueCount = cards.count { it.due },
            newCount = cards.count { !it.known },
            streak = streak ?: 0,
        )
    }

    val progressSnapshot: Flow<ProgressSnapshot> = combine(
        combine(
            collections,
            dictionary,
            dailyActivityDao.observeAll(),
        ) { cols, dict, activities ->
            Triple(cols, dict, activities)
        },
        combine(
            appStateDao.observeInt(ProgressKeys.STREAK),
            appStateDao.observeInt(ProgressKeys.BEST_STREAK),
            appStateDao.observeLong(ProgressKeys.RESET_AT),
        ) { streak, bestStreak, resetAt ->
            Triple(streak, bestStreak, resetAt)
        },
        combine(
            combine(
                appStateDao.observeInt(ProgressKeys.DAILY_GOAL),
                appStateDao.observeInt(ProgressKeys.WEEKLY_CARDS_GOAL),
                appStateDao.observeInt(ProgressKeys.WEEKLY_YOUTUBE_GOAL_MIN),
            ) { dailyGoal, weeklyCardsGoal, weeklyYoutubeGoal ->
                Triple(dailyGoal, weeklyCardsGoal, weeklyYoutubeGoal)
            },
            combine(
                appStateDao.observeInt(ProgressKeys.WEEKLY_READING_GOAL),
                appStateDao.observeInt(ProgressKeys.WEEKLY_SPEECH_GOAL),
                appStateDao.observeInt(ProgressKeys.WEEKLY_WRITING_GOAL),
            ) { weeklyReadingGoal, weeklySpeechGoal, weeklyWritingGoal ->
                Triple(weeklyReadingGoal, weeklySpeechGoal, weeklyWritingGoal)
            },
        ) { goalsPrimary, goalsSecondary ->
            ProgressGoals(
                dailyGoal = goalsPrimary.first,
                weeklyCardsGoal = goalsPrimary.second,
                weeklyYoutubeGoal = goalsPrimary.third,
                weeklyReadingGoal = goalsSecondary.first,
                weeklySpeechGoal = goalsSecondary.second,
                weeklyWritingGoal = goalsSecondary.third,
            )
        },
    ) { data, streakData, goals ->
        val (cols, dict, activities) = data
        val (streak, bestStreak, resetAt) = streakData

        ProgressCalculator.calculate(
            ProgressCalculator.ProgressInputs(
                collections = cols,
                dictionary = dict,
                dailyActivities = activities,
                streak = streak ?: 0,
                bestStreak = bestStreak ?: 0,
                resetAt = resetAt ?: 0L,
                dailyGoalTarget = goals.dailyGoal ?: 50,
                weeklyCardsGoal = goals.weeklyCardsGoal ?: 250,
                weeklyYoutubeGoalMin = goals.weeklyYoutubeGoal ?: 210,
                weeklyReadingGoal = goals.weeklyReadingGoal ?: 5000,
                weeklySpeechGoal = goals.weeklySpeechGoal ?: 5,
                weeklyWritingGoal = goals.weeklyWritingGoal ?: 3,
            ),
        )
    }

    suspend fun markCardKnown(cardId: String) {
        collectionDao.updateCardProgress(cardId, due = false, known = true)
        val stats = cardStats()
        ProgressTracker.recordCardStudied(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
    }

    suspend fun markCardLearning(cardId: String) {
        collectionDao.updateCardProgress(cardId, due = false, known = false)
    }

    suspend fun markCardRepeat(cardId: String) {
        collectionDao.updateCardProgress(cardId, due = true, known = false)
        val stats = cardStats()
        ProgressTracker.recordCardStudied(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
    }

    suspend fun recordYouTubeSeconds(seconds: Int) {
        val stats = cardStats()
        ProgressTracker.recordYouTubeSeconds(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            seconds = seconds,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
    }

    suspend fun recordReaderSession(wordsRead: Int, minutes: Int) {
        val stats = cardStats()
        ProgressTracker.recordReaderWords(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            words = wordsRead,
            minutes = minutes,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
    }

    suspend fun recordYouTubeWatch(
        videoId: String,
        title: String,
        channel: String = "",
        thumbnailUrl: String? = null,
        duration: String? = null,
        isShort: Boolean = false,
        positionSec: Float = 0f,
    ) {
        val existing = youtubeWatchHistoryDao.getByVideoId(videoId)
        val now = System.currentTimeMillis()
        youtubeWatchHistoryDao.upsert(
            YouTubeWatchHistoryEntity(
                videoId = videoId,
                title = title.ifBlank { existing?.title ?: "YouTube $videoId" },
                channel = channel.ifBlank { existing?.channel.orEmpty() },
                thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
                duration = duration ?: existing?.duration,
                isShort = isShort,
                lastWatchedAt = now,
                lastPositionSec = positionSec.coerceAtLeast(0f),
                watchCount = (existing?.watchCount ?: 0) + 1,
            ),
        )
    }

    suspend fun updateYouTubeWatchPosition(videoId: String, positionSec: Float) {
        val existing = youtubeWatchHistoryDao.getByVideoId(videoId) ?: return
        youtubeWatchHistoryDao.upsert(
            existing.copy(
                lastWatchedAt = System.currentTimeMillis(),
                lastPositionSec = positionSec.coerceAtLeast(0f),
            ),
        )
    }

    suspend fun deleteYouTubeWatchHistoryItem(videoId: String) {
        youtubeWatchHistoryDao.delete(videoId)
    }

    suspend fun clearYouTubeWatchHistory() {
        youtubeWatchHistoryDao.deleteAll()
    }

    suspend fun resetProgressMetrics() {
        dailyActivityDao.deleteAll()
        appStateDao.upsert(AppStateEntity(key = ProgressKeys.STREAK, intValue = 0))
        appStateDao.upsert(AppStateEntity(key = ProgressKeys.BEST_STREAK, intValue = 0))
        appStateDao.deleteKey(ProgressKeys.LAST_ACTIVE)
        appStateDao.upsert(
            AppStateEntity(
                key = ProgressKeys.RESET_AT,
                longValue = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteCollection(collectionId: String) {
        collectionDao.deleteCollection(collectionId)
    }

    suspend fun deleteCard(cardId: String) {
        collectionDao.deleteCard(cardId)
    }

    suspend fun updateCollectionTitle(collectionId: String, title: String) {
        collectionDao.updateCollectionTitle(collectionId, title.trim())
    }

    suspend fun updateCollectionDescription(collectionId: String, description: String?) {
        collectionDao.updateCollectionDescription(
            collectionId,
            description?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    suspend fun createManualCollection(title: String, description: String? = null): String {
        val id = "manual-${System.currentTimeMillis()}"
        collectionDao.insertCollection(
            CollectionEntity(
                id = id,
                title = title.trim().ifBlank { uiStrings().dictionaryNewFolder },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                sourceType = "manual",
            ),
        )
        return id
    }

    suspend fun addCardToCollection(
        collectionId: String,
        pt: String,
        ru: String,
        example: String? = null,
    ): String {
        val trimmedPt = pt.trim()
        val trimmedRu = ru.trim()
        require(trimmedPt.isNotEmpty() && trimmedRu.isNotEmpty()) {
            "Word and translation are required"
        }
        ensureCanAddNewWord(trimmedPt, enforceLimit = true)
        val normalized = WordNormalizer.normalize(trimmedPt)
        val cardId = "manual-card-$normalized-${System.currentTimeMillis()}"
        collectionDao.insertCards(
            listOf(
                CardEntity(
                    id = cardId,
                    collectionId = collectionId,
                    pt = trimmedPt,
                    ru = trimmedRu,
                    example = example?.trim()?.takeIf { it.isNotEmpty() },
                    due = true,
                    known = false,
                ),
            ),
        )
        dictionaryDao.insert(
            DictionaryEntity(
                id = "dict-$normalized-${System.currentTimeMillis()}",
                pt = trimmedPt,
                ru = trimmedRu,
                example = example?.trim()?.takeIf { it.isNotEmpty() },
                collectionId = collectionId,
            ),
        )
        val stats = cardStats()
        ProgressTracker.recordDictionaryWordAdded(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
        return cardId
    }

    suspend fun updateCollectionFolderAudio(collectionId: String, path: String?) {
        val paths = path?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
        saveCollectionFolderAudios(collectionId, paths)
    }

    suspend fun appendCollectionFolderAudio(collectionId: String, path: String) {
        if (path.isBlank()) return
        val entity = collectionDao.getCollection(collectionId) ?: return
        val current = folderAudioPathsFrom(entity)
        if (path in current) return
        saveCollectionFolderAudios(collectionId, current + path)
    }

    suspend fun setCollectionFolderAudios(
        collectionId: String,
        paths: List<String>,
        pathReplacements: Map<String, String> = emptyMap(),
    ) {
        saveCollectionFolderAudios(collectionId, paths, pathReplacements)
    }

    suspend fun setCollectionFolderAudioLabel(collectionId: String, path: String, label: String) {
        val entity = collectionDao.getCollection(collectionId) ?: return
        val labels = AudioLabelsCodec.decode(entity.folderAudioLabels).toMutableMap()
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            labels.remove(path)
        } else {
            labels[path] = trimmed
        }
        collectionDao.updateCollectionFolderAudioLabels(
            collectionId = collectionId,
            labelsJson = AudioLabelsCodec.encode(labels),
        )
    }

    private suspend fun saveCollectionFolderAudios(
        collectionId: String,
        paths: List<String>,
        pathReplacements: Map<String, String> = emptyMap(),
    ) {
        val entity = collectionDao.getCollection(collectionId)
        var labels = entity?.let { AudioLabelsCodec.decode(it.folderAudioLabels) } ?: emptyMap()
        labels = AudioLabelsCodec.migrate(labels, pathReplacements)
        val cleaned = paths.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        labels = AudioLabelsCodec.retainOnly(labels, cleaned)
        collectionDao.updateCollectionFolderAudios(
            collectionId = collectionId,
            pathsJson = AudioPathsCodec.encode(cleaned),
            legacyPath = cleaned.firstOrNull(),
            labelsJson = AudioLabelsCodec.encode(labels),
        )
    }

    suspend fun updateCard(card: WordCard) {
        val existing = collectionDao.getCardsForCollection(card.collectionId)
            .firstOrNull { it.id == card.id }
        val audioPaths = card.resolvedAudioPaths()
        val audioLegacy = audioPaths.firstOrNull()
        val audioJson = AudioPathsCodec.encode(audioPaths)
        val audioLabelsJson = AudioLabelsCodec.encode(
            AudioLabelsCodec.retainOnly(card.audioLabels, audioPaths),
        )
        collectionDao.updateCardContent(
            cardId = card.id,
            pt = card.pt.trim(),
            ru = card.ru.trim(),
            example = card.example?.trim()?.takeIf { it.isNotEmpty() },
            imagePath = card.imagePath,
            audioPath = audioLegacy,
            audioPathsJson = audioJson,
            audioLabelsJson = audioLabelsJson,
            imageUrl = card.imageUrl,
            partOfSpeech = card.partOfSpeech,
            ipa = card.ipa,
            sourceTitle = card.sourceTitle,
            chapterOrTag = card.chapterOrTag,
            exampleTranslation = card.exampleTranslation,
            isFavorite = card.isFavorite,
        )
        val hadAudio = existing?.let { cardAudioPathsFrom(it).isNotEmpty() } == true
        if (audioPaths.isNotEmpty() && !hadAudio) {
            val stats = cardStats()
            ProgressTracker.recordSpeechRecording(
                dailyActivityDao = dailyActivityDao,
                appStateDao = appStateDao,
                cardsKnown = stats.first,
                totalCards = stats.second,
            )
        }
    }

    suspend fun addWordFromYouTube(
        videoId: String,
        videoTitle: String,
        pt: String,
        ru: String,
        example: String?,
        enforceLimit: Boolean = true,
    ): String {
        val collectionId = ensureYouTubeCollection(videoId, videoTitle)
        val normalized = WordNormalizer.normalize(pt)

        val existing = collectionDao.getCardsForCollection(collectionId)
            .firstOrNull { WordNormalizer.normalize(it.pt) == normalized }
        if (existing != null) {
            collectionDao.updateCardContent(
                cardId = existing.id,
                pt = pt.trim(),
                ru = ru.trim(),
                example = example?.trim()?.takeIf { it.isNotEmpty() },
                imagePath = existing.imagePath,
                audioPath = existing.audioPath,
                imageUrl = existing.imageUrl,
                partOfSpeech = existing.partOfSpeech,
                ipa = existing.ipa,
                sourceTitle = existing.sourceTitle,
                chapterOrTag = existing.chapterOrTag,
                exampleTranslation = existing.exampleTranslation,
                isFavorite = existing.isFavorite,
            )
            return existing.id
        }

        ensureCanAddNewWord(pt, enforceLimit = enforceLimit)

        val cardId = "yt-card-$normalized-${System.currentTimeMillis()}"
        collectionDao.insertCards(
            listOf(
                CardEntity(
                    id = cardId,
                    collectionId = collectionId,
                    pt = pt.trim(),
                    ru = ru.trim(),
                    example = example?.trim()?.takeIf { it.isNotEmpty() },
                    due = true,
                    known = false,
                ),
            ),
        )

        dictionaryDao.insert(
            DictionaryEntity(
                id = "dict-$normalized-${System.currentTimeMillis()}",
                pt = pt.trim(),
                ru = ru.trim(),
                example = example?.trim()?.takeIf { it.isNotEmpty() },
                collectionId = collectionId,
                videoId = videoId,
            ),
        )
        val stats = cardStats()
        ProgressTracker.recordDictionaryWordAdded(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
        return cardId
    }

    suspend fun addWordFromReader(
        bookId: String,
        bookTitle: String,
        pt: String,
        ru: String,
        example: String?,
    ): String {
        val collectionId = ensureReaderCollection(bookId, bookTitle)
        val normalized = WordNormalizer.normalize(pt)

        val existing = collectionDao.getCardsForCollection(collectionId)
            .firstOrNull { WordNormalizer.normalize(it.pt) == normalized }
        if (existing != null) {
            collectionDao.updateCardContent(
                cardId = existing.id,
                pt = pt.trim(),
                ru = ru.trim(),
                example = example?.trim()?.takeIf { it.isNotEmpty() },
                imagePath = existing.imagePath,
                audioPath = existing.audioPath,
                imageUrl = existing.imageUrl,
                partOfSpeech = existing.partOfSpeech,
                ipa = existing.ipa,
                sourceTitle = existing.sourceTitle,
                chapterOrTag = existing.chapterOrTag,
                exampleTranslation = existing.exampleTranslation,
                isFavorite = existing.isFavorite,
            )
            return existing.id
        }

        ensureCanAddNewWord(pt, enforceLimit = true)

        val cardId = "book-card-$normalized-${System.currentTimeMillis()}"
        collectionDao.insertCards(
            listOf(
                CardEntity(
                    id = cardId,
                    collectionId = collectionId,
                    pt = pt.trim(),
                    ru = ru.trim(),
                    example = example?.trim()?.takeIf { it.isNotEmpty() },
                    due = true,
                    known = false,
                ),
            ),
        )

        dictionaryDao.insert(
            DictionaryEntity(
                id = "dict-$normalized-${System.currentTimeMillis()}",
                pt = pt.trim(),
                ru = ru.trim(),
                example = example?.trim()?.takeIf { it.isNotEmpty() },
                collectionId = collectionId,
            ),
        )
        val stats = cardStats()
        ProgressTracker.recordDictionaryWordAdded(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
        return cardId
    }

    suspend fun ensureReaderCollection(bookId: String, bookTitle: String): String {
        val strings = uiStrings()
        val collectionId = readerCollectionId(bookId)
        val existing = collectionDao.getCollection(collectionId)
        if (existing == null) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = collectionId,
                    title = bookTitle.ifBlank { strings.readerDefaultBookTitle },
                    description = strings.wordsFromReader,
                    sourceType = "reader",
                ),
            )
        } else if (bookTitle.isNotBlank() && existing.title != bookTitle) {
            collectionDao.updateCollectionTitle(collectionId, bookTitle)
        }
        return collectionId
    }

    suspend fun ensureYouTubeCollection(videoId: String, videoTitle: String): String {
        val strings = uiStrings()
        val collectionId = youtubeCollectionId(videoId)
        val existing = collectionDao.getCollection(collectionId)
        if (existing == null) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = collectionId,
                    title = videoTitle.ifBlank { "YouTube $videoId" },
                    description = strings.wordsFromVideo,
                    videoId = videoId,
                    sourceType = "youtube",
                ),
            )
        } else if (videoTitle.isNotBlank() && existing.title.startsWith("YouTube ")) {
            collectionDao.updateCollectionTitle(collectionId, videoTitle)
        }
        return collectionId
    }

    suspend fun addDictionaryEntry(pt: String, ru: String, example: String? = null) {
        val trimmedPt = pt.trim()
        ensureCanAddNewWord(trimmedPt, enforceLimit = true)
        val id = "dict-${WordNormalizer.normalize(trimmedPt).hashCode()}-${System.currentTimeMillis()}"
        dictionaryDao.insert(
            DictionaryEntity(
                id = id,
                pt = pt.trim(),
                ru = ru.trim(),
                example = example?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        val stats = cardStats()
        ProgressTracker.recordDictionaryWordAdded(
            dailyActivityDao = dailyActivityDao,
            appStateDao = appStateDao,
            cardsKnown = stats.first,
            totalCards = stats.second,
        )
    }

    fun observeWordsInSet(setId: String): Flow<List<WordCard>> =
        studySetDao.observeCardsInSet(setId).map { cards -> cards.map { it.toModel() } }

    suspend fun getDueWordsForStudySet(setId: String, now: Long = System.currentTimeMillis()): List<WordCard> =
        reviewStateDao.getDueCardsForSet(setId, now).map { it.toModel() }

    suspend fun getAllWordsForStudySet(setId: String): List<WordCard> =
        studySetDao.observeCardsInSet(setId).first().map { it.toModel() }

    suspend fun recordReviewKnown(wordId: String, now: Long = System.currentTimeMillis()) {
        val state = reviewStateDao.get(wordId)
        reviewStateDao.upsert(SrsEngine.onKnown(state, wordId, now))
    }

    suspend fun recordReviewAgain(wordId: String, now: Long = System.currentTimeMillis()) {
        val state = reviewStateDao.get(wordId)
        reviewStateDao.upsert(SrsEngine.onAgain(state, wordId, now))
    }

    suspend fun getStudySetReviewStats(setId: String, now: Long = System.currentTimeMillis()) =
        reviewStateDao.getSetStats(setId, now)

    suspend fun createStudySet(name: String, wordIds: List<String>): String {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val distinctIds = wordIds.distinct()
        studySetDao.createSetWithWords(
            set = StudySetEntity(
                id = id,
                name = name.trim().ifBlank { defaultStudySetName() },
                createdAt = now,
                wordCount = distinctIds.size,
            ),
            words = distinctIds.map { wordId ->
                StudySetWordEntity(studySetId = id, wordId = wordId, addedAt = now)
            },
        )
        return id
    }

    suspend fun addWordsToStudySet(setId: String, wordIds: List<String>) {
        val now = System.currentTimeMillis()
        val existing = studySetDao.getWordIdsInSet(setId).toSet()
        val newIds = wordIds.filter { it !in existing }
        if (newIds.isEmpty()) return
        studySetDao.insertWords(
            newIds.map { StudySetWordEntity(studySetId = setId, wordId = it, addedAt = now) },
        )
        val total = studySetDao.getWordIdsInSet(setId).size
        studySetDao.updateWordCount(setId, total)
    }

    suspend fun removeWordFromStudySet(setId: String, wordId: String) {
        studySetDao.removeWordFromSet(setId, wordId)
        val total = studySetDao.getWordIdsInSet(setId).size
        studySetDao.updateWordCount(setId, total)
    }

    /** After a practice run, keep only words marked «Не знаю» in the study set. */
    suspend fun getWordIdsInStudySet(setId: String): List<String> =
        studySetDao.getWordIdsInSet(setId)

    suspend fun retainOnlyWordsInStudySet(setId: String, keepWordIds: Iterable<String>) {
        val keep = keepWordIds.toSet()
        val current = studySetDao.getWordIdsInSet(setId)
        current.filter { it !in keep }.forEach { wordId ->
            studySetDao.removeWordFromSet(setId, wordId)
        }
        studySetDao.updateWordCount(setId, keep.size)
    }

    suspend fun deleteStudySet(setId: String) {
        studySetDao.deleteSet(setId)
    }

    suspend fun markStudySetPracticed(setId: String) {
        studySetDao.updateLastPracticed(setId, System.currentTimeMillis())
    }

    suspend fun getCardsByIds(wordIds: List<String>): List<WordCard> {
        if (wordIds.isEmpty()) return emptyList()
        val byId = collectionDao.getCardsByIds(wordIds).associateBy { it.id }
        return wordIds.mapNotNull { byId[it]?.toModel() }
    }

    private suspend fun defaultStudySetName(): String {
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        return uiStrings().defaultStudySetName(formatter.format(java.util.Date()))
    }

    fun youtubeCollectionId(videoId: String) = "yt-$videoId"

    fun readerCollectionId(bookId: String) = "book-$bookId"

    private fun CollectionWithCards.toModel(): Collection {
        val folderPaths = folderAudioPathsFrom(collection)
        return Collection(
            id = collection.id,
            title = collection.title,
            description = collection.description,
            videoId = collection.videoId,
            sourceType = collection.sourceType,
            folderAudioPath = folderPaths.firstOrNull(),
            folderAudioPaths = folderPaths,
            folderAudioLabels = AudioLabelsCodec.decode(collection.folderAudioLabels),
            cards = cards.map { it.toModel() },
        )
    }

    private fun StudySetEntity.toStudySet(
        newCount: Int = 0,
        dueCount: Int = 0,
        masteredCount: Int = 0,
    ): StudySet =
        StudySet(
            id = id,
            name = name,
            createdAt = createdAt,
            lastPracticedAt = lastPracticedAt,
            wordCount = wordCount,
            newCount = newCount,
            dueCount = dueCount,
            masteredCount = masteredCount,
        )

    private fun CardEntity.toModel(): WordCard {
        val paths = cardAudioPathsFrom(this)
        return WordCard(
            id = id,
            collectionId = collectionId,
            pt = pt,
            ru = ru,
            example = example,
            due = due,
            known = known,
            imagePath = imagePath,
            audioPath = paths.firstOrNull(),
            audioPaths = paths,
            audioLabels = AudioLabelsCodec.decode(audioLabels),
            imageUrl = imageUrl,
            partOfSpeech = partOfSpeech,
            ipa = ipa,
            sourceTitle = sourceTitle,
            chapterOrTag = chapterOrTag,
            exampleTranslation = exampleTranslation,
            isFavorite = isFavorite,
        )
    }

    private fun folderAudioPathsFrom(entity: CollectionEntity): List<String> =
        AudioPathsCodec.merge(entity.folderAudioPath, entity.folderAudioPaths)

    private fun cardAudioPathsFrom(entity: CardEntity): List<String> =
        AudioPathsCodec.merge(entity.audioPath, entity.audioPaths)

    suspend fun getStudySet(setId: String): StudySet? =
        studySetDao.getSet(setId)?.toStudySet()

    suspend fun toggleCardFavorite(cardId: String) {
        val card = collectionDao.getCardsByIds(listOf(cardId)).firstOrNull() ?: return
        collectionDao.updateCardFavorite(cardId, !card.isFavorite)
    }

    private fun ReaderBookEntity.toModel(): ReaderBook =
        ReaderBook(
            id = id,
            title = title,
            content = content,
            sourceUri = sourceUri,
            scrollParagraph = scrollParagraph,
            bookmarkParagraph = bookmarkParagraph,
            addedAt = addedAt,
        )

    private fun YouTubeWatchHistoryEntity.toModel(): YouTubeWatchHistoryItem =
        YouTubeWatchHistoryItem(
            videoId = videoId,
            title = title,
            channel = channel,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            isShort = isShort,
            lastWatchedAt = lastWatchedAt,
            lastPositionSec = lastPositionSec,
            watchCount = watchCount,
        )

    private suspend fun cardStats(): Pair<Int, Int> {
        val cards = collectionDao.getCollections()
            .flatMap { collectionDao.getCardsForCollection(it.id) }
        return cards.count { it.known } to cards.size
    }

    suspend fun getDictionarySyncSince(): Long =
        appStateDao.getLong(SyncKeys.DICTIONARY_SINCE) ?: 0L

    suspend fun setDictionarySyncSince(value: Long) {
        appStateDao.upsert(AppStateEntity(key = SyncKeys.DICTIONARY_SINCE, longValue = value))
    }

    suspend fun isPremiumUser(): Boolean = appStateDao.getInt(SyncKeys.IS_PREMIUM) == 1

    suspend fun setPremiumUser(premium: Boolean) {
        appStateDao.upsert(AppStateEntity(key = SyncKeys.IS_PREMIUM, intValue = if (premium) 1 else 0))
    }

    suspend fun getWordLimit(): Int =
        appStateDao.getInt(SyncKeys.WORD_LIMIT) ?: WordLimitPolicy.FREE_LIMIT

    suspend fun setWordLimit(limit: Int) {
        appStateDao.upsert(AppStateEntity(key = SyncKeys.WORD_LIMIT, intValue = limit))
    }

    suspend fun resetAccountLimits() {
        setPremiumUser(false)
        setWordLimit(WordLimitPolicy.FREE_LIMIT)
    }

    private suspend fun effectiveWordLimit(): Int {
        if (isPremiumUser()) return WordLimitPolicy.UNLIMITED
        return getWordLimit().coerceAtLeast(WordLimitPolicy.FREE_LIMIT)
    }

    suspend fun getSyncPrimary(): SyncPrimary =
        SyncPrimary.fromStored(appStateDao.getInt(SyncKeys.SYNC_PRIMARY) ?: 0)

    suspend fun setSyncPrimary(primary: SyncPrimary) {
        appStateDao.upsert(AppStateEntity(key = SyncKeys.SYNC_PRIMARY, intValue = primary.toStored()))
    }

    suspend fun mirrorReplaceLocalFromWeb(words: List<WebVocabWord>) {
        for (collection in collectionDao.getCollections()) {
            collectionDao.deleteCollection(collection.id)
        }
        dictionaryDao.deleteAll()
        studySetDao.deleteAllSets()
        applyWebVocabularyWords(words)
    }

    suspend fun exportProgramsForWebsite(vocabWords: List<WebVocabWord>): JSONArray {
        val ptToWebId = vocabWords.associateBy { WordNormalizer.normalize(it.word) }
            .mapValues { it.value.id }
        val programs = JSONArray()
        var nextId = 1
        val today = java.time.LocalDate.now().toString()
        for (set in studySetDao.getAllSets()) {
            val webIds = JSONArray()
            for (cardId in studySetDao.getWordIdsInSet(set.id)) {
                val card = collectionDao.getCardsByIds(listOf(cardId)).firstOrNull() ?: continue
                val webId = ptToWebId[WordNormalizer.normalize(card.pt)] ?: continue
                webIds.put(webId)
            }
            if (webIds.length() == 0) continue
            programs.put(
                JSONObject()
                    .put("id", nextId++)
                    .put("name", set.name)
                    .put("startDate", today)
                    .put("dayCount", 7)
                    .put("runsPerDay", 3)
                    .put("wordIds", webIds),
            )
        }
        return programs
    }

    suspend fun mirrorImportProgramsFromWebsite(programsJson: JSONArray) {
        studySetDao.deleteAllSets()
        val webIdToPt = buildWebVocabularyWords().associate { it.id to it.word }
        for (index in 0 until programsJson.length()) {
            val program = programsJson.optJSONObject(index) ?: continue
            val name = program.optString("name").ifBlank { "Program" }
            val webWordIds = mutableListOf<Int>()
            program.optJSONArray("wordIds")?.let { arr ->
                for (j in 0 until arr.length()) webWordIds.add(arr.optInt(j))
            }
            if (webWordIds.isEmpty()) {
                program.optJSONObject("schedule")?.let { schedule ->
                    val seen = mutableSetOf<Int>()
                    val dayKeys = schedule.keys()
                    while (dayKeys.hasNext()) {
                        val dayKey = dayKeys.next()
                        schedule.optJSONArray(dayKey)?.let { dayArr ->
                            for (j in 0 until dayArr.length()) {
                                val id = dayArr.optInt(j)
                                if (seen.add(id)) webWordIds.add(id)
                            }
                        }
                    }
                }
            }
            val cardIds = webWordIds.mapNotNull { webId ->
                val pt = webIdToPt[webId] ?: return@mapNotNull null
                findCardIdByNormalizedPt(WordNormalizer.normalize(pt))
            }
            if (cardIds.isEmpty()) continue
            createStudySet(name, cardIds)
        }
    }

    private suspend fun findCardIdByNormalizedPt(normalizedPt: String): String? {
        for (collection in collectionDao.getCollections()) {
            val card = collectionDao.getCardsForCollection(collection.id)
                .firstOrNull { WordNormalizer.normalize(it.pt) == normalizedPt }
            if (card != null) return card.id
        }
        return null
    }

    suspend fun countVocabularyWords(): Int = buildWebVocabularyWords().size

    private suspend fun hasVocabularyWord(normalizedPt: String): Boolean {
        if (normalizedPt.isEmpty()) return false
        for (collection in collectionDao.getCollections()) {
            val exists = collectionDao.getCardsForCollection(collection.id)
                .any { WordNormalizer.normalize(it.pt) == normalizedPt }
            if (exists) return true
        }
        return dictionaryDao.getAll().any { WordNormalizer.normalize(it.pt) == normalizedPt }
    }

    suspend fun buildWebVocabularyWords(): List<WebVocabWord> {
        val result = mutableListOf<WebVocabWord>()
        var nextId = 1
        for (collection in collectionDao.getCollections()) {
            for (card in collectionDao.getCardsForCollection(collection.id)) {
                result.add(card.toWebVocabWord(collection, nextId++))
            }
        }
        val cardPts = result.map { WordNormalizer.normalize(it.word) }.toSet()
        for (entry in dictionaryDao.getAll()) {
            val normalized = WordNormalizer.normalize(entry.pt)
            if (normalized in cardPts) continue
            result.add(
                WebVocabWord(
                    id = nextId++,
                    word = entry.pt,
                    translation = entry.ru,
                    example = entry.example.orEmpty(),
                    tag = "geral",
                    videoId = entry.videoId,
                ),
            )
        }
        return result
    }

    suspend fun applyWebVocabularyWords(words: List<WebVocabWord>) {
        for (word in words) {
            if (!word.videoId.isNullOrBlank()) {
                addWordFromYouTube(
                    videoId = word.videoId,
                    videoTitle = word.videoTitle.orEmpty(),
                    pt = word.word,
                    ru = word.translation,
                    example = word.example.takeIf { it.isNotBlank() },
                    enforceLimit = false,
                )
            } else {
                addWordToCloudCollection(
                    pt = word.word,
                    ru = word.translation,
                    example = word.example.takeIf { it.isNotBlank() },
                    tag = word.tag,
                    infinitivo = word.infinitivo,
                )
            }
        }
    }

    private suspend fun addWordToCloudCollection(
        pt: String,
        ru: String,
        example: String?,
        tag: String,
        infinitivo: String,
    ) {
        val normalized = WordNormalizer.normalize(pt)
        for (collection in collectionDao.getCollections()) {
            val existing = collectionDao.getCardsForCollection(collection.id)
                .firstOrNull { WordNormalizer.normalize(it.pt) == normalized }
            if (existing != null) {
                collectionDao.updateCardContent(
                    cardId = existing.id,
                    pt = pt.trim(),
                    ru = ru.trim(),
                    example = example ?: existing.example,
                    imagePath = existing.imagePath,
                    audioPath = existing.audioPath,
                    audioPathsJson = existing.audioPaths,
                    audioLabelsJson = existing.audioLabels,
                    imageUrl = existing.imageUrl,
                    partOfSpeech = tag.ifBlank { existing.partOfSpeech.orEmpty() },
                    ipa = existing.ipa,
                    sourceTitle = existing.sourceTitle,
                    chapterOrTag = infinitivo.takeIf { it.isNotBlank() } ?: existing.chapterOrTag,
                    exampleTranslation = existing.exampleTranslation,
                    isFavorite = existing.isFavorite,
                )
                return
            }
        }
        val collectionId = ensureCloudSyncCollection()
        val existing = collectionDao.getCardsForCollection(collectionId)
            .firstOrNull { WordNormalizer.normalize(it.pt) == normalized }
        if (existing != null) {
            collectionDao.updateCardContent(
                cardId = existing.id,
                pt = pt.trim(),
                ru = ru.trim(),
                example = example,
                imagePath = existing.imagePath,
                audioPath = existing.audioPath,
                audioPathsJson = existing.audioPaths,
                audioLabelsJson = existing.audioLabels,
                imageUrl = existing.imageUrl,
                partOfSpeech = tag,
                ipa = existing.ipa,
                sourceTitle = existing.sourceTitle,
                chapterOrTag = infinitivo.takeIf { it.isNotBlank() },
                exampleTranslation = existing.exampleTranslation,
                isFavorite = existing.isFavorite,
            )
            return
        }
        ensureCanAddNewWord(pt, enforceLimit = false)
        collectionDao.insertCards(
            listOf(
                CardEntity(
                    id = "cloud-${normalized}-${System.currentTimeMillis()}",
                    collectionId = collectionId,
                    pt = pt.trim(),
                    ru = ru.trim(),
                    example = example,
                    due = true,
                    known = false,
                    partOfSpeech = tag,
                    chapterOrTag = infinitivo.takeIf { it.isNotBlank() },
                ),
            ),
        )
    }

    private suspend fun uiStrings(): UiStrings {
        val code = appStateDao.getInt(AppSettingsKeys.UI_LANGUAGE)
        return UiStrings.forLanguage(AppLanguage.fromStorage(code))
    }

    private suspend fun ensureCloudSyncCollection(): String {
        val id = "cloud-sync"
        val strings = uiStrings()
        if (collectionDao.getCollection(id) == null) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = id,
                    title = strings.syncCollectionTitle,
                    description = strings.syncCollectionDescription,
                    sourceType = "sync",
                ),
            )
        }
        return id
    }

    private suspend fun ensureCanAddNewWord(pt: String, enforceLimit: Boolean) {
        if (!enforceLimit || isPremiumUser()) return
        val normalized = WordNormalizer.normalize(pt)
        if (hasVocabularyWord(normalized)) return
        val count = countVocabularyWords()
        val limit = effectiveWordLimit()
        if (count >= limit) {
            throw WordLimitReachedException(count, limit)
        }
    }

    suspend fun buildDictionarySyncPayload(): List<DictionarySyncWord> {
        val now = System.currentTimeMillis()
        val byId = linkedMapOf<String, DictionarySyncWord>()
        for (collection in collectionDao.getCollections()) {
            for (card in collectionDao.getCardsForCollection(collection.id)) {
                byId[card.id] = DictionarySyncWord(
                    id = card.id,
                    pt = card.pt,
                    ru = card.ru,
                    example = card.example,
                    collectionId = collection.id,
                    videoId = collection.videoId,
                    updatedAt = now,
                )
            }
        }
        for (entry in dictionaryDao.getAll()) {
            if (entry.id in byId) continue
            byId[entry.id] = DictionarySyncWord(
                id = entry.id,
                pt = entry.pt,
                ru = entry.ru,
                example = entry.example,
                collectionId = entry.collectionId,
                videoId = entry.videoId,
                updatedAt = entry.addedAt.takeIf { it > 0 } ?: now,
            )
        }
        return byId.values.toList()
    }

    suspend fun applyPulledDictionaryWords(words: List<DictionarySyncWord>) {
        for (word in words) {
            if (word.deleted) {
                dictionaryDao.deleteById(word.id)
                continue
            }
            dictionaryDao.insert(
                DictionaryEntity(
                    id = word.id,
                    pt = word.pt,
                    ru = word.ru,
                    example = word.example,
                    collectionId = word.collectionId,
                    videoId = word.videoId,
                    addedAt = word.updatedAt,
                ),
            )
            val cards = collectionDao.getCardsByIds(listOf(word.id))
            val card = cards.firstOrNull() ?: continue
            collectionDao.updateCardContent(
                cardId = card.id,
                pt = word.pt,
                ru = word.ru,
                example = word.example,
                imagePath = card.imagePath,
                audioPath = card.audioPath,
                audioPathsJson = card.audioPaths,
                audioLabelsJson = card.audioLabels,
                imageUrl = card.imageUrl,
                partOfSpeech = card.partOfSpeech,
                ipa = card.ipa,
                sourceTitle = card.sourceTitle,
                chapterOrTag = card.chapterOrTag,
                exampleTranslation = card.exampleTranslation,
                isFavorite = card.isFavorite,
            )
        }
    }

    private data class ProgressGoals(
        val dailyGoal: Int?,
        val weeklyCardsGoal: Int?,
        val weeklyYoutubeGoal: Int?,
        val weeklyReadingGoal: Int?,
        val weeklySpeechGoal: Int?,
        val weeklyWritingGoal: Int?,
    )

    private fun CardEntity.toWebVocabWord(collection: CollectionEntity, id: Int): WebVocabWord =
        WebVocabWord(
            id = id,
            word = pt,
            translation = ru,
            example = example.orEmpty(),
            tag = partOfSpeech.orEmpty().ifBlank { "geral" },
            infinitivo = chapterOrTag.orEmpty(),
            img = imageUrl.orEmpty().ifBlank { imagePath.orEmpty() },
            videoId = collection.videoId,
            videoTitle = collection.title,
        )
}
