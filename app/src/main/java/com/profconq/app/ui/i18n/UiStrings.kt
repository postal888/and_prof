package com.profconq.app.ui.i18n

import com.profconq.app.data.model.ReaderAutoScroll
import com.profconq.app.data.model.SubtitleFontSize

class UiStrings(private val language: AppLanguage) {
    val tabHome: String
        get() = when (language) {
            AppLanguage.RU -> "Главная"
            AppLanguage.EN -> "Home"
            AppLanguage.PT -> "Início"
        }

    val tabStudy: String
        get() = when (language) {
            AppLanguage.RU -> "Учёба"
            AppLanguage.EN -> "Study"
            AppLanguage.PT -> "Estudo"
        }

    val tabStudio: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val tabRead: String
        get() = when (language) {
            AppLanguage.RU -> "Читалка"
            AppLanguage.EN -> "Read"
            AppLanguage.PT -> "Leitura"
        }

    val tabVideo: String
        get() = when (language) {
            AppLanguage.RU -> "Видео"
            AppLanguage.EN -> "Video"
            AppLanguage.PT -> "Vídeo"
        }

    val tabDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "Словарь"
            AppLanguage.EN -> "Dictionary"
            AppLanguage.PT -> "Dicionário"
        }

    val tabProgress: String
        get() = when (language) {
            AppLanguage.RU -> "Прогресс"
            AppLanguage.EN -> "Progress"
            AppLanguage.PT -> "Progresso"
        }

    val profileOpenProgress: String
        get() = when (language) {
            AppLanguage.RU -> "Прогресс и статистика"
            AppLanguage.EN -> "Progress & stats"
            AppLanguage.PT -> "Progresso e estatísticas"
        }

    val tabProfile: String
        get() = when (language) {
            AppLanguage.RU -> "Профиль"
            AppLanguage.EN -> "Profile"
            AppLanguage.PT -> "Perfil"
        }

    val youtubeScreenTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Видео"
            AppLanguage.EN -> "Video"
            AppLanguage.PT -> "Vídeo"
        }

    val youtubeScreenSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск видео и субтитры"
            AppLanguage.EN -> "Video search and subtitles"
            AppLanguage.PT -> "Busca de vídeos e legendas"
        }

    val searchOnYoutube: String
        get() = when (language) {
            AppLanguage.RU -> "Искать видео"
            AppLanguage.EN -> "Search videos"
            AppLanguage.PT -> "Buscar vídeos"
        }

    val searchPlaceholder: String = "português A1, gramática, CELPE…"

    val searchAction: String
        get() = when (language) {
            AppLanguage.RU -> "Найти"
            AppLanguage.EN -> "Search"
            AppLanguage.PT -> "Buscar"
        }

    val filterVideos: String
        get() = when (language) {
            AppLanguage.RU -> "Видео"
            AppLanguage.EN -> "Videos"
            AppLanguage.PT -> "Vídeos"
        }

    val filterShorts: String
        get() = when (language) {
            AppLanguage.RU -> "Шортс"
            AppLanguage.EN -> "Shorts"
            AppLanguage.PT -> "Shorts"
        }

    val filterAll: String
        get() = when (language) {
            AppLanguage.RU -> "Все"
            AppLanguage.EN -> "All"
            AppLanguage.PT -> "Todos"
        }

    val pasteLink: String
        get() = when (language) {
            AppLanguage.RU -> "Или вставить ссылку"
            AppLanguage.EN -> "Or paste a link"
            AppLanguage.PT -> "Ou colar um link"
        }

    val loadByLink: String
        get() = when (language) {
            AppLanguage.RU -> "Загрузить по ссылке"
            AppLanguage.EN -> "Load from link"
            AppLanguage.PT -> "Carregar pelo link"
        }

    val searchResults: String
        get() = when (language) {
            AppLanguage.RU -> "Результаты"
            AppLanguage.EN -> "Results"
            AppLanguage.PT -> "Resultados"
        }

    val watchHistory: String
        get() = when (language) {
            AppLanguage.RU -> "История"
            AppLanguage.EN -> "History"
            AppLanguage.PT -> "Histórico"
        }

    fun youtubeDate(millis: Long): String {
        val format = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        return format.format(java.util.Date(millis))
    }

    fun youtubeVideoAge(publishedAtMillis: Long, now: Long = System.currentTimeMillis()): String {
        val days = ((now - publishedAtMillis).coerceAtLeast(0L) / 86_400_000L).toInt()
        val hours = ((now - publishedAtMillis).coerceAtLeast(0L) / 3_600_000L).toInt()
        return when {
            days >= 365 -> {
                val n = days / 365
                when (language) {
                    AppLanguage.RU -> ruCount(n, "год", "года", "лет")
                    AppLanguage.EN -> if (n == 1) "1 year" else "$n years"
                    AppLanguage.PT -> if (n == 1) "1 ano" else "$n anos"
                }
            }
            days >= 30 -> {
                val n = days / 30
                when (language) {
                    AppLanguage.RU -> ruCount(n, "месяц", "месяца", "месяцев")
                    AppLanguage.EN -> if (n == 1) "1 month" else "$n months"
                    AppLanguage.PT -> if (n == 1) "1 mês" else "$n meses"
                }
            }
            days >= 7 -> {
                val n = days / 7
                when (language) {
                    AppLanguage.RU -> ruCount(n, "неделя", "недели", "недель")
                    AppLanguage.EN -> if (n == 1) "1 week" else "$n weeks"
                    AppLanguage.PT -> if (n == 1) "1 semana" else "$n semanas"
                }
            }
            days >= 1 -> when (language) {
                AppLanguage.RU -> ruCount(days, "день", "дня", "дней")
                AppLanguage.EN -> if (days == 1) "1 day" else "$days days"
                AppLanguage.PT -> if (days == 1) "1 dia" else "$days dias"
            }
            hours >= 1 -> when (language) {
                AppLanguage.RU -> ruCount(hours, "час", "часа", "часов")
                AppLanguage.EN -> if (hours == 1) "1 hour" else "$hours hours"
                AppLanguage.PT -> if (hours == 1) "1 hora" else "$hours horas"
            }
            else -> when (language) {
                AppLanguage.RU -> "только что"
                AppLanguage.EN -> "just now"
                AppLanguage.PT -> "agora"
            }
        }
    }

    fun youtubeReleasedLine(date: String, age: String): String = when (language) {
        AppLanguage.RU -> "Вышло $date · $age"
        AppLanguage.EN -> "Released $date · $age"
        AppLanguage.PT -> "Publicado $date · $age"
    }

    fun youtubeWatchedLine(date: String): String = when (language) {
        AppLanguage.RU -> "Смотрели $date"
        AppLanguage.EN -> "Watched $date"
        AppLanguage.PT -> "Visto $date"
    }

    private fun ruCount(n: Int, one: String, few: String, many: String): String {
        val mod10 = n % 10
        val mod100 = n % 100
        val word = when {
            mod10 == 1 && mod100 != 11 -> one
            mod10 in 2..4 && mod100 !in 12..14 -> few
            else -> many
        }
        return "$n $word"
    }

    val ytSearchAnotherVideo: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск другого видео"
            AppLanguage.EN -> "Search another video"
            AppLanguage.PT -> "Buscar outro vídeo"
        }

    val ytCollapseSearch: String
        get() = when (language) {
            AppLanguage.RU -> "Свернуть поиск"
            AppLanguage.EN -> "Collapse search"
            AppLanguage.PT -> "Recolher busca"
        }

    val commonCollapse: String
        get() = when (language) {
            AppLanguage.RU -> "Свернуть"
            AppLanguage.EN -> "Collapse"
            AppLanguage.PT -> "Recolher"
        }

    val commonExpand: String
        get() = when (language) {
            AppLanguage.RU -> "Развернуть"
            AppLanguage.EN -> "Expand"
            AppLanguage.PT -> "Expandir"
        }

    val clear: String
        get() = when (language) {
            AppLanguage.RU -> "Очистить"
            AppLanguage.EN -> "Clear"
            AppLanguage.PT -> "Limpar"
        }

    val clearHistoryTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Очистить историю?"
            AppLanguage.EN -> "Clear history?"
            AppLanguage.PT -> "Limpar histórico?"
        }

    val clearHistoryMessage: String
        get() = when (language) {
            AppLanguage.RU -> "Список просмотренных видео будет удалён. Сохранённые слова останутся."
            AppLanguage.EN -> "Watched videos will be removed. Saved words will stay."
            AppLanguage.PT -> "Os vídeos assistidos serão removidos. As palavras salvas permanecem."
        }

    val cancel: String
        get() = when (language) {
            AppLanguage.RU -> "Отмена"
            AppLanguage.EN -> "Cancel"
            AppLanguage.PT -> "Cancelar"
        }

    val subtitles: String
        get() = when (language) {
            AppLanguage.RU -> "Субтитры"
            AppLanguage.EN -> "Subtitles"
            AppLanguage.PT -> "Legendas"
        }

    val youtubeReloadSubtitles: String
        get() = when (language) {
            AppLanguage.RU -> "Перезагрузить субтитры"
            AppLanguage.EN -> "Reload subtitles"
            AppLanguage.PT -> "Recarregar legendas"
        }

    val subtitleFontSizeTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Размер субтитров"
            AppLanguage.EN -> "Subtitle size"
            AppLanguage.PT -> "Tamanho das legendas"
        }

    val subtitleFontSizeSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Текст субтитров в YouTube и читалке"
            AppLanguage.EN -> "Subtitle text in YouTube and reader"
            AppLanguage.PT -> "Texto das legendas no YouTube e leitura"
        }

    val homeSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Сегодняшний план"
            AppLanguage.EN -> "Today's plan"
            AppLanguage.PT -> "Plano de hoje"
        }

    val homeContinueReview: String
        get() = when (language) {
            AppLanguage.RU -> "Продолжить повтор"
            AppLanguage.EN -> "Continue review"
            AppLanguage.PT -> "Continuar revisão"
        }

    val homeMyCollections: String
        get() = when (language) {
            AppLanguage.RU -> "Мои наборы"
            AppLanguage.EN -> "My decks"
            AppLanguage.PT -> "Meus conjuntos"
        }

    val homeLibraryTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Статистика и прогресс"
            AppLanguage.EN -> "Stats and progress"
            AppLanguage.PT -> "Estatísticas e progresso"
        }

    val homeStatWords: String
        get() = when (language) {
            AppLanguage.RU -> "слова"
            AppLanguage.EN -> "words"
            AppLanguage.PT -> "palavras"
        }

    val homeStatCards: String
        get() = when (language) {
            AppLanguage.RU -> "карточки"
            AppLanguage.EN -> "cards"
            AppLanguage.PT -> "cartões"
        }

    val homeStatStudio: String
        get() = when (language) {
            AppLanguage.RU -> "коллекции"
            AppLanguage.EN -> "collections"
            AppLanguage.PT -> "coleções"
        }

    val homeStatTests: String
        get() = when (language) {
            AppLanguage.RU -> "тесты"
            AppLanguage.EN -> "tests"
            AppLanguage.PT -> "testes"
        }

    fun homeCardsCoverage(cards: Int, words: Int): String = when (language) {
        AppLanguage.RU -> "Карточки: $cards из $words слов словаря"
        AppLanguage.EN -> "Cards: $cards of $words dictionary words"
        AppLanguage.PT -> "Cartões: $cards de $words palavras do dicionário"
    }

    val statDueLabel: String
        get() = when (language) {
            AppLanguage.RU -> "на повтор"
            AppLanguage.EN -> "due"
            AppLanguage.PT -> "para revisar"
        }

    val statNewLabel: String
        get() = when (language) {
            AppLanguage.RU -> "новых"
            AppLanguage.EN -> "new"
            AppLanguage.PT -> "novas"
        }

    val statStreakLabel: String
        get() = when (language) {
            AppLanguage.RU -> "серия"
            AppLanguage.EN -> "streak"
            AppLanguage.PT -> "sequência"
        }

    val tabPractice: String
        get() = when (language) {
            AppLanguage.RU -> "Практика"
            AppLanguage.EN -> "Practice"
            AppLanguage.PT -> "Prática"
        }

    val studyScreenSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Колода → режим"
            AppLanguage.EN -> "Deck, then mode"
            AppLanguage.PT -> "Conjunto, depois modo"
        }

    val studyModeStudio: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val studyModeCards: String
        get() = when (language) {
            AppLanguage.RU -> "Карты"
            AppLanguage.EN -> "Cards"
            AppLanguage.PT -> "Cartões"
        }

    val studyModeTest: String
        get() = when (language) {
            AppLanguage.RU -> "Тест"
            AppLanguage.EN -> "Test"
            AppLanguage.PT -> "Teste"
        }

    val studyTestTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Тест"
            AppLanguage.EN -> "Test"
            AppLanguage.PT -> "Teste"
        }

    val studyTestChoiceTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Выбор варианта"
            AppLanguage.EN -> "Multiple choice"
            AppLanguage.PT -> "Múltipla escolha"
        }

    val studyTestChoiceHint: String
        get() = when (language) {
            AppLanguage.RU -> "Слово и пример. Четыре перевода, один верный из этой колоды."
            AppLanguage.EN -> "Word and example. Four translations, one correct from this deck."
            AppLanguage.PT -> "Palavra e exemplo. Quatro traduções, uma correta deste conjunto."
        }

    val studyTestMatchTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Пары"
            AppLanguage.EN -> "Matching"
            AppLanguage.PT -> "Correspondência"
        }

    val studyTestMatchHint: String
        get() = when (language) {
            AppLanguage.RU -> "Слева слова, справа переводы. Соедини пары."
            AppLanguage.EN -> "Words on the left, translations on the right. Match the pairs."
            AppLanguage.PT -> "Palavras à esquerda, traduções à direita. Ligue os pares."
        }

    val studyTestDone: String
        get() = when (language) {
            AppLanguage.RU -> "Готово"
            AppLanguage.EN -> "Done"
            AppLanguage.PT -> "Concluído"
        }

    val studyTestCorrect: String
        get() = when (language) {
            AppLanguage.RU -> "Правильно"
            AppLanguage.EN -> "Correct"
            AppLanguage.PT -> "Correto"
        }

    val studyTestIncorrect: String
        get() = when (language) {
            AppLanguage.RU -> "Неправильно"
            AppLanguage.EN -> "Incorrect"
            AppLanguage.PT -> "Incorreto"
        }

    val studyTestRuns: String
        get() = when (language) {
            AppLanguage.RU -> "Прогоны"
            AppLanguage.EN -> "Runs"
            AppLanguage.PT -> "Rodadas"
        }

    val studyTestTotal: String
        get() = when (language) {
            AppLanguage.RU -> "Всего"
            AppLanguage.EN -> "Total"
            AppLanguage.PT -> "Total"
        }

    fun studyTestAccuracy(percent: Int): String = when (language) {
        AppLanguage.RU -> "Точность $percent%"
        AppLanguage.EN -> "Accuracy $percent%"
        AppLanguage.PT -> "Precisão $percent%"
    }

    fun studyTestAnswersLine(correct: Int, incorrect: Int): String = when (language) {
        AppLanguage.RU -> "Правильно $correct · неправильно $incorrect"
        AppLanguage.EN -> "Correct $correct · incorrect $incorrect"
        AppLanguage.PT -> "Correto $correct · incorreto $incorrect"
    }

    val studyTestNeedWords: String
        get() = when (language) {
            AppLanguage.RU -> "В колоде мало слов для теста."
            AppLanguage.EN -> "This deck needs more words for a test."
            AppLanguage.PT -> "Este conjunto precisa de mais palavras."
        }

    val studyTestMatchHud: String
        get() = when (language) {
            AppLanguage.RU -> "Пары на время"
            AppLanguage.EN -> "Timed pairs"
            AppLanguage.PT -> "Pares no tempo"
        }

    val studyTestMatchTime: String
        get() = when (language) {
            AppLanguage.RU -> "Время"
            AppLanguage.EN -> "Time"
            AppLanguage.PT -> "Tempo"
        }

    val studyTestMatchScore: String
        get() = when (language) {
            AppLanguage.RU -> "Очки"
            AppLanguage.EN -> "Score"
            AppLanguage.PT -> "Pontos"
        }

    val studyTestMatchStreak: String
        get() = when (language) {
            AppLanguage.RU -> "Серия"
            AppLanguage.EN -> "Streak"
            AppLanguage.PT -> "Série"
        }

    val studyTestMatchPtCol: String
        get() = when (language) {
            AppLanguage.RU -> "Португальский"
            AppLanguage.EN -> "Portuguese"
            AppLanguage.PT -> "Português"
        }

    val studyTestMatchRuCol: String
        get() = when (language) {
            AppLanguage.RU -> "Русский"
            AppLanguage.EN -> "Russian"
            AppLanguage.PT -> "Russo"
        }

    val studyTestMatchLead: String
        get() = when (language) {
            AppLanguage.RU -> "Соедини слово с переводом. Серия даёт бонус, ошибка сбрасывает таймер-штраф."
            AppLanguage.EN -> "Match each word with its translation. Streaks score extra; a miss breaks the combo."
            AppLanguage.PT -> "Liga cada palavra à tradução. A série dá bónus; o erro quebra o combo."
        }

    val studyTestMatchStart: String
        get() = when (language) {
            AppLanguage.RU -> "Старт"
            AppLanguage.EN -> "Start"
            AppLanguage.PT -> "Começar"
        }

    val studyTestMatchNewGame: String
        get() = when (language) {
            AppLanguage.RU -> "Новая игра"
            AppLanguage.EN -> "New game"
            AppLanguage.PT -> "Novo jogo"
        }

    val studyTestMatchPause: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза"
            AppLanguage.EN -> "Pause"
            AppLanguage.PT -> "Pausa"
        }

    val studyTestMatchResume: String
        get() = when (language) {
            AppLanguage.RU -> "Продолжить"
            AppLanguage.EN -> "Resume"
            AppLanguage.PT -> "Continuar"
        }

    val studyTestMatchPauseLead: String
        get() = when (language) {
            AppLanguage.RU -> "Игра на паузе"
            AppLanguage.EN -> "Game paused"
            AppLanguage.PT -> "Jogo em pausa"
        }

    val studyTestMatchAgain: String
        get() = when (language) {
            AppLanguage.RU -> "Ещё раз"
            AppLanguage.EN -> "Play again"
            AppLanguage.PT -> "Jogar de novo"
        }

    val studyTestMatchEndClear: String
        get() = when (language) {
            AppLanguage.RU -> "Колода пройдена!"
            AppLanguage.EN -> "Deck cleared!"
            AppLanguage.PT -> "Conjunto limpo!"
        }

    val studyTestMatchEndTime: String
        get() = when (language) {
            AppLanguage.RU -> "Время вышло"
            AppLanguage.EN -> "Time's up"
            AppLanguage.PT -> "Tempo esgotado"
        }

    val studyTestMatchClearYell: String
        get() = when (language) {
            AppLanguage.RU -> "ЧИСТО!"
            AppLanguage.EN -> "CLEAR!"
            AppLanguage.PT -> "LIMPO!"
        }

    fun studyTestMatchComboYell(mult: Int): String = when (language) {
        AppLanguage.RU -> "КОМБО ×$mult"
        AppLanguage.EN -> "COMBO ×$mult"
        AppLanguage.PT -> "COMBO ×$mult"
    }

    fun studyTestMatchHint(n: Int): String = when (language) {
        AppLanguage.RU -> "Подсказка $n"
        AppLanguage.EN -> "Hint $n"
        AppLanguage.PT -> "Dica $n"
    }

    fun studyTestMatchBoard(board: Int, total: Int): String = when (language) {
        AppLanguage.RU -> "Доска $board/$total"
        AppLanguage.EN -> "Board $board/$total"
        AppLanguage.PT -> "Quadro $board/$total"
    }

    fun studyTestMatchCombo(n: Int): String = when (language) {
        AppLanguage.RU -> "Серия ×$n"
        AppLanguage.EN -> "Streak ×$n"
        AppLanguage.PT -> "Série ×$n"
    }

    fun studyTestMatchComboBonus(n: Int, extra: Int): String = when (language) {
        AppLanguage.RU -> "Серия ×$n  +$extra"
        AppLanguage.EN -> "Streak ×$n  +$extra"
        AppLanguage.PT -> "Série ×$n  +$extra"
    }

    fun studyTestMatchSec(value: String): String = when (language) {
        AppLanguage.RU -> "${value}с"
        AppLanguage.EN -> "${value}s"
        AppLanguage.PT -> "${value}s"
    }

    fun studyTestMatchEndLine(pairs: Int, score: Int, best: Int): String = when (language) {
        AppLanguage.RU -> "$pairs пар · $score очков · серия $best"
        AppLanguage.EN -> "$pairs pairs · $score pts · best $best"
        AppLanguage.PT -> "$pairs pares · $score pts · série $best"
    }

    fun studyTestMatchStreakYell(n: Int): String = when (language) {
        AppLanguage.RU -> "СЕРИЯ $n"
        AppLanguage.EN -> "STREAK $n"
        AppLanguage.PT -> "SÉRIE $n"
    }

    val studyCollectionsSection: String
        get() = when (language) {
            AppLanguage.RU -> "Наборы"
            AppLanguage.EN -> "Decks"
            AppLanguage.PT -> "Conjuntos"
        }

    val studyLastTestTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Последний тест"
            AppLanguage.EN -> "Last test"
            AppLanguage.PT -> "Último teste"
        }

    val studyLastTestHint: String
        get() = when (language) {
            AppLanguage.RU -> "Нажмите, чтобы открыть слова"
            AppLanguage.EN -> "Tap to see the words"
            AppLanguage.PT -> "Toque para ver as palavras"
        }

    val studyLastTestWordsTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Слова теста"
            AppLanguage.EN -> "Test words"
            AppLanguage.PT -> "Palavras do teste"
        }

    val studyLastTestWordsEmpty: String
        get() = when (language) {
            AppLanguage.RU -> "Для этого теста ещё нет разбивки по словам. Пройдите тест ещё раз."
            AppLanguage.EN -> "No per-word breakdown yet. Take the test again."
            AppLanguage.PT -> "Ainda não há detalhe por palavra. Faça o teste de novo."
        }

    val studyTestAccuracyLabel: String
        get() = when (language) {
            AppLanguage.RU -> "точность"
            AppLanguage.EN -> "accuracy"
            AppLanguage.PT -> "precisão"
        }

    val studyCalendarTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Календарь"
            AppLanguage.EN -> "Calendar"
            AppLanguage.PT -> "Calendário"
        }

    val studyCalendarLegend: String
        get() = when (language) {
            AppLanguage.RU -> "Квадрат — была активность"
            AppLanguage.EN -> "Square marks a day with activity"
            AppLanguage.PT -> "O quadrado marca um dia com atividade"
        }

    val studyDayStatsTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Статистика дня"
            AppLanguage.EN -> "Day stats"
            AppLanguage.PT -> "Estatísticas do dia"
        }

    val studyDayNoActivity: String
        get() = when (language) {
            AppLanguage.RU -> "В этот день ещё не было занятий."
            AppLanguage.EN -> "No study activity on this day yet."
            AppLanguage.PT -> "Ainda não houve estudo neste dia."
        }

    val studyDayCards: String
        get() = when (language) {
            AppLanguage.RU -> "Карточки"
            AppLanguage.EN -> "Cards"
            AppLanguage.PT -> "Cartões"
        }

    val studyDayGames: String
        get() = when (language) {
            AppLanguage.RU -> "Игры"
            AppLanguage.EN -> "Games"
            AppLanguage.PT -> "Jogos"
        }

    val studyDayTests: String
        get() = when (language) {
            AppLanguage.RU -> "Тесты"
            AppLanguage.EN -> "Tests"
            AppLanguage.PT -> "Testes"
        }

    val studyDaySessionsTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Все сессии"
            AppLanguage.EN -> "All sessions"
            AppLanguage.PT -> "Todas as sessões"
        }

    fun studyDaySessionLine(index: Int, kind: String, correct: Int, incorrect: Int): String = when (language) {
        AppLanguage.RU -> "$index. $kind · $correct верно · $incorrect ошиб."
        AppLanguage.EN -> "$index. $kind · $correct right · $incorrect wrong"
        AppLanguage.PT -> "$index. $kind · $correct certos · $incorrect errados"
    }

    val studyIntensityTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Интенсивность"
            AppLanguage.EN -> "Intensity"
            AppLanguage.PT -> "Intensidade"
        }

    val studyIntensityHint: String
        get() = when (language) {
            AppLanguage.RU -> "Нажмите день на графике, затем слово — откроется история прогонов."
            AppLanguage.EN -> "Tap a day on the chart, then a word to open its history."
            AppLanguage.PT -> "Toque num dia no gráfico e depois numa palavra para ver o histórico."
        }

    val studyIntensityTests: String
        get() = when (language) {
            AppLanguage.RU -> "Тесты"
            AppLanguage.EN -> "Tests"
            AppLanguage.PT -> "Testes"
        }

    val studyIntensityCards: String
        get() = when (language) {
            AppLanguage.RU -> "Карты"
            AppLanguage.EN -> "Cards"
            AppLanguage.PT -> "Cartões"
        }

    val studyIntensityStudio: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val studyIntensityError: String
        get() = when (language) {
            AppLanguage.RU -> "Ошибки"
            AppLanguage.EN -> "Errors"
            AppLanguage.PT -> "Erros"
        }

    val studyIntensityWordTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Слово"
            AppLanguage.EN -> "Word"
            AppLanguage.PT -> "Palavra"
        }

    val studyIntensityWordsEmpty: String
        get() = when (language) {
            AppLanguage.RU -> "В этот день слова ещё не прогоняли."
            AppLanguage.EN -> "No words were drilled on this day yet."
            AppLanguage.PT -> "Ainda não há palavras neste dia."
        }

    fun studyIntensityWordsTitle(dayLabel: String): String = when (language) {
        AppLanguage.RU -> "Слова · $dayLabel"
        AppLanguage.EN -> "Words · $dayLabel"
        AppLanguage.PT -> "Palavras · $dayLabel"
    }

    fun studyIntensityModules(tests: Int, cards: Int, studio: Int): String = when (language) {
        AppLanguage.RU -> "Тесты $tests · карты $cards · студия $studio"
        AppLanguage.EN -> "Tests $tests · cards $cards · studio $studio"
        AppLanguage.PT -> "Testes $tests · cartões $cards · estúdio $studio"
    }

    fun studyIntensityErrorPct(percent: Int): String = when (language) {
        AppLanguage.RU -> "Коэффициент ошибок $percent%"
        AppLanguage.EN -> "Error rate $percent%"
        AppLanguage.PT -> "Taxa de erro $percent%"
    }

    val javaLocale: java.util.Locale
        get() = when (language) {
            AppLanguage.RU -> java.util.Locale.forLanguageTag("ru")
            AppLanguage.EN -> java.util.Locale.ENGLISH
            AppLanguage.PT -> java.util.Locale.forLanguageTag("pt-BR")
        }

    val studioTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val studioSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Слово → перевод → пример с озвучкой"
            AppLanguage.EN -> "Word → translation → example with audio"
            AppLanguage.PT -> "Palavra → tradução → exemplo com áudio"
        }

    val studioSection: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val studioOpen: String
        get() = when (language) {
            AppLanguage.RU -> "Открыть Studio"
            AppLanguage.EN -> "Open Studio"
            AppLanguage.PT -> "Abrir Estúdio"
        }

    val studioPlay: String
        get() = when (language) {
            AppLanguage.RU -> "Слушать"
            AppLanguage.EN -> "Play"
            AppLanguage.PT -> "Ouvir"
        }

    val studioFullscreen: String
        get() = when (language) {
            AppLanguage.RU -> "Полный экран"
            AppLanguage.EN -> "Full screen"
            AppLanguage.PT -> "Ecrã inteiro"
        }

    val studioFullscreenExit: String
        get() = when (language) {
            AppLanguage.RU -> "Выйти из полного экрана"
            AppLanguage.EN -> "Exit full screen"
            AppLanguage.PT -> "Sair do ecrã inteiro"
        }

    val studioEmpty: String
        get() = when (language) {
            AppLanguage.RU -> "Нет коллекций"
            AppLanguage.EN -> "No collections"
            AppLanguage.PT -> "Sem coleções"
        }

    val studioSyncing: String
        get() = when (language) {
            AppLanguage.RU -> "Синхронизация Studio…"
            AppLanguage.EN -> "Syncing Studio…"
            AppLanguage.PT -> "A sincronizar Estúdio…"
        }

    val studioDone: String
        get() = when (language) {
            AppLanguage.RU -> "Готово"
            AppLanguage.EN -> "Done"
            AppLanguage.PT -> "Concluído"
        }

    fun studioCardProgress(current: Int, total: Int): String = when (language) {
        AppLanguage.RU -> "Карточка $current из $total"
        AppLanguage.EN -> "Card $current of $total"
        AppLanguage.PT -> "Cartão $current de $total"
    }

    fun studioNextIn(sec: String): String = when (language) {
        AppLanguage.RU -> "Следующая через ${sec}с"
        AppLanguage.EN -> "Next in ${sec}s"
        AppLanguage.PT -> "Próximo em ${sec}s"
    }

    val studioDirection: String
        get() = when (language) {
            AppLanguage.RU -> "Направление"
            AppLanguage.EN -> "Direction"
            AppLanguage.PT -> "Direção"
        }

    val studioDirPtToRu: String
        get() = when (language) {
            AppLanguage.RU -> "порт. → рус."
            AppLanguage.EN -> "PT → RU"
            AppLanguage.PT -> "port. → russo"
        }

    val studioDirRuToPt: String
        get() = when (language) {
            AppLanguage.RU -> "рус. → порт."
            AppLanguage.EN -> "RU → PT"
            AppLanguage.PT -> "russo → port."
        }

    fun studioDirLabel(fromCode: Int, toCode: Int): String =
        "${SubtitleLanguage.shortCode(fromCode)} → ${SubtitleLanguage.shortCode(toCode)}"

    fun studioSpeakWordLabel(sourceCode: Int): String = when (language) {
        AppLanguage.RU -> "Слово (${SubtitleLanguage.shortCode(sourceCode)})"
        AppLanguage.EN -> "Word (${SubtitleLanguage.shortCode(sourceCode)})"
        AppLanguage.PT -> "Palavra (${SubtitleLanguage.shortCode(sourceCode)})"
    }

    fun studioSpeakTrLabel(targetCode: Int): String = when (language) {
        AppLanguage.RU -> "Перевод (${SubtitleLanguage.shortCode(targetCode)})"
        AppLanguage.EN -> "Translation (${SubtitleLanguage.shortCode(targetCode)})"
        AppLanguage.PT -> "Tradução (${SubtitleLanguage.shortCode(targetCode)})"
    }

    fun studioSpeakExLabel(sourceCode: Int): String = when (language) {
        AppLanguage.RU -> "Пример (${SubtitleLanguage.shortCode(sourceCode)})"
        AppLanguage.EN -> "Example (${SubtitleLanguage.shortCode(sourceCode)})"
        AppLanguage.PT -> "Exemplo (${SubtitleLanguage.shortCode(sourceCode)})"
    }

    fun studioSpeakExTrLabel(targetCode: Int): String = when (language) {
        AppLanguage.RU -> "Пр. пер. (${SubtitleLanguage.shortCode(targetCode)})"
        AppLanguage.EN -> "Ex. tr. (${SubtitleLanguage.shortCode(targetCode)})"
        AppLanguage.PT -> "Ex. tr. (${SubtitleLanguage.shortCode(targetCode)})"
    }

    val studioVoice: String
        get() = when (language) {
            AppLanguage.RU -> "Голос"
            AppLanguage.EN -> "Voice"
            AppLanguage.PT -> "Voz"
        }

    val studioSpeed: String
        get() = when (language) {
            AppLanguage.RU -> "Скорость"
            AppLanguage.EN -> "Speed"
            AppLanguage.PT -> "Velocidade"
        }

    val studioPauseAfter: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза после слова"
            AppLanguage.EN -> "Pause after word"
            AppLanguage.PT -> "Pausa após a palavra"
        }

    val studioPauseAfterShort: String
        get() = when (language) {
            AppLanguage.RU -> "Слово"
            AppLanguage.EN -> "Word"
            AppLanguage.PT -> "Palavra"
        }

    val studioPauseBetween: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза между слов."
            AppLanguage.EN -> "Pause between lines"
            AppLanguage.PT -> "Pausa entre linhas"
        }

    val studioPauseBetweenShort: String
        get() = when (language) {
            AppLanguage.RU -> "Строки"
            AppLanguage.EN -> "Lines"
            AppLanguage.PT -> "Linhas"
        }

    val studioPauseCards: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза между карточками"
            AppLanguage.EN -> "Pause between cards"
            AppLanguage.PT -> "Pausa entre cartões"
        }

    val studioPauseCardsShort: String
        get() = when (language) {
            AppLanguage.RU -> "Карточки"
            AppLanguage.EN -> "Cards"
            AppLanguage.PT -> "Cartões"
        }

    val studioSpeakWhat: String
        get() = when (language) {
            AppLanguage.RU -> "Что озвучивать"
            AppLanguage.EN -> "What to speak"
            AppLanguage.PT -> "O que falar"
        }

    val studioSpeakNum: String
        get() = when (language) {
            AppLanguage.RU -> "Номер карточки"
            AppLanguage.EN -> "Card number"
            AppLanguage.PT -> "Número do cartão"
        }

    val studioSpeakNumShort: String
        get() = when (language) {
            AppLanguage.RU -> "№"
            AppLanguage.EN -> "#"
            AppLanguage.PT -> "Nº"
        }

    val studioSpeakWord: String
        get() = when (language) {
            AppLanguage.RU -> "Основное слово"
            AppLanguage.EN -> "Main word"
            AppLanguage.PT -> "Palavra principal"
        }

    val studioSpeakWordShort: String
        get() = when (language) {
            AppLanguage.RU -> "Слово"
            AppLanguage.EN -> "Word"
            AppLanguage.PT -> "Palavra"
        }

    val studioSpeakTr: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод"
            AppLanguage.EN -> "Translation"
            AppLanguage.PT -> "Tradução"
        }

    val studioSpeakTrShort: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод"
            AppLanguage.EN -> "Trans."
            AppLanguage.PT -> "Trad."
        }

    val studioSpeakEx: String
        get() = when (language) {
            AppLanguage.RU -> "Пример"
            AppLanguage.EN -> "Example"
            AppLanguage.PT -> "Exemplo"
        }

    val studioSpeakExShort: String
        get() = when (language) {
            AppLanguage.RU -> "Пример"
            AppLanguage.EN -> "Ex."
            AppLanguage.PT -> "Ex."
        }

    val studioSpeakExRu: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод примера"
            AppLanguage.EN -> "Example translation"
            AppLanguage.PT -> "Tradução do exemplo"
        }

    val studioSpeakExRuShort: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод"
            AppLanguage.EN -> "Trans."
            AppLanguage.PT -> "Trad."
        }

    val studioColSpeak: String
        get() = when (language) {
            AppLanguage.RU -> "Речь"
            AppLanguage.EN -> "Speak"
            AppLanguage.PT -> "Falar"
        }

    val studioColPause: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза, с"
            AppLanguage.EN -> "Pause, s"
            AppLanguage.PT -> "Pausa, s"
        }

    val studioPauses: String
        get() = when (language) {
            AppLanguage.RU -> "Паузы"
            AppLanguage.EN -> "Pauses"
            AppLanguage.PT -> "Pausas"
        }

    val studioCollections: String
        get() = when (language) {
            AppLanguage.RU -> "Коллекции"
            AppLanguage.EN -> "Collections"
            AppLanguage.PT -> "Coleções"
        }

    val studioRenders: String
        get() = when (language) {
            AppLanguage.RU -> "Рендеры"
            AppLanguage.EN -> "Renders"
            AppLanguage.PT -> "Renders"
        }

    val studioNoRenders: String
        get() = when (language) {
            AppLanguage.RU -> "Нет рендеров"
            AppLanguage.EN -> "No renders"
            AppLanguage.PT -> "Sem renders"
        }

    val studioNew: String
        get() = when (language) {
            AppLanguage.RU -> "Новая"
            AppLanguage.EN -> "New"
            AppLanguage.PT -> "Nova"
        }

    val studioNewCollection: String
        get() = when (language) {
            AppLanguage.RU -> "Новая коллекция"
            AppLanguage.EN -> "New collection"
            AppLanguage.PT -> "Nova coleção"
        }

    val studioRename: String
        get() = when (language) {
            AppLanguage.RU -> "Переименовать"
            AppLanguage.EN -> "Rename"
            AppLanguage.PT -> "Renomear"
        }

    val studioSave: String
        get() = when (language) {
            AppLanguage.RU -> "Сохранить"
            AppLanguage.EN -> "Save"
            AppLanguage.PT -> "Guardar"
        }

    val studioSelectWords: String
        get() = when (language) {
            AppLanguage.RU -> "Выбор слов для тренировки"
            AppLanguage.EN -> "Words for this session"
            AppLanguage.PT -> "Palavras para treinar"
        }

    val studioSetupTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Настройки"
            AppLanguage.EN -> "Settings"
            AppLanguage.PT -> "Definições"
        }

    val studioCollapsed: String
        get() = when (language) {
            AppLanguage.RU -> "свёрнуто"
            AppLanguage.EN -> "collapsed"
            AppLanguage.PT -> "recolhido"
        }

    fun studioSetupSummary(title: String, count: Int, collapsed: Boolean): String {
        val words = if (count <= 0) studioNoWords else studyWordsCount(count)
        val prefix = title.trim().takeIf { it.isNotEmpty() }?.let { "$it · " }.orEmpty()
        val collapsedBit = if (collapsed) "${studioCollapsed} · " else ""
        return prefix + collapsedBit + words
    }

    val studioNoWords: String
        get() = when (language) {
            AppLanguage.RU -> "Нет слов"
            AppLanguage.EN -> "No words"
            AppLanguage.PT -> "Sem palavras"
        }

    val studioAddMore: String
        get() = when (language) {
            AppLanguage.RU -> "добавить ещё"
            AppLanguage.EN -> "add more"
            AppLanguage.PT -> "adicionar mais"
        }

    val studioStart: String
        get() = when (language) {
            AppLanguage.RU -> "Начать"
            AppLanguage.EN -> "Start"
            AppLanguage.PT -> "Começar"
        }

    val studioRenderCollection: String
        get() = when (language) {
            AppLanguage.RU -> "Рендер коллекции"
            AppLanguage.EN -> "Render collection"
            AppLanguage.PT -> "Renderizar coleção"
        }

    val studioReRenderCollection: String
        get() = when (language) {
            AppLanguage.RU -> "Перерендерить"
            AppLanguage.EN -> "Re-render"
            AppLanguage.PT -> "Renderizar de novo"
        }

    fun studioRenderProgress(done: Int, total: Int): String = when (language) {
        AppLanguage.RU -> "Рендер… $done из $total"
        AppLanguage.EN -> "Rendering… $done of $total"
        AppLanguage.PT -> "A renderizar… $done de $total"
    }

    fun studioRenderCancelLabel(done: Int, total: Int): String = when (language) {
        AppLanguage.RU -> "Отменить · $done / $total"
        AppLanguage.EN -> "Cancel · $done / $total"
        AppLanguage.PT -> "Cancelar · $done / $total"
    }

    fun studioRenderReady(count: Int): String = when (language) {
        AppLanguage.RU -> "Готово · Play · $count карт."
        AppLanguage.EN -> "Ready · Play · $count cards"
        AppLanguage.PT -> "Pronto · Play · $count cartões"
    }

    val studioRenderNeeded: String
        get() = when (language) {
            AppLanguage.RU -> "Сначала рендер · потом Play"
            AppLanguage.EN -> "Render first, then Play"
            AppLanguage.PT -> "Primeiro renderizar, depois Play"
        }

    val studioRenderStale: String
        get() = when (language) {
            AppLanguage.RU -> "Текст или направление изменились — пересоберите, затем Play"
            AppLanguage.EN -> "Text or direction changed — render again, then Play"
            AppLanguage.PT -> "Texto ou direção mudou — renderize de novo e dê Play"
        }

    val studioRenderStaleVoice: String
        get() = when (language) {
            AppLanguage.RU -> "Голос сменился — пересоберите, затем Play"
            AppLanguage.EN -> "Voice changed — render again, then Play"
            AppLanguage.PT -> "A voz mudou — renderize de novo e dê Play"
        }

    val studioRenderFail: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось собрать озвучку"
            AppLanguage.EN -> "Could not render audio"
            AppLanguage.PT -> "Não foi possível renderizar o áudio"
        }

    val studioRenderCancelled: String
        get() = when (language) {
            AppLanguage.RU -> "Рендер отменён"
            AppLanguage.EN -> "Render cancelled"
            AppLanguage.PT -> "Renderização cancelada"
        }

    val studioPlayNeedsRender: String
        get() = when (language) {
            AppLanguage.RU -> "Сначала сделайте рендер коллекции"
            AppLanguage.EN -> "Render the collection first"
            AppLanguage.PT -> "Renderize a coleção primeiro"
        }

    val studioShuffle: String
        get() = when (language) {
            AppLanguage.RU -> "Перемешать"
            AppLanguage.EN -> "Shuffle"
            AppLanguage.PT -> "Baralhar"
        }

    val studioShuffleNow: String
        get() = when (language) {
            AppLanguage.RU -> "Перемешать сейчас"
            AppLanguage.EN -> "Shuffle now"
            AppLanguage.PT -> "Baralhar agora"
        }

    fun studioPauseValue(value: Float): String {
        val n = "%.1f".format(value)
        return when (language) {
            AppLanguage.RU -> "${n}с"
            AppLanguage.EN -> "${n}s"
            AppLanguage.PT -> "${n}s"
        }
    }

    fun studioSyncPulled(count: Int): String = when (language) {
        AppLanguage.RU -> "Studio: $count"
        AppLanguage.EN -> "Studio: $count"
        AppLanguage.PT -> "Estúdio: $count"
    }

    val studioFinish: String
        get() = when (language) {
            AppLanguage.RU -> "Закончить"
            AppLanguage.EN -> "Finish"
            AppLanguage.PT -> "Terminar"
        }

    val studioAddFromDict: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить из словаря"
            AppLanguage.EN -> "Add from dictionary"
            AppLanguage.PT -> "Adicionar do dicionário"
        }

    val studioEmptyDict: String
        get() = when (language) {
            AppLanguage.RU -> "В словаре пока нет слов"
            AppLanguage.EN -> "No words in the dictionary yet"
            AppLanguage.PT -> "Ainda não há palavras no dicionário"
        }

    val studioPickerSub: String
        get() = when (language) {
            AppLanguage.RU -> "Откройте папку или добавьте её целиком"
            AppLanguage.EN -> "Open a folder or add it all at once"
            AppLanguage.PT -> "Abra uma pasta ou adicione-a inteira"
        }

    val studioPickerMarkWords: String
        get() = when (language) {
            AppLanguage.RU -> "Отметьте слова галочками"
            AppLanguage.EN -> "Tick the words to add"
            AppLanguage.PT -> "Marque as palavras"
        }

    val studioPickerSearch: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск"
            AppLanguage.EN -> "Search"
            AppLanguage.PT -> "Pesquisar"
        }

    val studioPickerBack: String
        get() = when (language) {
            AppLanguage.RU -> "К папкам"
            AppLanguage.EN -> "Folders"
            AppLanguage.PT -> "Pastas"
        }

    val studioPickerSelectAll: String
        get() = when (language) {
            AppLanguage.RU -> "Выбрать все"
            AppLanguage.EN -> "Select all"
            AppLanguage.PT -> "Selecionar tudo"
        }

    val studioPickerDeselectAll: String
        get() = when (language) {
            AppLanguage.RU -> "Снять все"
            AppLanguage.EN -> "Clear all"
            AppLanguage.PT -> "Limpar"
        }

    val studioPickerAddFolder: String
        get() = when (language) {
            AppLanguage.RU -> "+ папка"
            AppLanguage.EN -> "+ folder"
            AppLanguage.PT -> "+ pasta"
        }

    val studioPickerFolderPicked: String
        get() = when (language) {
            AppLanguage.RU -> "✓ в выборе"
            AppLanguage.EN -> "✓ selected"
            AppLanguage.PT -> "✓ escolhida"
        }

    val studioPickerFolderDone: String
        get() = when (language) {
            AppLanguage.RU -> "уже в наборе"
            AppLanguage.EN -> "already added"
            AppLanguage.PT -> "já no conjunto"
        }

    fun studioAddCount(count: Int): String = when (language) {
        AppLanguage.RU -> "Добавить ($count)"
        AppLanguage.EN -> "Add ($count)"
        AppLanguage.PT -> "Adicionar ($count)"
    }

    val studioAdd: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить"
            AppLanguage.EN -> "Add"
            AppLanguage.PT -> "Adicionar"
        }

    val studioCancel: String
        get() = when (language) {
            AppLanguage.RU -> "Отмена"
            AppLanguage.EN -> "Cancel"
            AppLanguage.PT -> "Cancelar"
        }

    val studioEditCard: String
        get() = when (language) {
            AppLanguage.RU -> "Изменить карточку"
            AppLanguage.EN -> "Edit card"
            AppLanguage.PT -> "Editar cartão"
        }

    val studioWordType: String
        get() = when (language) {
            AppLanguage.RU -> "Тип слова"
            AppLanguage.EN -> "Word type"
            AppLanguage.PT -> "Tipo de palavra"
        }

    fun studioTagLabel(tag: String): String {
        val key = tag.trim().lowercase()
        return when (language) {
            AppLanguage.RU -> when (key) {
                "substantivo" -> "сущ."
                "verbo" -> "гл."
                "adjetivo" -> "прил."
                "adverbio" -> "нар."
                "pronome" -> "мест."
                "preposição", "preposicao" -> "предл."
                "conjunção", "conjuncao" -> "союз"
                "expressão", "expressao" -> "выр."
                "frase" -> "фр."
                else -> "общ."
            }
            AppLanguage.EN -> when (key) {
                "substantivo" -> "noun"
                "verbo" -> "verb"
                "adjetivo" -> "adj."
                "adverbio" -> "adv."
                "pronome" -> "pron."
                "preposição", "preposicao" -> "prep."
                "conjunção", "conjuncao" -> "conj."
                "expressão", "expressao" -> "expr."
                "frase" -> "phr."
                else -> "gen."
            }
            AppLanguage.PT -> when (key) {
                "substantivo" -> "subst."
                "verbo" -> "verbo"
                "adjetivo" -> "adj."
                "adverbio" -> "adv."
                "pronome" -> "pron."
                "preposição", "preposicao" -> "prep."
                "conjunção", "conjuncao" -> "conj."
                "expressão", "expressao" -> "expr."
                "frase" -> "frase"
                else -> "geral"
            }
        }
    }

    val studyCreateSet: String
        get() = when (language) {
            AppLanguage.RU -> "Создать набор"
            AppLanguage.EN -> "Create deck"
            AppLanguage.PT -> "Criar conjunto"
        }

    val studyConfirmCreate: String
        get() = when (language) {
            AppLanguage.RU -> "Создать"
            AppLanguage.EN -> "Create"
            AppLanguage.PT -> "Criar"
        }

    fun studyWordsCount(count: Int): String = when (language) {
        AppLanguage.RU -> "$count слов"
        AppLanguage.EN -> "$count words"
        AppLanguage.PT -> "$count palavras"
    }

    fun studyDueCount(count: Int): String = when (language) {
        AppLanguage.RU -> "$count на повтор"
        AppLanguage.EN -> "$count due"
        AppLanguage.PT -> "$count para revisar"
    }

    val studyEmptyNoCollection: String
        get() = when (language) {
            AppLanguage.RU -> "Создайте набор на главной."
            AppLanguage.EN -> "Create a deck on the home screen."
            AppLanguage.PT -> "Crie um conjunto na tela inicial."
        }

    val studyEmptyNoDue: String
        get() = when (language) {
            AppLanguage.RU -> "В этом наборе нет карточек на повтор."
            AppLanguage.EN -> "No cards due in this deck."
            AppLanguage.PT -> "Não há cartões para revisar neste conjunto."
        }

    val studyAllDone: String
        get() = when (language) {
            AppLanguage.RU -> "На сегодня всё ✓"
            AppLanguage.EN -> "All done for today ✓"
            AppLanguage.PT -> "Tudo feito por hoje ✓"
        }

    val studyAgain: String
        get() = when (language) {
            AppLanguage.RU -> "Ещё раз"
            AppLanguage.EN -> "Again"
            AppLanguage.PT -> "De novo"
        }

    val studyKnown: String
        get() = when (language) {
            AppLanguage.RU -> "Знаю"
            AppLanguage.EN -> "Know it"
            AppLanguage.PT -> "Sei"
        }

    val practiceScreenSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Дополнительные тренировки"
            AppLanguage.EN -> "Extra practice modules"
            AppLanguage.PT -> "Treinos extras"
        }

    val practiceModuleCelpeTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Времена CELPE"
            AppLanguage.EN -> "CELPE tenses"
            AppLanguage.PT -> "Tempos CELPE"
        }

    val practiceModuleCelpeDesc: String
        get() = when (language) {
            AppLanguage.RU -> "Времена для CELPE-Bras"
            AppLanguage.EN -> "Tenses for CELPE-Bras"
            AppLanguage.PT -> "Tempos para o CELPE-Bras"
        }

    val practiceModuleVerbsTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Тест на глаголы"
            AppLanguage.EN -> "Verb test"
            AppLanguage.PT -> "Teste de verbos"
        }

    val practiceModuleVerbsDesc: String
        get() = when (language) {
            AppLanguage.RU -> "Тест на спряжения"
            AppLanguage.EN -> "Conjugation quiz"
            AppLanguage.PT -> "Teste de conjugação"
        }

    val practiceModuleYoutubeTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Практика с видео"
            AppLanguage.EN -> "YouTube practice"
            AppLanguage.PT -> "Prática no YouTube"
        }

    val practiceModuleYoutubeDesc: String
        get() = when (language) {
            AppLanguage.RU -> "Субтитры и слова"
            AppLanguage.EN -> "Subtitles and vocabulary"
            AppLanguage.PT -> "Legendas e vocabulário"
        }

    val practiceModulePronunciationTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Произношение"
            AppLanguage.EN -> "Pronunciation"
            AppLanguage.PT -> "Pronúncia"
        }

    val practiceModulePronunciationDesc: String
        get() = when (language) {
            AppLanguage.RU -> "Произношение и аудио"
            AppLanguage.EN -> "Pronunciation and audio"
            AppLanguage.PT -> "Pronúncia e áudio"
        }

    val readerScreenSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Книги с переводом слов и фраз по тапу"
            AppLanguage.EN -> "Books with tap-to-translate words and phrases"
            AppLanguage.PT -> "Livros com tradução de palavras e frases ao toque"
        }

    val readerLibrarySection: String
        get() = when (language) {
            AppLanguage.RU -> "Библиотека"
            AppLanguage.EN -> "Library"
            AppLanguage.PT -> "Biblioteca"
        }

    val readerImportButton: String
        get() = when (language) {
            AppLanguage.RU -> "Импорт .txt / .docx"
            AppLanguage.EN -> "Import .txt / .docx"
            AppLanguage.PT -> "Importar .txt / .docx"
        }

    val readerNoBooks: String
        get() = when (language) {
            AppLanguage.RU -> "Нет книг. Импортируйте .txt или .docx."
            AppLanguage.EN -> "No books yet. Import a .txt or .docx file."
            AppLanguage.PT -> "Sem livros. Importe um arquivo .txt ou .docx."
        }

    fun readerBookProgressLabel(percent: Int): String = when (language) {
        AppLanguage.RU -> "Прочитано $percent%"
        AppLanguage.EN -> "$percent% read"
        AppLanguage.PT -> "$percent% lido"
    }

    val readerOpeningBook: String
        get() = when (language) {
            AppLanguage.RU -> "Открываем книгу…"
            AppLanguage.EN -> "Opening book…"
            AppLanguage.PT -> "Abrindo livro…"
        }

    val readerImportingBook: String
        get() = when (language) {
            AppLanguage.RU -> "Импорт файла…"
            AppLanguage.EN -> "Importing file…"
            AppLanguage.PT -> "Importando arquivo…"
        }

    val loadingSubtitles: String
        get() = when (language) {
            AppLanguage.RU -> "Загрузка субтитров…"
            AppLanguage.EN -> "Loading subtitles…"
            AppLanguage.PT -> "Carregando legendas…"
        }

    val loadingSearch: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск видео…"
            AppLanguage.EN -> "Searching videos…"
            AppLanguage.PT -> "Buscando vídeos…"
        }

    val loadingSession: String
        get() = when (language) {
            AppLanguage.RU -> "Загрузка карточек…"
            AppLanguage.EN -> "Loading cards…"
            AppLanguage.PT -> "Carregando cartões…"
        }

    val dictionaryNoSearchResults: String
        get() = when (language) {
            AppLanguage.RU -> "Слов не найдено. Измените поиск или фильтр папки."
            AppLanguage.EN -> "No words found. Change search or folder filter."
            AppLanguage.PT -> "Nenhuma palavra. Altere a busca ou o filtro da pasta."
        }

    val dictionaryNewFolder: String
        get() = when (language) {
            AppLanguage.RU -> "Новая папка"
            AppLanguage.EN -> "New folder"
            AppLanguage.PT -> "Nova pasta"
        }

    val dictionaryNewFolderTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Создать папку"
            AppLanguage.EN -> "Create folder"
            AppLanguage.PT -> "Criar pasta"
        }

    val dictionaryEditFolderTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Редактировать папку"
            AppLanguage.EN -> "Edit folder"
            AppLanguage.PT -> "Editar pasta"
        }

    val dictionaryFolderNameLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Название"
            AppLanguage.EN -> "Name"
            AppLanguage.PT -> "Nome"
        }

    val dictionaryFolderDescLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Описание (необязательно)"
            AppLanguage.EN -> "Description (optional)"
            AppLanguage.PT -> "Descrição (opcional)"
        }

    val dictionaryCreate: String
        get() = when (language) {
            AppLanguage.RU -> "Создать"
            AppLanguage.EN -> "Create"
            AppLanguage.PT -> "Criar"
        }

    val save: String
        get() = when (language) {
            AppLanguage.RU -> "Сохранить"
            AppLanguage.EN -> "Save"
            AppLanguage.PT -> "Salvar"
        }

    val delete: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить"
            AppLanguage.EN -> "Delete"
            AppLanguage.PT -> "Excluir"
        }

    val dictionaryAddWord: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить слово"
            AppLanguage.EN -> "Add word"
            AppLanguage.PT -> "Adicionar palavra"
        }

    val dictionaryAddWordTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Новое слово"
            AppLanguage.EN -> "New word"
            AppLanguage.PT -> "Nova palavra"
        }

    val dictionaryWordPtLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Португальский"
            AppLanguage.EN -> "Portuguese"
            AppLanguage.PT -> "Português"
        }

    val dictionaryWordRuLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод"
            AppLanguage.EN -> "Translation"
            AppLanguage.PT -> "Tradução"
        }

    val dictionaryWordExampleLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Пример"
            AppLanguage.EN -> "Example"
            AppLanguage.PT -> "Exemplo"
        }

    val dictionaryWordExampleRuLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод примера"
            AppLanguage.EN -> "Example translation"
            AppLanguage.PT -> "Tradução do exemplo"
        }

    val dictionaryDeleteFolderTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить папку?"
            AppLanguage.EN -> "Delete folder?"
            AppLanguage.PT -> "Excluir pasta?"
        }

    fun dictionaryDeleteFolderMessage(title: String, wordCount: Int): String = when (language) {
        AppLanguage.RU -> "Папка «$title» и $wordCount слов будут удалены без восстановления."
        AppLanguage.EN -> "Folder \"$title\" and $wordCount words will be deleted permanently."
        AppLanguage.PT -> "A pasta \"$title\" e $wordCount palavras serão excluídas permanentemente."
    }

    val dictionaryDeleteWordTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить слово?"
            AppLanguage.EN -> "Delete word?"
            AppLanguage.PT -> "Excluir palavra?"
        }

    val dictionaryAllFolders: String
        get() = when (language) {
            AppLanguage.RU -> "Все папки"
            AppLanguage.EN -> "All folders"
            AppLanguage.PT -> "Todas as pastas"
        }

    val dictionarySelectFolderToRecord: String
        get() = when (language) {
            AppLanguage.RU -> "Выберите папку в списке выше"
            AppLanguage.EN -> "Select a folder above"
            AppLanguage.PT -> "Selecione uma pasta acima"
        }

    val dictionarySearchAndFolders: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск и папка"
            AppLanguage.EN -> "Search & folder"
            AppLanguage.PT -> "Busca e pasta"
        }

    val dictionaryAddRowHint: String
        get() = when (language) {
            AppLanguage.RU -> "Новая строка — введите слово и перевод"
            AppLanguage.EN -> "New row — enter word and translation"
            AppLanguage.PT -> "Nova linha — digite palavra e tradução"
        }

    fun dictionarySubtitle(wordCount: Int): String = when (language) {
        AppLanguage.RU -> if (wordCount > 0) "$wordCount слов · таблица" else "Таблица слов из видео и читалки"
        AppLanguage.EN -> if (wordCount > 0) "$wordCount words · table" else "Words from video and reader"
        AppLanguage.PT -> if (wordCount > 0) "$wordCount palavras · tabela" else "Palavras de vídeo e leitura"
    }

    val dictionaryEmptyHint: String
        get() = when (language) {
            AppLanguage.RU -> "Создайте папку и добавляйте слова вручную, или импортируйте из YouTube и читалки."
            AppLanguage.EN -> "Create a folder and add words manually, or import from YouTube and the reader."
            AppLanguage.PT -> "Crie uma pasta e adicione palavras manualmente, ou importe do YouTube e da leitura."
        }

    val studyStartSession: String
        get() = when (language) {
            AppLanguage.RU -> "Начать"
            AppLanguage.EN -> "Start"
            AppLanguage.PT -> "Começar"
        }

    val studyRepeatSet: String
        get() = when (language) {
            AppLanguage.RU -> "Повторить набор"
            AppLanguage.EN -> "Review set"
            AppLanguage.PT -> "Repetir conjunto"
        }

    val studyDoneTodayHint: String
        get() = when (language) {
            AppLanguage.RU -> "По расписанию на сегодня всё · можно повторить всё"
            AppLanguage.EN -> "Today's schedule is done · you can still review all"
            AppLanguage.PT -> "Agenda de hoje concluída · ainda pode repetir tudo"
        }

    val studySetSettingsTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Настройки набора"
            AppLanguage.EN -> "Deck settings"
            AppLanguage.PT -> "Configurações do conjunto"
        }

    val studySetSettingsHint: String
        get() = when (language) {
            AppLanguage.RU -> "Карточки этой колоды — те же слова из словаря. Снимите галочку, чтобы убрать слово из колоды."
            AppLanguage.EN -> "These are the same dictionary cards in this deck. Uncheck to remove a word from the deck."
            AppLanguage.PT -> "São as mesmas cartas do dicionário neste conjunto. Desmarque para remover a palavra."
        }

    val studySetSettingsAddFromDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить из словаря"
            AppLanguage.EN -> "Add from dictionary"
            AppLanguage.PT -> "Adicionar do dicionário"
        }

    val studySetSettingsEmpty: String
        get() = when (language) {
            AppLanguage.RU -> "В наборе пока нет слов. Добавьте из словаря."
            AppLanguage.EN -> "No words in this deck yet. Add from the dictionary."
            AppLanguage.PT -> "Ainda não há palavras neste conjunto. Adicione do dicionário."
        }

    val studySetSettingsGear: String
        get() = when (language) {
            AppLanguage.RU -> "Настройки набора"
            AppLanguage.EN -> "Deck settings"
            AppLanguage.PT -> "Configurações"
        }

    val studyDeleteSetTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить колоду?"
            AppLanguage.EN -> "Delete deck?"
            AppLanguage.PT -> "Eliminar o baralho?"
        }

    fun studyDeleteSetMessage(name: String): String = when (language) {
        AppLanguage.RU -> "«$name» будет удалена. Слова в словаре останутся."
        AppLanguage.EN -> "“$name” will be deleted. Dictionary words stay."
        AppLanguage.PT -> "“$name” será eliminado. As palavras do dicionário ficam."
    }

    val studyAllReviewedTitle: String
        get() = when (language) {
            AppLanguage.RU -> "На сегодня по расписанию всё"
            AppLanguage.EN -> "Nothing due for today"
            AppLanguage.PT -> "Nada pendente para hoje"
        }

    val studyAllReviewedHint: String
        get() = when (language) {
            AppLanguage.RU -> "Можно повторить весь набор в любой момент — интервалы обновятся как обычно."
            AppLanguage.EN -> "You can review the full set anytime — intervals still update as usual."
            AppLanguage.PT -> "Pode repetir o conjunto inteiro a qualquer momento — os intervalos atualizam normalmente."
        }

    fun studyPracticeFullSet(wordCount: Int): String = when (language) {
        AppLanguage.RU -> "Повторить весь набор ($wordCount сл.)"
        AppLanguage.EN -> "Review full set ($wordCount words)"
        AppLanguage.PT -> "Repetir conjunto ($wordCount pal.)"
    }

    fun studyMasteredInSet(count: Int): String = when (language) {
        AppLanguage.RU -> "Освоено в наборе: $count"
        AppLanguage.EN -> "Mastered in set: $count"
        AppLanguage.PT -> "Dominadas no conjunto: $count"
    }

    val studyBackToMenu: String
        get() = when (language) {
            AppLanguage.RU -> "Вернуться в меню"
            AppLanguage.EN -> "Back to menu"
            AppLanguage.PT -> "Voltar ao menu"
        }

    val practiceDontKnow: String
        get() = when (language) {
            AppLanguage.RU -> "Не знаю"
            AppLanguage.EN -> "Don't know"
            AppLanguage.PT -> "Não sei"
        }

    val practiceKnow: String
        get() = when (language) {
            AppLanguage.RU -> "✓ Знаю"
            AppLanguage.EN -> "✓ Know"
            AppLanguage.PT -> "✓ Sei"
        }

    val practiceStatDontKnow: String
        get() = when (language) {
            AppLanguage.RU -> "НЕ ЗНАЮ"
            AppLanguage.EN -> "DON'T KNOW"
            AppLanguage.PT -> "NÃO SEI"
        }

    val practiceStatKnow: String
        get() = when (language) {
            AppLanguage.RU -> "ЗНАЮ"
            AppLanguage.EN -> "KNOW"
            AppLanguage.PT -> "SEI"
        }

    val practiceStatRemaining: String
        get() = when (language) {
            AppLanguage.RU -> "ОСТАЛОСЬ"
            AppLanguage.EN -> "LEFT"
            AppLanguage.PT -> "RESTAM"
        }

    val practiceSummaryKnow: String
        get() = when (language) {
            AppLanguage.RU -> "Знаю:"
            AppLanguage.EN -> "Know:"
            AppLanguage.PT -> "Sei:"
        }

    val practiceSummaryDontKnow: String
        get() = when (language) {
            AppLanguage.RU -> "Не знаю:"
            AppLanguage.EN -> "Don't know:"
            AppLanguage.PT -> "Não sei:"
        }

    val practiceSummaryTotal: String
        get() = when (language) {
            AppLanguage.RU -> "Всего в сессии:"
            AppLanguage.EN -> "In session:"
            AppLanguage.PT -> "Na sessão:"
        }

    val practiceSummaryDone: String
        get() = when (language) {
            AppLanguage.RU -> "Готово!"
            AppLanguage.EN -> "Done!"
            AppLanguage.PT -> "Pronto!"
        }

    fun practiceSummarySessionProgress(known: Int, total: Int, percent: Int): String = when (language) {
        AppLanguage.RU -> "В этой сессии: $known из $total слов · $percent%"
        AppLanguage.EN -> "This session: $known of $total words · $percent%"
        AppLanguage.PT -> "Nesta sessão: $known de $total palavras · $percent%"
    }

    fun practiceSummaryDeckMastery(mastered: Int, total: Int, percent: Int): String = when (language) {
        AppLanguage.RU -> "Полностью освоено в наборе (SRS): $mastered из $total · $percent%"
        AppLanguage.EN -> "Fully mastered in set (SRS): $mastered of $total · $percent%"
        AppLanguage.PT -> "Dominadas no conjunto (SRS): $mastered de $total · $percent%"
    }

    val audioTrimTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Обрезка записи"
            AppLanguage.EN -> "Trim recording"
            AppLanguage.PT -> "Cortar gravação"
        }

    val audioTrimSave: String
        get() = when (language) {
            AppLanguage.RU -> "Сохранить"
            AppLanguage.EN -> "Save"
            AppLanguage.PT -> "Salvar"
        }

    val audioTrimDelete: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить"
            AppLanguage.EN -> "Delete"
            AppLanguage.PT -> "Excluir"
        }

    val audioTrimCancel: String
        get() = when (language) {
            AppLanguage.RU -> "Отмена"
            AppLanguage.EN -> "Cancel"
            AppLanguage.PT -> "Cancelar"
        }

    val audioTrimPlay: String
        get() = when (language) {
            AppLanguage.RU -> "Прослушать фрагмент"
            AppLanguage.EN -> "Play selection"
            AppLanguage.PT -> "Ouvir trecho"
        }

    val audioTrimEdit: String
        get() = when (language) {
            AppLanguage.RU -> "Обрезать"
            AppLanguage.EN -> "Trim"
            AppLanguage.PT -> "Cortar"
        }

    val audioTrimLoadError: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось загрузить аудио"
            AppLanguage.EN -> "Could not load audio"
            AppLanguage.PT -> "Não foi possível carregar o áudio"
        }

    val audioTrimSaveError: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось сохранить обрезку"
            AppLanguage.EN -> "Could not save trim"
            AppLanguage.PT -> "Não foi possível salvar o corte"
        }

    fun dictionaryFolderRecordingDefault(index: Int): String =
        when (language) {
            AppLanguage.RU -> "Запись папки ${index + 1}"
            AppLanguage.EN -> "Folder recording ${index + 1}"
            AppLanguage.PT -> "Gravação da pasta ${index + 1}"
        }

    fun dictionaryCardRecordingDefault(index: Int): String =
        when (language) {
            AppLanguage.RU -> "Запись ${index + 1}"
            AppLanguage.EN -> "Recording ${index + 1}"
            AppLanguage.PT -> "Gravação ${index + 1}"
        }

    val profileTagline: String
        get() = when (language) {
            AppLanguage.RU -> "Proficiência Conquistada"
            AppLanguage.EN -> "Proficiency Achieved"
            AppLanguage.PT -> "Proficiência Conquistada"
        }

    val profileAccountSection: String
        get() = when (language) {
            AppLanguage.RU -> "Аккаунт"
            AppLanguage.EN -> "Account"
            AppLanguage.PT -> "Conta"
        }

    val profileSignedOut: String
        get() = when (language) {
            AppLanguage.RU -> "Вы не вошли в аккаунт"
            AppLanguage.EN -> "You are not signed in"
            AppLanguage.PT -> "Você não está conectado"
        }

    val profileSignInGoogle: String
        get() = when (language) {
            AppLanguage.RU -> "Войти через Google"
            AppLanguage.EN -> "Sign in with Google"
            AppLanguage.PT -> "Entrar com Google"
        }

    val profileSignInEmail: String
        get() = when (language) {
            AppLanguage.RU -> "Войти"
            AppLanguage.EN -> "Sign in"
            AppLanguage.PT -> "Entrar"
        }

    val profileEmailPlaceholder: String
        get() = when (language) {
            AppLanguage.RU -> "Email с сайта"
            AppLanguage.EN -> "Email from the site"
            AppLanguage.PT -> "E-mail do site"
        }

    val profilePasswordPlaceholder: String
        get() = when (language) {
            AppLanguage.RU -> "Пароль"
            AppLanguage.EN -> "Password"
            AppLanguage.PT -> "Senha"
        }

    val profileSiteLoginHint: String
        get() = when (language) {
            AppLanguage.RU -> "Тот же email и пароль, что на profconq.com. Google сейчас может не работать — это не ломает вход с сайта."
            AppLanguage.EN -> "Same email and password as on profconq.com. Google may fail until SHA-1 is added in Firebase."
            AppLanguage.PT -> "O mesmo e-mail e senha do profconq.com. O Google pode falhar até o SHA-1 no Firebase."
        }

    val profileSignOut: String
        get() = when (language) {
            AppLanguage.RU -> "Выйти"
            AppLanguage.EN -> "Sign out"
            AppLanguage.PT -> "Sair"
        }

    val profileAuthLoading: String
        get() = when (language) {
            AppLanguage.RU -> "Подождите..."
            AppLanguage.EN -> "Please wait..."
            AppLanguage.PT -> "Aguarde..."
        }

    val profileCloudWords: String
        get() = when (language) {
            AppLanguage.RU -> "Слова в облаке"
            AppLanguage.EN -> "Cloud words"
            AppLanguage.PT -> "Palavras na nuvem"
        }

    val profileLocalWords: String
        get() = when (language) {
            AppLanguage.RU -> "Слов в словаре (на устройстве)"
            AppLanguage.EN -> "Words on device"
            AppLanguage.PT -> "Palavras no dispositivo"
        }

    val profileSyncPrimaryApp: String
        get() = when (language) {
            AppLanguage.RU -> "Источник: приложение"
            AppLanguage.EN -> "Source: app"
            AppLanguage.PT -> "Fonte: app"
        }

    val profileSyncPrimarySite: String
        get() = when (language) {
            AppLanguage.RU -> "Источник: сайт"
            AppLanguage.EN -> "Source: website"
            AppLanguage.PT -> "Fonte: site"
        }

    val profileSyncMirror: String
        get() = when (language) {
            AppLanguage.RU -> "Синхронизировать"
            AppLanguage.EN -> "Sync"
            AppLanguage.PT -> "Sincronizar"
        }

    val profileSyncMirrorHint: String
        get() = when (language) {
            AppLanguage.RU -> "Зеркало: словарь и программы копируются с выбранного источника."
            AppLanguage.EN -> "Mirror: vocabulary and programs copy from the selected source."
            AppLanguage.PT -> "Espelho: vocabulário e programas copiam da fonte escolhida."
        }

    val profileSyncPrimaryAppHint: String
        get() = when (language) {
            AppLanguage.RU -> "Телефон → облако → сайт подтянет при синхронизации на profconq.com"
            AppLanguage.EN -> "Phone → cloud → site pulls when you sync on profconq.com"
            AppLanguage.PT -> "Telefone → nuvem → o site puxa ao sincronizar no profconq.com"
        }

    val profileSyncPrimarySiteHint: String
        get() = when (language) {
            AppLanguage.RU -> "Сайт → облако → приложение подтянет по кнопке «Синхронизировать»"
            AppLanguage.EN -> "Site → cloud → app pulls when you tap Sync"
            AppLanguage.PT -> "Site → nuvem → o app puxa ao tocar em Sincronizar"
        }

    val profileSyncCloudSignInFirst: String
        get() = when (language) {
            AppLanguage.RU -> "Войти через Google"
            AppLanguage.EN -> "Sign in with Google"
            AppLanguage.PT -> "Entrar com o Google"
        }

    val profileSyncCloudHint: String
        get() = when (language) {
            AppLanguage.RU -> "Тот же email, что на profconq.com"
            AppLanguage.EN -> "Use the same email as on profconq.com"
            AppLanguage.PT -> "Use o mesmo e-mail do profconq.com"
        }

    val profilePromoSection: String
        get() = when (language) {
            AppLanguage.RU -> "Промокод"
            AppLanguage.EN -> "Promo code"
            AppLanguage.PT -> "Código promocional"
        }

    val profilePromoPlaceholder: String
        get() = "FRIEND100"

    val profilePromoApply: String
        get() = when (language) {
            AppLanguage.RU -> "Применить"
            AppLanguage.EN -> "Apply"
            AppLanguage.PT -> "Aplicar"
        }

    val promoEnterCode: String
        get() = when (language) {
            AppLanguage.RU -> "Введите промокод"
            AppLanguage.EN -> "Enter a promo code"
            AppLanguage.PT -> "Digite o código"
        }

    val promoSignInFirst: String
        get() = when (language) {
            AppLanguage.RU -> "Войдите, чтобы применить промокод"
            AppLanguage.EN -> "Sign in to apply a promo code"
            AppLanguage.PT -> "Entre para usar um código promocional"
        }

    fun promoSuccess(limit: Int): String = when (language) {
        AppLanguage.RU -> "Лимит увеличен до $limit слов"
        AppLanguage.EN -> "Word limit increased to $limit"
        AppLanguage.PT -> "Limite aumentado para $limit palavras"
    }

    val promoInvalid: String
        get() = when (language) {
            AppLanguage.RU -> "Промокод не найден"
            AppLanguage.EN -> "Promo code not found"
            AppLanguage.PT -> "Código não encontrado"
        }

    val promoAlreadyRedeemed: String
        get() = when (language) {
            AppLanguage.RU -> "Вы уже использовали этот промокод"
            AppLanguage.EN -> "You already used this promo code"
            AppLanguage.PT -> "Você já usou este código"
        }

    val promoErrorGeneric: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось применить промокод"
            AppLanguage.EN -> "Could not apply promo code"
            AppLanguage.PT -> "Não foi possível aplicar o código"
        }

    val profileAdminSection: String
        get() = when (language) {
            AppLanguage.RU -> "Админ"
            AppLanguage.EN -> "Admin"
            AppLanguage.PT -> "Admin"
        }

    val profileAdminHint: String
        get() = when (language) {
            AppLanguage.RU -> "Вход для управления промокодами на profconq.com"
            AppLanguage.EN -> "Sign in to manage promo codes on profconq.com"
            AppLanguage.PT -> "Entrada para gerir códigos promocionais no profconq.com"
        }

    val profileAdminLogin: String
        get() = when (language) {
            AppLanguage.RU -> "Логин"
            AppLanguage.EN -> "Login"
            AppLanguage.PT -> "Utilizador"
        }

    val profileAdminPassword: String
        get() = when (language) {
            AppLanguage.RU -> "Пароль"
            AppLanguage.EN -> "Password"
            AppLanguage.PT -> "Palavra-passe"
        }

    val profileAdminSignIn: String
        get() = when (language) {
            AppLanguage.RU -> "Войти как админ"
            AppLanguage.EN -> "Sign in as admin"
            AppLanguage.PT -> "Entrar como admin"
        }

    val profileAdminSignOut: String
        get() = when (language) {
            AppLanguage.RU -> "Выйти из админки"
            AppLanguage.EN -> "Sign out of admin"
            AppLanguage.PT -> "Sair do admin"
        }

    fun profileAdminSignedInAs(username: String): String = when (language) {
        AppLanguage.RU -> "Админ: $username"
        AppLanguage.EN -> "Admin: $username"
        AppLanguage.PT -> "Admin: $username"
    }

    val profileAdminInvalidCredentials: String
        get() = when (language) {
            AppLanguage.RU -> "Неверный логин или пароль"
            AppLanguage.EN -> "Invalid login or password"
            AppLanguage.PT -> "Utilizador ou palavra-passe inválidos"
        }

    val profileAdminErrorGeneric: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось войти. Проверьте сеть."
            AppLanguage.EN -> "Could not sign in. Check your connection."
            AppLanguage.PT -> "Não foi possível entrar. Verifique a rede."
        }

    val profileStatsSection: String
        get() = when (language) {
            AppLanguage.RU -> "Статистика"
            AppLanguage.EN -> "Statistics"
            AppLanguage.PT -> "Estatísticas"
        }

    val profileInDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "в словаре"
            AppLanguage.EN -> "in dictionary"
            AppLanguage.PT -> "no dicionário"
        }

    val profileCards: String
        get() = when (language) {
            AppLanguage.RU -> "карточек"
            AppLanguage.EN -> "cards"
            AppLanguage.PT -> "cartões"
        }

    val profileProgressHint: String
        get() = when (language) {
            AppLanguage.RU -> "Серия, тепловая карта, цели и экзамен"
            AppLanguage.EN -> "Streak, heatmap, goals and exam"
            AppLanguage.PT -> "Sequência, mapa de calor, metas e exame"
        }

    val themeModeTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Тема оформления"
            AppLanguage.EN -> "Theme"
            AppLanguage.PT -> "Tema"
        }

    val themeDark: String
        get() = when (language) {
            AppLanguage.RU -> "Тёмная"
            AppLanguage.EN -> "Dark"
            AppLanguage.PT -> "Escuro"
        }

    val themeLight: String
        get() = when (language) {
            AppLanguage.RU -> "Светлая"
            AppLanguage.EN -> "Light"
            AppLanguage.PT -> "Claro"
        }

    val settingChatGptTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Умный перевод"
            AppLanguage.EN -> "Smart translation"
            AppLanguage.PT -> "Tradução inteligente"
        }

    val settingChatGptSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Нейроперевод или MyMemory"
            AppLanguage.EN -> "Neural translation or MyMemory"
            AppLanguage.PT -> "Tradução neural ou MyMemory"
        }

    val settingPhraseCopyTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Копирование фраз в словарь"
            AppLanguage.EN -> "Copy phrases to dictionary"
            AppLanguage.PT -> "Copiar frases para o dicionário"
        }

    val settingPhraseCopySubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Только слова или целые фразы"
            AppLanguage.EN -> "Single words or whole phrases"
            AppLanguage.PT -> "Só palavras ou frases inteiras"
        }

    val settingWordContextTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Пример из контекста"
            AppLanguage.EN -> "Context example"
            AppLanguage.PT -> "Exemplo do contexto"
        }

    val settingWordContextSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Фраза из контекста при добавлении"
            AppLanguage.EN -> "Context phrase when adding words"
            AppLanguage.PT -> "Frase do contexto ao adicionar"
        }

    val settingYoutubeBackgroundTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Фоновое воспроизведение видео"
            AppLanguage.EN -> "Background video playback"
            AppLanguage.PT -> "Reprodução de vídeo em segundo plano"
        }

    val settingYoutubeBackgroundSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Видео продолжит играть при переходе в другие вкладки"
            AppLanguage.EN -> "Keep playing when you switch to other tabs"
            AppLanguage.PT -> "Continua a tocar ao mudar de separador"
        }

    val youtubeBackgroundPlaying: String
        get() = when (language) {
            AppLanguage.RU -> "Видео воспроизводится"
            AppLanguage.EN -> "Video is playing"
            AppLanguage.PT -> "Vídeo a reproduzir"
        }

    val youtubeBackgroundOpen: String
        get() = when (language) {
            AppLanguage.RU -> "Видео"
            AppLanguage.EN -> "Video"
            AppLanguage.PT -> "Vídeo"
        }

    val studioBackgroundPlaying: String
        get() = when (language) {
            AppLanguage.RU -> "Студия играет"
            AppLanguage.EN -> "Studio is playing"
            AppLanguage.PT -> "Estúdio a reproduzir"
        }

    val studioBackgroundOpen: String
        get() = when (language) {
            AppLanguage.RU -> "Студия"
            AppLanguage.EN -> "Studio"
            AppLanguage.PT -> "Estúdio"
        }

    val studioBackgroundClose: String
        get() = when (language) {
            AppLanguage.RU -> "Закрыть мини-плеер"
            AppLanguage.EN -> "Close mini player"
            AppLanguage.PT -> "Fechar mini-player"
        }

    val profileSettings: String
        get() = when (language) {
            AppLanguage.RU -> "Настройки"
            AppLanguage.EN -> "Settings"
            AppLanguage.PT -> "Configurações"
        }

    val uiLanguageTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Язык интерфейса"
            AppLanguage.EN -> "Interface language"
            AppLanguage.PT -> "Idioma da interface"
        }

    val translationLanguagesTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Языки карточек и субтитров"
            AppLanguage.EN -> "Card & subtitle languages"
            AppLanguage.PT -> "Idiomas dos cartões e legendas"
        }

    val translationLanguagesHint: String
        get() = when (language) {
            AppLanguage.RU -> "Выберите 2 языка."
            AppLanguage.EN -> "Choose 2 languages."
            AppLanguage.PT -> "Escolha 2 idiomas."
        }

    fun studyLanguageName(code: Int): String = SubtitleLanguage.label(code, language)

    fun uiLanguageLabel(value: AppLanguage): String = value.displayName(language)

    fun subtitleFontSizeLabel(level: Int): String = when (language) {
        AppLanguage.RU -> when (level.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)) {
            -2 -> "Очень мелко"
            -1 -> "Мелко"
            0 -> "Обычный"
            1 -> "Крупно"
            2 -> "Очень крупно"
            else -> "Обычный"
        }
        AppLanguage.EN -> when (level.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)) {
            -2 -> "Very small"
            -1 -> "Small"
            0 -> "Normal"
            1 -> "Large"
            2 -> "Very large"
            else -> "Normal"
        }
        AppLanguage.PT -> when (level.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)) {
            -2 -> "Muito pequeno"
            -1 -> "Pequeno"
            0 -> "Normal"
            1 -> "Grande"
            2 -> "Muito grande"
            else -> "Normal"
        }
    }

    val untitled: String
        get() = when (language) {
            AppLanguage.RU -> "Без названия"
            AppLanguage.EN -> "Untitled"
            AppLanguage.PT -> "Sem título"
        }

    val syncCollectionTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Синхронизация"
            AppLanguage.EN -> "Sync"
            AppLanguage.PT -> "Sincronização"
        }

    val syncCollectionDescription: String
        get() = when (language) {
            AppLanguage.RU -> "Слова с profconq.com"
            AppLanguage.EN -> "Words from profconq.com"
            AppLanguage.PT -> "Palavras do profconq.com"
        }

    val wordsFromReader: String
        get() = when (language) {
            AppLanguage.RU -> "Слова из книги"
            AppLanguage.EN -> "Words from book"
            AppLanguage.PT -> "Palavras do livro"
        }

    val wordsFromVideo: String
        get() = when (language) {
            AppLanguage.RU -> "Слова из видео"
            AppLanguage.EN -> "Words from video"
            AppLanguage.PT -> "Palavras do vídeo"
        }

    val readerDefaultBookTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Книга"
            AppLanguage.EN -> "Book"
            AppLanguage.PT -> "Livro"
        }

    fun defaultStudySetName(date: String): String = when (language) {
        AppLanguage.RU -> "Набор от $date"
        AppLanguage.EN -> "Set from $date"
        AppLanguage.PT -> "Conjunto de $date"
    }

    val readerEmptyImportFile: String
        get() = when (language) {
            AppLanguage.RU -> "Файл пустой или не удалось прочитать текст."
            AppLanguage.EN -> "The file is empty or could not be read."
            AppLanguage.PT -> "O arquivo está vazio ou não pôde ser lido."
        }

    fun syncMirrorSuccessApp(words: Int, programs: Int): String = when (language) {
        AppLanguage.RU -> "Зеркало на сайт: $words слов, $programs программ отправлено в облако."
        AppLanguage.EN -> "Mirror to cloud: $words words, $programs programs uploaded."
        AppLanguage.PT -> "Espelho para nuvem: $words palavras, $programs programas enviados."
    }

    fun syncMirrorSuccessSite(words: Int, programs: Int): String = when (language) {
        AppLanguage.RU -> "Зеркало с сайта: $words слов, $programs программ загружено в приложение."
        AppLanguage.EN -> "Mirror from cloud: $words words, $programs programs downloaded to the app."
        AppLanguage.PT -> "Espelho do site: $words palavras, $programs programas baixados para o app."
    }

    val syncSignInGoogleFirst: String
        get() = syncSignInFirst

    val syncSignInFirst: String
        get() = when (language) {
            AppLanguage.RU -> "Сначала войдите email и паролем с сайта или через Google"
            AppLanguage.EN -> "Sign in with the site email and password, or Google"
            AppLanguage.PT -> "Entre com e-mail e senha do site, ou Google"
        }

    val syncErrorGeneric: String
        get() = when (language) {
            AppLanguage.RU -> "Ошибка синхронизации"
            AppLanguage.EN -> "Sync failed"
            AppLanguage.PT -> "Falha na sincronização"
        }

    val syncSessionExpired: String
        get() = when (language) {
            AppLanguage.RU -> "Сессия истекла. Войдите снова."
            AppLanguage.EN -> "Session expired. Sign in again."
            AppLanguage.PT -> "Sessão expirada. Entre novamente."
        }

    fun syncWordLimit(count: Int, limit: Int): String = when (language) {
        AppLanguage.RU -> "Лимит слов: $count/$limit"
        AppLanguage.EN -> "Word limit: $count/$limit"
        AppLanguage.PT -> "Limite de palavras: $count/$limit"
    }

    fun wordLimitMessage(count: Int, limit: Int): String = when (language) {
        AppLanguage.RU -> "Лимит словаря: $count из $limit слов. Войдите через Google для синхронизации или оформите Premium."
        AppLanguage.EN -> "Dictionary limit: $count of $limit words. Sign in with Google to sync or upgrade to Premium."
        AppLanguage.PT -> "Limite do dicionário: $count de $limit palavras. Entre com o Google para sincronizar ou assine Premium."
    }

    val googleSignInNetwork: String
        get() = when (language) {
            AppLanguage.RU -> "Нет сети. Проверьте интернет и повторите."
            AppLanguage.EN -> "No network. Check your connection and try again."
            AppLanguage.PT -> "Sem rede. Verifique a internet e tente de novo."
        }

    val googleSignInInternal: String
        get() = when (language) {
            AppLanguage.RU -> "Внутренняя ошибка Google Sign-In. Обычно помогает обновить Google Play Services."
            AppLanguage.EN -> "Google Sign-In internal error. Try updating Google Play Services."
            AppLanguage.PT -> "Erro interno do Google Sign-In. Atualize o Google Play Services."
        }

    val googleSignInDeveloper: String
        get() = when (language) {
            AppLanguage.RU -> "Google не узнаёт подпись этой сборки. Добавьте SHA-1 в Firebase → Android com.profconq.app. Если приложение с Play — берите SHA-1 из Play Console → App signing → App signing key."
            AppLanguage.EN -> "Google does not recognize this build's signature. Add the SHA-1 in Firebase → Android com.profconq.app. For a Play install, use Play Console → App signing → App signing key."
            AppLanguage.PT -> "O Google não reconhece a assinatura deste build. Adicione o SHA-1 no Firebase → Android com.profconq.app. Se veio da Play, use Play Console → App signing → App signing key."
        }

    fun googleSignInDeveloperWithSha(sha1: String): String = when (language) {
        AppLanguage.RU -> "${googleSignInDeveloper}\n\nSHA-1 этой установки:\n$sha1"
        AppLanguage.EN -> "${googleSignInDeveloper}\n\nSHA-1 of this install:\n$sha1"
        AppLanguage.PT -> "${googleSignInDeveloper}\n\nSHA-1 desta instalação:\n$sha1"
    }

    val googleSignInOAuth: String
        get() = when (language) {
            AppLanguage.RU -> "Ошибка OAuth-конфигурации в Firebase/Google Cloud."
            AppLanguage.EN -> "OAuth configuration error in Firebase/Google Cloud."
            AppLanguage.PT -> "Erro de configuração OAuth no Firebase/Google Cloud."
        }

    val googleSignInCancelled: String
        get() = when (language) {
            AppLanguage.RU -> "Вход отменён пользователем."
            AppLanguage.EN -> "Sign-in cancelled."
            AppLanguage.PT -> "Entrada cancelada."
        }

    val googleSignInInProgress: String
        get() = when (language) {
            AppLanguage.RU -> "Вход уже выполняется. Повторите через секунду."
            AppLanguage.EN -> "Sign-in already in progress. Try again in a moment."
            AppLanguage.PT -> "Entrada em andamento. Tente novamente em instantes."
        }

    fun googleSignInFailed(code: Int): String = when (language) {
        AppLanguage.RU -> "Ошибка входа Google (код $code)"
        AppLanguage.EN -> "Google sign-in failed (code $code)"
        AppLanguage.PT -> "Falha no login Google (código $code)"
    }

    val signOutFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось выйти"
            AppLanguage.EN -> "Sign-out failed"
            AppLanguage.PT -> "Falha ao sair"
        }

    val ytEmptySearchQuery: String
        get() = when (language) {
            AppLanguage.RU -> "Введите запрос для поиска видео."
            AppLanguage.EN -> "Enter a search query."
            AppLanguage.PT -> "Digite uma busca de vídeo."
        }

    val ytSearchFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось выполнить поиск."
            AppLanguage.EN -> "Search failed."
            AppLanguage.PT -> "Falha na busca."
        }

    val ytNoResultsAll: String
        get() = when (language) {
            AppLanguage.RU -> "Ничего не найдено. Попробуйте другой запрос."
            AppLanguage.EN -> "No results. Try another query."
            AppLanguage.PT -> "Nada encontrado. Tente outra busca."
        }

    val ytNoResultsFull: String
        get() = when (language) {
            AppLanguage.RU -> "Полные видео не найдены. Попробуйте «Все» или другой запрос."
            AppLanguage.EN -> "No full videos found. Try All or another query."
            AppLanguage.PT -> "Nenhum vídeo completo. Tente Todos ou outra busca."
        }

    val ytNoResultsShorts: String
        get() = when (language) {
            AppLanguage.RU -> "Shorts не найдены. Попробуйте «Все» или другой запрос."
            AppLanguage.EN -> "No Shorts found. Try All or another query."
            AppLanguage.PT -> "Nenhum Short encontrado. Tente Todos ou outra busca."
        }

    val ytInvalidUrl: String
        get() = when (language) {
            AppLanguage.RU -> "Вставьте корректную ссылку YouTube или ID ролика."
            AppLanguage.EN -> "Paste a valid YouTube link or video ID."
            AppLanguage.PT -> "Cole um link válido do YouTube ou ID do vídeo."
        }

    val ytReloadSubtitlesFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось перезагрузить субтитры."
            AppLanguage.EN -> "Could not reload subtitles."
            AppLanguage.PT -> "Não foi possível recarregar legendas."
        }

    val ytLoadSubtitlesFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось загрузить субтитры."
            AppLanguage.EN -> "Could not load subtitles."
            AppLanguage.PT -> "Não foi possível carregar legendas."
        }

    val ytTranslateFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось перевести."
            AppLanguage.EN -> "Translation failed."
            AppLanguage.PT -> "Falha na tradução."
        }

    val wordTranslationPhraseTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод фразы"
            AppLanguage.EN -> "Phrase translation"
            AppLanguage.PT -> "Tradução da frase"
        }

    val wordTranslationInProgress: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод…"
            AppLanguage.EN -> "Translating…"
            AppLanguage.PT -> "Traduzindo…"
        }

    val wordTranslationUnavailable: String
        get() = when (language) {
            AppLanguage.RU -> "Перевод недоступен"
            AppLanguage.EN -> "Translation unavailable"
            AppLanguage.PT -> "Tradução indisponível"
        }

    fun wordTranslationInfinitive(infinitive: String): String = when (language) {
        AppLanguage.RU -> "*инф. - $infinitive"
        AppLanguage.EN -> "*inf. - $infinitive"
        AppLanguage.PT -> "*inf. - $infinitive"
    }

    fun wordTranslationExample(example: String): String = when (language) {
        AppLanguage.RU -> "Пример: $example"
        AppLanguage.EN -> "Example: $example"
        AppLanguage.PT -> "Exemplo: $example"
    }

    val wordTranslationAdded: String
        get() = when (language) {
            AppLanguage.RU -> "Добавлено"
            AppLanguage.EN -> "Added"
            AppLanguage.PT -> "Adicionado"
        }

    val wordTranslationAddToDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить в словарь"
            AppLanguage.EN -> "Add to dictionary"
            AppLanguage.PT -> "Adicionar ao dicionário"
        }

    val wordTranslationClose: String
        get() = when (language) {
            AppLanguage.RU -> "Закрыть"
            AppLanguage.EN -> "Close"
            AppLanguage.PT -> "Fechar"
        }

    val wordTranslationTranslateButton: String
        get() = when (language) {
            AppLanguage.RU -> "Перевести"
            AppLanguage.EN -> "Translate"
            AppLanguage.PT -> "Traduzir"
        }

    val cursorModeWord: String
        get() = when (language) {
            AppLanguage.RU -> "👆 Слово"
            AppLanguage.EN -> "👆 Word"
            AppLanguage.PT -> "👆 Palavra"
        }

    val cursorModePhrase: String
        get() = when (language) {
            AppLanguage.RU -> "▎ Фраза"
            AppLanguage.EN -> "▎ Phrase"
            AppLanguage.PT -> "▎ Frase"
        }

    val ytSubtitleSearchPlaceholder: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск по субтитрам…"
            AppLanguage.EN -> "Search subtitles…"
            AppLanguage.PT -> "Pesquisar nas legendas…"
        }

    val ytSubtitleSearchHint: String
        get() = when (language) {
            AppLanguage.RU -> "Введите слово или фразу"
            AppLanguage.EN -> "Enter a word or phrase"
            AppLanguage.PT -> "Digite uma palavra ou frase"
        }

    fun ytSubtitleSearchMatchStatus(activeMatch: Int, matchCount: Int): String = when {
        matchCount == 0 -> when (language) {
            AppLanguage.RU -> "0 совпадений"
            AppLanguage.EN -> "0 matches"
            AppLanguage.PT -> "0 correspondências"
        }
        else -> when (language) {
            AppLanguage.RU -> "${activeMatch + 1} / $matchCount совпадений"
            AppLanguage.EN -> "${activeMatch + 1} / $matchCount matches"
            AppLanguage.PT -> "${activeMatch + 1} / $matchCount correspondências"
        }
    }

    val ytAddToDictionaryFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось добавить в словарь."
            AppLanguage.EN -> "Could not add to dictionary."
            AppLanguage.PT -> "Não foi possível adicionar ao dicionário."
        }

    val ytSubtitlesUnavailable: String
        get() = when (language) {
            AppLanguage.RU -> "Субтитры для этого видео недоступны."
            AppLanguage.EN -> "Subtitles are not available for this video."
            AppLanguage.PT -> "Legendas indisponíveis para este vídeo."
        }

    val ytNoMatches: String
        get() = when (language) {
            AppLanguage.RU -> "Совпадений не найдено."
            AppLanguage.EN -> "No matches found."
            AppLanguage.PT -> "Nenhuma correspondência."
        }

    val readerOpenBookFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось открыть книгу."
            AppLanguage.EN -> "Could not open book."
            AppLanguage.PT -> "Não foi possível abrir o livro."
        }

    val readerImportFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось импортировать файл."
            AppLanguage.EN -> "Could not import file."
            AppLanguage.PT -> "Não foi possível importar o arquivo."
        }

    val readerDemoNoDelete: String
        get() = when (language) {
            AppLanguage.RU -> "Демо-книгу нельзя удалить."
            AppLanguage.EN -> "Demo book cannot be deleted."
            AppLanguage.PT -> "O livro demo não pode ser excluído."
        }

    val readerImportedBookTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Импортированная книга"
            AppLanguage.EN -> "Imported book"
            AppLanguage.PT -> "Livro importado"
        }

    val progressTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Прогресс"
            AppLanguage.EN -> "Progress"
            AppLanguage.PT -> "Progresso"
        }

    fun progressGreeting(name: String): String {
        val who = name.trim()
        return when (language) {
            AppLanguage.RU -> if (who.isBlank()) "Добрый день" else "Добрый день, $who"
            AppLanguage.EN -> if (who.isBlank()) "Hello" else "Hello, $who"
            AppLanguage.PT -> if (who.isBlank()) "Bom dia" else "Bom dia, $who"
        }
    }

    fun progressDaysShort(count: Int): String = when (language) {
        AppLanguage.RU -> "$count дн"
        AppLanguage.EN -> "$count d"
        AppLanguage.PT -> "$count d"
    }

    fun progressMinutesShort(count: Int): String = when (language) {
        AppLanguage.RU -> "$count мин"
        AppLanguage.EN -> "$count min"
        AppLanguage.PT -> "$count min"
    }

    val progressStreakLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Серия"
            AppLanguage.EN -> "Streak"
            AppLanguage.PT -> "Sequência"
        }

    val progressResetTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Сбросить прогресс?"
            AppLanguage.EN -> "Reset progress?"
            AppLanguage.PT -> "Redefinir progresso?"
        }

    val progressResetBody: String
        get() = when (language) {
            AppLanguage.RU -> "Будут обнулены streak, heatmap, недельная активность и счётчики целей. Карточки и словарь останутся без изменений."
            AppLanguage.EN -> "Streak, heatmap, weekly activity and goal counters will be cleared. Cards and dictionary stay unchanged."
            AppLanguage.PT -> "Sequência, heatmap, atividade semanal e metas serão zerados. Cartões e dicionário permanecem."
        }

    val progressResetConfirm: String
        get() = when (language) {
            AppLanguage.RU -> "Сбросить"
            AppLanguage.EN -> "Reset"
            AppLanguage.PT -> "Redefinir"
        }

    val progressResetButton: String
        get() = when (language) {
            AppLanguage.RU -> "Сброс"
            AppLanguage.EN -> "Reset"
            AppLanguage.PT -> "Redefinir"
        }

    val progressGoalLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Цель"
            AppLanguage.EN -> "Goal"
            AppLanguage.PT -> "Meta"
        }

    val progressTodayLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Сегодня"
            AppLanguage.EN -> "Today"
            AppLanguage.PT -> "Hoje"
        }

    val progressActivity365: String
        get() = when (language) {
            AppLanguage.RU -> "АКТИВНОСТЬ • 365 ДНЕЙ"
            AppLanguage.EN -> "ACTIVITY • 365 DAYS"
            AppLanguage.PT -> "ATIVIDADE • 365 DIAS"
        }

    val progressHeatRecord: String
        get() = when (language) {
            AppLanguage.RU -> "рекорд"
            AppLanguage.EN -> "record"
            AppLanguage.PT -> "recorde"
        }

    val progressHeatDaysInYear: String
        get() = when (language) {
            AppLanguage.RU -> "дней в году"
            AppLanguage.EN -> "days in year"
            AppLanguage.PT -> "dias no ano"
        }

    val progressHeatAverage: String
        get() = when (language) {
            AppLanguage.RU -> "среднее"
            AppLanguage.EN -> "average"
            AppLanguage.PT -> "média"
        }

    val progressKnownWords: String
        get() = when (language) {
            AppLanguage.RU -> "ИЗВЕСТНЫЕ СЛОВА"
            AppLanguage.EN -> "KNOWN WORDS"
            AppLanguage.PT -> "PALAVRAS CONHECIDAS"
        }

    val progressWeeklyActivity: String
        get() = when (language) {
            AppLanguage.RU -> "НЕДЕЛЬНАЯ АКТИВНОСТЬ"
            AppLanguage.EN -> "WEEKLY ACTIVITY"
            AppLanguage.PT -> "ATIVIDADE SEMANAL"
        }

    val dictionaryMasteryWeak: String get() = dictionaryMastery2
    val dictionaryMasteryMedium: String get() = dictionaryMastery3
    val dictionaryMasteryGood: String get() = dictionaryMastery4

    val dictionaryMastery1: String
        get() = when (language) {
            AppLanguage.RU -> "Едва"
            AppLanguage.EN -> "Barely"
            AppLanguage.PT -> "Quase"
        }

    val dictionaryMastery2: String
        get() = when (language) {
            AppLanguage.RU -> "Слабо"
            AppLanguage.EN -> "Weak"
            AppLanguage.PT -> "Fraco"
        }

    val dictionaryMastery3: String
        get() = when (language) {
            AppLanguage.RU -> "Средне"
            AppLanguage.EN -> "Medium"
            AppLanguage.PT -> "Médio"
        }

    val dictionaryMastery4: String
        get() = when (language) {
            AppLanguage.RU -> "Хорошо"
            AppLanguage.EN -> "Good"
            AppLanguage.PT -> "Bom"
        }

    val dictionaryMastery5: String
        get() = when (language) {
            AppLanguage.RU -> "Отлично"
            AppLanguage.EN -> "Excellent"
            AppLanguage.PT -> "Ótimo"
        }

    val dictionarySourceColumn: String
        get() = when (language) {
            AppLanguage.RU -> "Источник"
            AppLanguage.EN -> "Source"
            AppLanguage.PT -> "Origem"
        }

    val dictionarySearchTablePlaceholder: String
        get() = when (language) {
            AppLanguage.RU -> "Поиск по PT, RU, примеру…"
            AppLanguage.EN -> "Search PT, translation, example…"
            AppLanguage.PT -> "Buscar PT, tradução, exemplo…"
        }

    val dictionaryRecording: String
        get() = when (language) {
            AppLanguage.RU -> "Идёт запись"
            AppLanguage.EN -> "Recording"
            AppLanguage.PT -> "Gravando"
        }

    val dictionaryRecordFolder: String
        get() = when (language) {
            AppLanguage.RU -> "Запись папки"
            AppLanguage.EN -> "Record folder"
            AppLanguage.PT -> "Gravar pasta"
        }

    val dictionaryStop: String
        get() = when (language) {
            AppLanguage.RU -> "Стоп"
            AppLanguage.EN -> "Stop"
            AppLanguage.PT -> "Parar"
        }

    fun dictionaryPickWordsForSet(selectedCount: Int): String = when (language) {
        AppLanguage.RU -> "Выберите слова для набора · Выбрано: $selectedCount"
        AppLanguage.EN -> "Pick words for deck · Selected: $selectedCount"
        AppLanguage.PT -> "Escolha palavras · Selecionadas: $selectedCount"
    }

    val dictionaryDone: String
        get() = when (language) {
            AppLanguage.RU -> "Готово"
            AppLanguage.EN -> "Done"
            AppLanguage.PT -> "Pronto"
        }

    fun dictionaryDoneCount(count: Int): String = when (language) {
        AppLanguage.RU -> "Готово ($count)"
        AppLanguage.EN -> "Done ($count)"
        AppLanguage.PT -> "Pronto ($count)"
    }

    val dictionarySendToStudio: String
        get() = when (language) {
            AppLanguage.RU -> "В Studio"
            AppLanguage.EN -> "To Studio"
            AppLanguage.PT -> "Para o Estúdio"
        }

    fun dictionarySelectedCount(count: Int): String = when (language) {
        AppLanguage.RU -> "Выбрано: $count"
        AppLanguage.EN -> "Selected: $count"
        AppLanguage.PT -> "Selecionadas: $count"
    }

    val dictionaryExportWords: String
        get() = when (language) {
            AppLanguage.RU -> "Экспорт слов"
            AppLanguage.EN -> "Export words"
            AppLanguage.PT -> "Exportar palavras"
        }

    val dictionaryStudioCollection: String
        get() = when (language) {
            AppLanguage.RU -> "Из словаря"
            AppLanguage.EN -> "From dictionary"
            AppLanguage.PT -> "Do dicionário"
        }

    val dictionaryExpandFolder: String
        get() = when (language) {
            AppLanguage.RU -> "Развернуть папку"
            AppLanguage.EN -> "Expand folder"
            AppLanguage.PT -> "Expandir pasta"
        }

    val dictionaryCollapseFolder: String
        get() = when (language) {
            AppLanguage.RU -> "Свернуть папку"
            AppLanguage.EN -> "Collapse folder"
            AppLanguage.PT -> "Recolher pasta"
        }

    val dictionaryCompactCards: String
        get() = when (language) {
            AppLanguage.RU -> "Свернуть карточки"
            AppLanguage.EN -> "Collapse cards"
            AppLanguage.PT -> "Recolher cartões"
        }

    val dictionaryExpandCards: String
        get() = when (language) {
            AppLanguage.RU -> "Развернуть карточки"
            AppLanguage.EN -> "Expand cards"
            AppLanguage.PT -> "Expandir cartões"
        }

    val sourceVideo: String
        get() = when (language) {
            AppLanguage.RU -> "Видео"
            AppLanguage.EN -> "Video"
            AppLanguage.PT -> "Vídeo"
        }

    val sourceBook: String
        get() = when (language) {
            AppLanguage.RU -> "Книга"
            AppLanguage.EN -> "Book"
            AppLanguage.PT -> "Livro"
        }

    val sourceManual: String
        get() = when (language) {
            AppLanguage.RU -> "Вручную"
            AppLanguage.EN -> "Manual"
            AppLanguage.PT -> "Manual"
        }

    val commonBack: String
        get() = when (language) {
            AppLanguage.RU -> "Назад"
            AppLanguage.EN -> "Back"
            AppLanguage.PT -> "Voltar"
        }

    val commonForward: String
        get() = when (language) {
            AppLanguage.RU -> "Вперёд"
            AppLanguage.EN -> "Forward"
            AppLanguage.PT -> "Avançar"
        }

    val studyCreateSetFromDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "Создайте набор из слов словаря — кнопка выше."
            AppLanguage.EN -> "Create a deck from dictionary words using the button above."
            AppLanguage.PT -> "Crie um conjunto das palavras do dicionário — botão acima."
        }

    val studyNewSetTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Новый набор"
            AppLanguage.EN -> "New deck"
            AppLanguage.PT -> "Novo conjunto"
        }

    val studyRenameSetTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Переименовать набор"
            AppLanguage.EN -> "Rename deck"
            AppLanguage.PT -> "Renomear conjunto"
        }

    val studySelectWords: String
        get() = when (language) {
            AppLanguage.RU -> "Выбрать слова"
            AppLanguage.EN -> "Select words"
            AppLanguage.PT -> "Selecionar palavras"
        }

    fun studySelectedWordsCount(count: Int): String = when (language) {
        AppLanguage.RU -> "Выбрано: $count слов"
        AppLanguage.EN -> "Selected: $count words"
        AppLanguage.PT -> "Selecionadas: $count palavras"
    }

    val commonRemove: String
        get() = when (language) {
            AppLanguage.RU -> "Убрать"
            AppLanguage.EN -> "Remove"
            AppLanguage.PT -> "Remover"
        }

    val commonOk: String
        get() = "OK"

    val commonListen: String
        get() = when (language) {
            AppLanguage.RU -> "Прослушать"
            AppLanguage.EN -> "Listen"
            AppLanguage.PT -> "Ouvir"
        }

    val commonFavorite: String
        get() = when (language) {
            AppLanguage.RU -> "Избранное"
            AppLanguage.EN -> "Favorite"
            AppLanguage.PT -> "Favorito"
        }

    val commonPause: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза"
            AppLanguage.EN -> "Pause"
            AppLanguage.PT -> "Pausar"
        }

    val commonPlay: String
        get() = when (language) {
            AppLanguage.RU -> "Воспроизвести"
            AppLanguage.EN -> "Play"
            AppLanguage.PT -> "Reproduzir"
        }

    val notifyPrevious: String
        get() = when (language) {
            AppLanguage.RU -> "Назад"
            AppLanguage.EN -> "Prev"
            AppLanguage.PT -> "Anterior"
        }

    val notifyNext: String
        get() = when (language) {
            AppLanguage.RU -> "Далее"
            AppLanguage.EN -> "Next"
            AppLanguage.PT -> "Seguinte"
        }

    val commonExport: String
        get() = when (language) {
            AppLanguage.RU -> "Экспорт"
            AppLanguage.EN -> "Export"
            AppLanguage.PT -> "Exportar"
        }

    val cardEditorNotFound: String
        get() = when (language) {
            AppLanguage.RU -> "Карточка не найдена"
            AppLanguage.EN -> "Card not found"
            AppLanguage.PT -> "Cartão não encontrado"
        }

    val cardEditorTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Карточка"
            AppLanguage.EN -> "Card"
            AppLanguage.PT -> "Cartão"
        }

    val cardEditorAddImage: String
        get() = when (language) {
            AppLanguage.RU -> "Добавить картинку"
            AppLanguage.EN -> "Add image"
            AppLanguage.PT -> "Adicionar imagem"
        }

    val cardEditorRecordWord: String
        get() = when (language) {
            AppLanguage.RU -> "Запись слова"
            AppLanguage.EN -> "Record word"
            AppLanguage.PT -> "Gravar palavra"
        }

    val dictionaryRecord: String
        get() = when (language) {
            AppLanguage.RU -> "Запись"
            AppLanguage.EN -> "Record"
            AppLanguage.PT -> "Gravar"
        }

    val dictionaryCardEditor: String
        get() = when (language) {
            AppLanguage.RU -> "Редактор"
            AppLanguage.EN -> "Editor"
            AppLanguage.PT -> "Editor"
        }

    val practiceExitSessionTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Завершить сессию?"
            AppLanguage.EN -> "End session?"
            AppLanguage.PT -> "Encerrar sessão?"
        }

    val practiceExitSessionMessage: String
        get() = when (language) {
            AppLanguage.RU -> "Прогресс этой сессии не будет сохранён."
            AppLanguage.EN -> "Progress from this session will not be saved."
            AppLanguage.PT -> "O progresso desta sessão não será salvo."
        }

    val practiceExitConfirm: String
        get() = when (language) {
            AppLanguage.RU -> "Выйти"
            AppLanguage.EN -> "Exit"
            AppLanguage.PT -> "Sair"
        }

    val practiceEditWord: String
        get() = when (language) {
            AppLanguage.RU -> "Редактировать слово"
            AppLanguage.EN -> "Edit word"
            AppLanguage.PT -> "Editar palavra"
        }

    val practiceRemoveFromSet: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить из набора"
            AppLanguage.EN -> "Remove from deck"
            AppLanguage.PT -> "Remover do conjunto"
        }

    val practiceReportProblem: String
        get() = when (language) {
            AppLanguage.RU -> "Сообщить о проблеме"
            AppLanguage.EN -> "Report a problem"
            AppLanguage.PT -> "Reportar problema"
        }

    fun studySessionCardProgress(current: Int, total: Int): String = when (language) {
        AppLanguage.RU -> "Карточка $current из $total"
        AppLanguage.EN -> "Card $current of $total"
        AppLanguage.PT -> "Cartão $current de $total"
    }

    fun wordAddedToDictionary(word: String): String = when (language) {
        AppLanguage.RU -> "«$word» добавлено в словарь"
        AppLanguage.EN -> "«$word» added to dictionary"
        AppLanguage.PT -> "«$word» adicionada ao dicionário"
    }

    val ytRemoveFromHistory: String
        get() = when (language) {
            AppLanguage.RU -> "Удалить из истории"
            AppLanguage.EN -> "Remove from history"
            AppLanguage.PT -> "Remover do histórico"
        }

    fun ytEmbeddedPlayerError(message: String): String = when (language) {
        AppLanguage.RU -> "Встроенный плеер: $message"
        AppLanguage.EN -> "Embedded player: $message"
        AppLanguage.PT -> "Player embutido: $message"
    }

    val ytOpenInYoutube: String
        get() = when (language) {
            AppLanguage.RU -> "Открыть в YouTube"
            AppLanguage.EN -> "Open in YouTube"
            AppLanguage.PT -> "Abrir no YouTube"
        }

    val ytPlayerErrorNotEmbeddable: String
        get() = when (language) {
            AppLanguage.RU -> "видео нельзя смотреть во встроенном плеере"
            AppLanguage.EN -> "video cannot be played in the embedded player"
            AppLanguage.PT -> "o vídeo não pode ser reproduzido no player embutido"
        }

    val ytPlayerErrorNotFound: String
        get() = when (language) {
            AppLanguage.RU -> "видео не найдено"
            AppLanguage.EN -> "video not found"
            AppLanguage.PT -> "vídeo não encontrado"
        }

    fun ytPlayerErrorGeneric(code: String): String = when (language) {
        AppLanguage.RU -> "ошибка $code"
        AppLanguage.EN -> "error $code"
        AppLanguage.PT -> "erro $code"
    }

    val readerFontSizeLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Размер шрифта"
            AppLanguage.EN -> "Font size"
            AppLanguage.PT -> "Tamanho da fonte"
        }

    val readerLineSpacingLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Межстрочный интервал"
            AppLanguage.EN -> "Line spacing"
            AppLanguage.PT -> "Espaçamento entre linhas"
        }

    fun readerFontSizeValue(percent: String): String = "$readerFontSizeLabel: $percent"

    fun readerLineSpacingValue(percent: Int): String = "$readerLineSpacingLabel: $percent%"

    val readerAutoScrollLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Автоскролл"
            AppLanguage.EN -> "Auto-scroll"
            AppLanguage.PT -> "Rolagem automática"
        }

    val readerAutoScrollPlay: String
        get() = when (language) {
            AppLanguage.RU -> "Запустить автоскролл"
            AppLanguage.EN -> "Start auto-scroll"
            AppLanguage.PT -> "Iniciar rolagem automática"
        }

    val readerAutoScrollPause: String
        get() = when (language) {
            AppLanguage.RU -> "Пауза автоскролла"
            AppLanguage.EN -> "Pause auto-scroll"
            AppLanguage.PT -> "Pausar rolagem automática"
        }

    val readerAutoScrollSpeedLabel: String
        get() = when (language) {
            AppLanguage.RU -> "Скорость прокрутки"
            AppLanguage.EN -> "Scroll speed"
            AppLanguage.PT -> "Velocidade"
        }

    fun readerAutoScrollSpeedValue(speed: Int): String {
        val percent = ReaderAutoScroll.speedPercent(speed)
        return when (language) {
            AppLanguage.RU -> "$percent%"
            AppLanguage.EN -> "$percent%"
            AppLanguage.PT -> "$percent%"
        }
    }

    val readerBookmarkSaved: String
        get() = when (language) {
            AppLanguage.RU -> "Закладка сохранена"
            AppLanguage.EN -> "Bookmark saved"
            AppLanguage.PT -> "Marcador salvo"
        }

    val readerBookmarkRemoved: String
        get() = when (language) {
            AppLanguage.RU -> "Закладка снята"
            AppLanguage.EN -> "Bookmark removed"
            AppLanguage.PT -> "Marcador removido"
        }

    fun readerBookmarkPageHint(page: Int): String = when (language) {
        AppLanguage.RU -> "Страница $page · долгое нажатие на ★ — перейти"
        AppLanguage.EN -> "Page $page · long-press ★ to jump"
        AppLanguage.PT -> "Página $page · toque longo em ★ para ir"
    }

    val readerBookmarkSetAgainHint: String
        get() = when (language) {
            AppLanguage.RU -> "Можно поставить снова на текущей странице"
            AppLanguage.EN -> "You can set it again on the current page"
            AppLanguage.PT -> "Você pode definir novamente na página atual"
        }

    val readerPreviousPage: String
        get() = when (language) {
            AppLanguage.RU -> "Предыдущая страница"
            AppLanguage.EN -> "Previous page"
            AppLanguage.PT -> "Página anterior"
        }

    val readerNextPage: String
        get() = when (language) {
            AppLanguage.RU -> "Следующая страница"
            AppLanguage.EN -> "Next page"
            AppLanguage.PT -> "Próxima página"
        }

    val readerBookmarkLongPressHint: String
        get() = when (language) {
            AppLanguage.RU -> "Закладка (долгое нажатие — перейти)"
            AppLanguage.EN -> "Bookmark (long-press to jump)"
            AppLanguage.PT -> "Marcador (toque longo para ir)"
        }

    val readerSetBookmark: String
        get() = when (language) {
            AppLanguage.RU -> "Поставить закладку"
            AppLanguage.EN -> "Set bookmark"
            AppLanguage.PT -> "Definir marcador"
        }

    val readerGoToPageTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Перейти на страницу"
            AppLanguage.EN -> "Go to page"
            AppLanguage.PT -> "Ir para página"
        }

    fun readerPageFieldLabel(totalPages: Int): String = when (language) {
        AppLanguage.RU -> "Страница (1–$totalPages)"
        AppLanguage.EN -> "Page (1–$totalPages)"
        AppLanguage.PT -> "Página (1–$totalPages)"
    }

    val readerGoButton: String
        get() = when (language) {
            AppLanguage.RU -> "Перейти"
            AppLanguage.EN -> "Go"
            AppLanguage.PT -> "Ir"
        }

    val readerFontAndSpacing: String
        get() = when (language) {
            AppLanguage.RU -> "Шрифт и интервал"
            AppLanguage.EN -> "Font and spacing"
            AppLanguage.PT -> "Fonte e espaçamento"
        }

    val readerDocNotSupported: String
        get() = when (language) {
            AppLanguage.RU -> "Формат .doc не поддерживается. Сохраните файл как .docx."
            AppLanguage.EN -> ".doc format is not supported. Save the file as .docx."
            AppLanguage.PT -> "Formato .doc não suportado. Salve o arquivo como .docx."
        }

    val readerDocxReadFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось прочитать .docx файл."
            AppLanguage.EN -> "Could not read the .docx file."
            AppLanguage.PT -> "Não foi possível ler o arquivo .docx."
        }

    val readerFileReadFailed: String
        get() = when (language) {
            AppLanguage.RU -> "Не удалось прочитать файл."
            AppLanguage.EN -> "Could not read the file."
            AppLanguage.PT -> "Não foi possível ler o arquivo."
        }

    val audioFileNotFound: String
        get() = when (language) {
            AppLanguage.RU -> "Аудиофайл не найден"
            AppLanguage.EN -> "Audio file not found"
            AppLanguage.PT -> "Arquivo de áudio não encontrado"
        }

    val audioExportChooserTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Экспорт аудио"
            AppLanguage.EN -> "Export audio"
            AppLanguage.PT -> "Exportar áudio"
        }

    val audioExportM4aOriginal: String
        get() = when (language) {
            AppLanguage.RU -> "M4A (оригинал)"
            AppLanguage.EN -> "M4A (original)"
            AppLanguage.PT -> "M4A (original)"
        }

    val audioCreatingMp3: String
        get() = when (language) {
            AppLanguage.RU -> "Создание MP3…"
            AppLanguage.EN -> "Creating MP3…"
            AppLanguage.PT -> "Criando MP3…"
        }

    fun audioMp3ExportFailed(message: String?): String = when (language) {
        AppLanguage.RU -> "Не удалось создать MP3: ${message ?: "ошибка"}"
        AppLanguage.EN -> "Could not create MP3: ${message ?: "error"}"
        AppLanguage.PT -> "Não foi possível criar MP3: ${message ?: "erro"}"
    }

    val translateAuthRequired: String
        get() = when (language) {
            AppLanguage.RU -> "Войдите через Google для перевода ChatGPT"
            AppLanguage.EN -> "Sign in with Google to use ChatGPT translation"
            AppLanguage.PT -> "Entre com Google para tradução ChatGPT"
        }

    fun progressWeeklyActivityLabel(kind: com.profconq.app.data.model.WeeklyActivityKind): String = when (kind) {
        com.profconq.app.data.model.WeeklyActivityKind.Cards -> when (language) {
            AppLanguage.RU -> "Карточки"
            AppLanguage.EN -> "Cards"
            AppLanguage.PT -> "Cartões"
        }
        com.profconq.app.data.model.WeeklyActivityKind.YoutubeHours -> when (language) {
            AppLanguage.RU -> "YouTube часы"
            AppLanguage.EN -> "YouTube hours"
            AppLanguage.PT -> "Horas no YouTube"
        }
        com.profconq.app.data.model.WeeklyActivityKind.WordsRead -> when (language) {
            AppLanguage.RU -> "Слова в чтении"
            AppLanguage.EN -> "Words read"
            AppLanguage.PT -> "Palavras lidas"
        }
        com.profconq.app.data.model.WeeklyActivityKind.Speech -> when (language) {
            AppLanguage.RU -> "Запись речи"
            AppLanguage.EN -> "Speech recording"
            AppLanguage.PT -> "Gravação de fala"
        }
        com.profconq.app.data.model.WeeklyActivityKind.Writing -> when (language) {
            AppLanguage.RU -> "Письменные задания"
            AppLanguage.EN -> "Writing tasks"
            AppLanguage.PT -> "Tarefas de escrita"
        }
    }

    val progressHoursUnit: String
        get() = when (language) {
            AppLanguage.RU -> "ч"
            AppLanguage.EN -> "h"
            AppLanguage.PT -> "h"
        }

    fun audioExportLabels(): com.profconq.app.media.AudioExportLabels = com.profconq.app.media.AudioExportLabels(
        chooserTitle = audioExportChooserTitle,
        m4aOption = audioExportM4aOriginal,
        creatingMp3 = audioCreatingMp3,
        mp3Failed = ::audioMp3ExportFailed,
    )

    fun userVisibleError(error: Throwable, fallback: String): String = when (error) {
        is com.profconq.app.youtube.TranslationException.AuthRequired -> translateAuthRequired
        is com.profconq.app.youtube.TranslationException.WordLimit -> syncWordLimit(error.count, error.limit)
        is com.profconq.app.reader.ReaderImportException -> when (error.reason) {
            com.profconq.app.reader.ReaderImportException.Reason.DOC_NOT_SUPPORTED -> readerDocNotSupported
            com.profconq.app.reader.ReaderImportException.Reason.DOCX_READ_FAILED -> readerDocxReadFailed
            com.profconq.app.reader.ReaderImportException.Reason.FILE_READ_FAILED -> readerFileReadFailed
        }
        else -> error.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    companion object {
        fun forLanguage(language: AppLanguage) = UiStrings(language)
    }
}
