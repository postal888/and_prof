# Проверка Google Sign-In и ChatGPT на телефоне

## На сервере (profconq.com)

1. PostgreSQL: `npm run db:init` в папке `server/`
2. `.env` с `DATABASE_URL`, `OPENAI_API_KEY`, `FIREBASE_SERVICE_ACCOUNT_PATH`
3. `serviceAccountKey.json` из Firebase Console → Project settings → Service accounts
4. Запуск API и прокси nginx на `https://profconq.com`

Проверка: `curl https://profconq.com/health` → `{"ok":true}`

## На телефоне (Xiaomi)

1. Android Studio → проект `E:\GIT\Android\Profconq` → Run на устройстве
2. Если стояло старое приложение:  
   `adb uninstall com.proficon.app`  
   `adb uninstall com.profconq.app`
3. **Профиль** → **Войти через Google** → имя/email в блоке аккаунта
4. **Профиль** → включить **ChatGPT для перевода**
5. **YouTube** или **Читалка** → выделить слово/фразу → перевод через сервер
6. Без входа ChatGPT должен показать: «Войдите через Google…»
7. **Синхронизировать словарь** → счётчик «Слова в облаке: N / 50»

## Ожидаемое

| Действие | Результат |
|----------|-----------|
| Вход Google | email в профиле, авто-синхронизация |
| Перевод с ChatGPT | ответ от `/api/book-translate` |
| 401 / истёк токен | повтор с refresh или «Войдите снова» |
| >50 слов (бесплатно) | HTTP 402, сообщение о лимите |
