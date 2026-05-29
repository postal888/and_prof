package com.proficon.app.ui.i18n

import com.proficon.app.data.model.SubtitleFontSize

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

    val demoSubtitles: String
        get() = when (language) {
            AppLanguage.RU -> "Демо субтитры"
            AppLanguage.EN -> "Demo subtitles"
            AppLanguage.PT -> "Legendas demo"
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

    val demoSubtitlesTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Субтитры (демо)"
            AppLanguage.EN -> "Subtitles (demo)"
            AppLanguage.PT -> "Legendas (demo)"
        }

    val subtitleFontSizeTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Размер субтитров"
            AppLanguage.EN -> "Subtitle size"
            AppLanguage.PT -> "Tamanho das legendas"
        }

    val subtitleFontSizeSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "YouTube и читалка. Крупнее или мельче текст строк субтитров."
            AppLanguage.EN -> "YouTube and reader. Make subtitle lines larger or smaller."
            AppLanguage.PT -> "YouTube e leitura. Aumente ou diminua o texto das legendas."
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
            AppLanguage.RU -> "Выберите набор для карточек"
            AppLanguage.EN -> "Choose a deck for flashcards"
            AppLanguage.PT -> "Escolha um conjunto de cartões"
        }

    val studyCollectionsSection: String
        get() = when (language) {
            AppLanguage.RU -> "Наборы"
            AppLanguage.EN -> "Decks"
            AppLanguage.PT -> "Conjuntos"
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
            AppLanguage.RU -> "YouTube-практика"
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
            AppLanguage.RU -> "Нет книг. Импортируйте .txt / .docx или откройте демо ниже."
            AppLanguage.EN -> "No books yet. Import .txt / .docx or open the demo below."
            AppLanguage.PT -> "Sem livros. Importe .txt / .docx ou abra a demo abaixo."
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

    val profileTagline: String
        get() = when (language) {
            AppLanguage.RU -> "Proficiência Conquistada"
            AppLanguage.EN -> "Proficiency Achieved"
            AppLanguage.PT -> "Proficiência Conquistada"
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
            AppLanguage.RU -> "Streak, heatmap, цели и экзамен"
            AppLanguage.EN -> "Streak, heatmap, goals and exam"
            AppLanguage.PT -> "Sequência, heatmap, metas e exame"
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
            AppLanguage.RU -> "ChatGPT для перевода"
            AppLanguage.EN -> "ChatGPT translation"
            AppLanguage.PT -> "Tradução com ChatGPT"
        }

    val settingChatGptSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Включено — gentechnet.com (OpenAI). Выключено — MyMemory."
            AppLanguage.EN -> "On — gentechnet.com (OpenAI). Off — MyMemory."
            AppLanguage.PT -> "Ativo — gentechnet.com (OpenAI). Inativo — MyMemory."
        }

    val settingPhraseCopyTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Копирование фраз в словарь"
            AppLanguage.EN -> "Copy phrases to dictionary"
            AppLanguage.PT -> "Copiar frases para o dicionário"
        }

    val settingPhraseCopySubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Выключите — только слова по тапу. Режим «фраза» и добавление фраз отключены."
            AppLanguage.EN -> "Off — tap adds words only. Phrase mode and phrase saving disabled."
            AppLanguage.PT -> "Desligado — só palavras ao toque. Modo frase desativado."
        }

    val settingWordContextTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Пример из контекста"
            AppLanguage.EN -> "Context example"
            AppLanguage.PT -> "Exemplo do contexto"
        }

    val settingWordContextSubtitle: String
        get() = when (language) {
            AppLanguage.RU -> "Включено — при тапе на слово в карточку добавляется фраза между знаками препинания. Выключено — только слово."
            AppLanguage.EN -> "On — tap saves the phrase between punctuation. Off — word only."
            AppLanguage.PT -> "Ativo — ao toque salva a frase entre pontuação. Inativo — só a palavra."
        }

    val profileAboutSection: String
        get() = when (language) {
            AppLanguage.RU -> "О приложении"
            AppLanguage.EN -> "About"
            AppLanguage.PT -> "Sobre o app"
        }

    val profileAboutText: String
        get() = when (language) {
            AppLanguage.RU -> "Proficon переводит слова через ChatGPT (gentechnet.com) или MyMemory. " +
                "Озвучка слов — через ElevenLabs (в веб-версии PortuPrep)."
            AppLanguage.EN -> "Proficon translates words via ChatGPT (gentechnet.com) or MyMemory. " +
                "Word audio uses ElevenLabs (in the PortuPrep web app)."
            AppLanguage.PT -> "O Proficon traduz palavras via ChatGPT (gentechnet.com) ou MyMemory. " +
                "Áudio das palavras via ElevenLabs (na versão web PortuPrep)."
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

    val subtitleLanguageTitle: String
        get() = when (language) {
            AppLanguage.RU -> "Язык субтитров YouTube"
            AppLanguage.EN -> "YouTube subtitle language"
            AppLanguage.PT -> "Idioma das legendas do YouTube"
        }

    fun uiLanguageLabel(value: AppLanguage): String = when (language) {
        AppLanguage.RU -> when (value) {
            AppLanguage.RU -> "Русский"
            AppLanguage.EN -> "English"
            AppLanguage.PT -> "Português"
        }
        AppLanguage.EN -> when (value) {
            AppLanguage.RU -> "Russian"
            AppLanguage.EN -> "English"
            AppLanguage.PT -> "Portuguese"
        }
        AppLanguage.PT -> when (value) {
            AppLanguage.RU -> "Russo"
            AppLanguage.EN -> "Inglês"
            AppLanguage.PT -> "Português"
        }
    }

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

    companion object {
        fun forLanguage(language: AppLanguage) = UiStrings(language)
    }
}
