package com.proficon.app.data.repository

import com.proficon.app.data.local.AppStateDao
import com.proficon.app.data.local.AppStateEntity
import com.proficon.app.data.local.CardEntity
import com.proficon.app.data.local.CollectionDao
import com.proficon.app.data.local.CollectionEntity
import com.proficon.app.data.local.CollectionWithCards
import com.proficon.app.data.local.DictionaryDao
import com.proficon.app.data.local.DictionaryEntity
import com.proficon.app.data.local.DailyActivityDao
import com.proficon.app.data.local.DailyActivityEntity
import com.proficon.app.data.local.ReaderBookDao
import com.proficon.app.data.local.ReaderBookEntity
import com.proficon.app.data.local.YouTubeWatchHistoryDao
import com.proficon.app.data.local.YouTubeWatchHistoryEntity
import com.proficon.app.data.model.ReaderBook
import com.proficon.app.data.model.AppSettings
import com.proficon.app.data.model.AppThemeMode
import com.proficon.app.data.model.SubtitleFontSize
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.DictionaryEntry
import com.proficon.app.data.model.ProgressSnapshot
import com.proficon.app.data.model.ReaderLineSpacing
import com.proficon.app.reader.ReaderFontSize
import com.proficon.app.data.model.TodayPlan
import com.proficon.app.progress.ProgressCalculator
import com.proficon.app.progress.ProgressKeys
import com.proficon.app.progress.ProgressTracker
import com.proficon.app.data.model.WordCard
import com.proficon.app.reader.ReaderDemoBooks
import com.proficon.app.youtube.YouTubeWatchHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object WordNormalizer {
    fun normalize(text: String): String =
        text.trim()
            .trim(',', '.', '!', '?', ';', ':', '"', '\'', ')', '»', '…', '—', '(', '«')
            .lowercase()
}

object AppSettingsKeys {
    const val CHATGPT_TRANSLATION = "settings.chatgpt"
    const val DICTIONARY_COPY = "settings.dict_copy"
    const val WORD_CONTEXT_EXAMPLE = "settings.word_context"
    const val SUBTITLE_FONT = "settings.subtitle_font"
    const val UI_LANGUAGE = "settings.ui_language"
    const val SUBTITLE_LANGUAGE = "settings.subtitle_language"
    const val READER_LINE_SPACING = "settings.reader_line_spacing"
    const val READER_FONT = "settings.reader_font"
    const val THEME_MODE = "settings.theme_mode"
}

class ProficonRepository(
    private val collectionDao: CollectionDao,
    private val dictionaryDao: DictionaryDao,
    private val appStateDao: AppStateDao,
    private val readerBookDao: ReaderBookDao,
    private val dailyActivityDao: DailyActivityDao,
    private val youtubeWatchHistoryDao: YouTubeWatchHistoryDao,
) {
    val collections: Flow<List<Collection>> =
        collectionDao.observeCollectionsWithCards().map { rows -> rows.map { it.toModel() } }

    val dictionaryCollections: Flow<List<Collection>> =
        collections.map { cols ->
            cols.filter {
                it.sourceType == "youtube" ||
                    it.sourceType == "reader" ||
                    it.sourceType == "manual" ||
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
            appStateDao.observeInt(AppSettingsKeys.UI_LANGUAGE),
            appStateDao.observeInt(AppSettingsKeys.SUBTITLE_LANGUAGE),
            appStateDao.observeInt(AppSettingsKeys.READER_LINE_SPACING),
            appStateDao.observeInt(AppSettingsKeys.READER_FONT),
            appStateDao.observeInt(AppSettingsKeys.THEME_MODE),
        ) { uiLanguage, subtitleLanguage, readerLineSpacing, readerFont, themeMode ->
            Triple(uiLanguage, subtitleLanguage, Triple(readerLineSpacing, readerFont, themeMode))
        },
    ) { base, localeAndReader ->
        val (chatGpt, dictCopy, subtitlePrefs) = base
        val (subtitleFont, wordContext) = subtitlePrefs
        val (uiLanguage, subtitleLanguage, readerPrefs) = localeAndReader
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
            uiLanguage = uiLanguage ?: 0,
            subtitleLanguage = subtitleLanguage ?: 0,
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

    suspend fun setSubtitleLanguage(languageCode: Int) {
        appStateDao.upsert(
            AppStateEntity(
                key = AppSettingsKeys.SUBTITLE_LANGUAGE,
                intValue = languageCode.coerceIn(0, 3),
            ),
        )
    }

    suspend fun setThemeMode(mode: com.proficon.app.data.model.AppThemeMode) {
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

    suspend fun ensureDemoReaderBook() {
        if (readerBookDao.getBook(ReaderDemoBooks.DEMO_ID) != null) return
        readerBookDao.upsert(
            ReaderBookEntity(
                id = ReaderDemoBooks.DEMO_ID,
                title = ReaderDemoBooks.demoTitle,
                content = ReaderDemoBooks.demoContent,
            ),
        )
    }

    suspend fun ensurePreinstalledReaderBook(
        bookId: String,
        title: String,
        content: String,
        sourceUri: String? = null,
        forceUpdate: Boolean = false,
    ) {
        if (!forceUpdate && readerBookDao.getBook(bookId) != null) return
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return
        readerBookDao.upsert(
            ReaderBookEntity(
                id = bookId,
                title = title.trim().ifBlank { "Без названия" },
                content = cleanContent,
                sourceUri = sourceUri,
            ),
        )
    }

    suspend fun importReaderBook(title: String, content: String, sourceUri: String? = null): String {
        val cleanTitle = title.trim().ifBlank { "Без названия" }
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty()) { "Файл пустой или не удалось прочитать текст." }

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
                title = title.trim().ifBlank { "Новая папка" },
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
        collectionDao.updateCollectionFolderAudio(collectionId, path)
    }

    suspend fun updateCard(card: WordCard) {
        val existing = collectionDao.getCardsForCollection(card.collectionId)
            .firstOrNull { it.id == card.id }
        collectionDao.updateCardContent(
            cardId = card.id,
            pt = card.pt.trim(),
            ru = card.ru.trim(),
            example = card.example?.trim()?.takeIf { it.isNotEmpty() },
            imagePath = card.imagePath,
            audioPath = card.audioPath,
        )
        if (!card.audioPath.isNullOrBlank() && existing?.audioPath.isNullOrBlank()) {
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
            )
            return existing.id
        }

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
            )
            return existing.id
        }

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
        val collectionId = readerCollectionId(bookId)
        val existing = collectionDao.getCollection(collectionId)
        if (existing == null) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = collectionId,
                    title = bookTitle.ifBlank { "Книга" },
                    description = "Слова из книги",
                    sourceType = "reader",
                ),
            )
        } else if (bookTitle.isNotBlank() && existing.title != bookTitle) {
            collectionDao.updateCollectionTitle(collectionId, bookTitle)
        }
        return collectionId
    }

    suspend fun ensureYouTubeCollection(videoId: String, videoTitle: String): String {
        val collectionId = youtubeCollectionId(videoId)
        val existing = collectionDao.getCollection(collectionId)
        if (existing == null) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = collectionId,
                    title = videoTitle.ifBlank { "YouTube $videoId" },
                    description = "Слова из видео",
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
        val id = "dict-${WordNormalizer.normalize(pt).hashCode()}-${System.currentTimeMillis()}"
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

    fun youtubeCollectionId(videoId: String) = "yt-$videoId"

    fun readerCollectionId(bookId: String) = "book-$bookId"

    private fun CollectionWithCards.toModel(): Collection =
        Collection(
            id = collection.id,
            title = collection.title,
            description = collection.description,
            videoId = collection.videoId,
            sourceType = collection.sourceType,
            folderAudioPath = collection.folderAudioPath,
            cards = cards.map { it.toModel() },
        )

    private fun CardEntity.toModel(): WordCard =
        WordCard(
            id = id,
            collectionId = collectionId,
            pt = pt,
            ru = ru,
            example = example,
            due = due,
            known = known,
            imagePath = imagePath,
            audioPath = audioPath,
        )

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

    private data class ProgressGoals(
        val dailyGoal: Int?,
        val weeklyCardsGoal: Int?,
        val weeklyYoutubeGoal: Int?,
        val weeklyReadingGoal: Int?,
        val weeklySpeechGoal: Int?,
        val weeklyWritingGoal: Int?,
    )
}
