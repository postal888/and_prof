package com.profconq.app.ui.i18n

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
            AppLanguage.RU -> "Нет книг. Импортируйте .txt или .docx."
            AppLanguage.EN -> "No books yet. Import a .txt or .docx file."
            AppLanguage.PT -> "Sem livros. Importe um arquivo .txt ou .docx."
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
            AppLanguage.RU -> "Снимите галочку, чтобы убрать слово из набора. После прогона в наборе остаются только слова с «Не знаю»."
            AppLanguage.EN -> "Uncheck to remove a word from the deck. After a run, only «Don't know» words stay in the deck."
            AppLanguage.PT -> "Desmarque para remover a palavra do conjunto. Após a sessão, ficam só as palavras com «Não sei»."
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
        get() = when (language) {
            AppLanguage.RU -> "Сначала войдите через Google"
            AppLanguage.EN -> "Sign in with Google first"
            AppLanguage.PT -> "Entre com o Google primeiro"
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
            AppLanguage.RU -> "Ошибка конфигурации Firebase (DEVELOPER_ERROR). Проверьте SHA-1/SHA-256 в google-services.json."
            AppLanguage.EN -> "Firebase configuration error (DEVELOPER_ERROR). Check SHA-1/SHA-256 in google-services.json."
            AppLanguage.PT -> "Erro de configuração Firebase (DEVELOPER_ERROR). Verifique SHA-1/SHA-256 no google-services.json."
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

    val dictionaryMasteryWeak: String
        get() = when (language) {
            AppLanguage.RU -> "Слабо"
            AppLanguage.EN -> "Weak"
            AppLanguage.PT -> "Fraco"
        }

    val dictionaryMasteryMedium: String
        get() = when (language) {
            AppLanguage.RU -> "Средне"
            AppLanguage.EN -> "Medium"
            AppLanguage.PT -> "Médio"
        }

    val dictionaryMasteryGood: String
        get() = when (language) {
            AppLanguage.RU -> "Хорошо"
            AppLanguage.EN -> "Good"
            AppLanguage.PT -> "Bom"
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

    val studyCreateSetFromDictionary: String
        get() = when (language) {
            AppLanguage.RU -> "Создайте набор из слов словаря — кнопка выше."
            AppLanguage.EN -> "Create a deck from dictionary words using the button above."
            AppLanguage.PT -> "Crie um conjunto das palavras do dicionário — botão acima."
        }

    companion object {
        fun forLanguage(language: AppLanguage) = UiStrings(language)
    }
}
