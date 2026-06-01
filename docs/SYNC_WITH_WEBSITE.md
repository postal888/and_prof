# Синхронизация приложения с profconq.com

## Как устроено

| Компонент | Формат |
|-----------|--------|
| Сайт | `GET/PUT /api/sync/vocabulary` + cookie-сессия (email/пароль) |
| Android | Тот же URL + `Authorization: Bearer <Firebase idToken>` |

Тело запроса/ответа (как на сайте):

```json
{
  "json": {
    "words": [
      {
        "id": 1,
        "word": "falar",
        "translation": "говорить",
        "example": "...",
        "tag": "verbo",
        "videoId": "abc123",
        "videoTitle": "Название ролика"
      }
    ]
  }
}
```

Приложение при синхронизации:

1. Забирает словарь с сервера
2. Объединяет с локальными карточками (по нормализованному PT)
3. Отправляет объединённый список на сервер
4. Обновляет Room из результата

## Важно: один аккаунт

- **Сайт** — вход по email и паролю
- **Приложение** — вход через Google (Firebase)

Чтобы видеть одни и те же слова:

1. На телефоне войдите через Google с **тем же email**, что на сайте
2. На сервере должен быть включён приём Firebase-токена для `/api/sync/vocabulary` (см. `server/`)
3. Нажмите **Синхронизировать словарь** в профиле приложения

Пока на production не задеплоен Firebase для `/api/sync/vocabulary`, синхронизация с телефона вернёт **401** — это ожидаемо.

## Проверка на production

### 1. API жив (после деплоя `server/`)

```bash
curl -s https://profconq.com/health
# ожидается: {"ok":true,...}  а не HTML
```

### 2. Словарь на сайте (браузер)

1. Откройте https://profconq.com
2. Войдите email/паролем
3. Вкладка со словарём / карточками — добавьте тестовое слово `teste_sync` / `тест`
4. Обновите страницу — слово должно остаться

### 3. С телефона

1. Установите свежий APK
2. **Профиль** → **Войти через Google** (тот же email)
3. **Синхронизировать словарь** — сообщение «Синхронизировано с сайтом: N слов»
4. На сайте обновите страницу — должны появиться слова из приложения (и наоборот)

### 4. Тест с ПК (если есть Firebase idToken)

```bash
curl -s -H "Authorization: Bearer YOUR_ID_TOKEN" \
  https://profconq.com/api/sync/vocabulary
```

## Деплой на profconq.com

В nginx для API-маршрутов проксируйте на Node (`server/`), а не на SPA:

- `/api/sync/vocabulary`
- `/api/book-translate`
- `/api/me`
- `/health`

В middleware существующего backend добавьте проверку Firebase Bearer (образец: `server/src/middleware/firebaseAuth.js`) **или** замените API-часть на наш `server/`.

Таблица `account_vocabulary` хранит тот же JSON, что и сайт для залогиненного пользователя (после связывания по `firebase_uid` / email).
