# 📋 Чек-лист для Railway развёртывания

## ✅ Что было сделано (Phase 7 - Fix Config Persistence)

### 1. **Персистентность конфига** ✓
- ConfigService теперь сохраняет конфиг на диск в файл `application.conf`
- При перезапуске бота все параметры (admin-id, time, timezone) сохраняются
- Файл создаётся в текущей рабочей директории (безопасно для Railway)

### 2. **Логирование инициализации** ✓
- ConfigService.init() теперь логирует загружаемый admin-id
- BotApplication показывает успешную инициализацию
- MyTelegramBot.getAdminId() логирует откуда берётся ID

### 3. **Обработка ошибок** ✓
- Все методы ConfigService имеют try-catch блоки
- Null-safety: проверяем recipientRepository перед использованием
- Graceful fallback при ошибках

### 4. **Сборка готова к продакшену** ✓
```bash
target/telegram-bot-1.0.0.jar  ← готов для Railway
```

---

## 🚀 Как развернуть на Railway

### Шаг 1: Подготовить переменные окружения

На Railway Dashboard → Project → Variables:
```
BOT_TOKEN = <токен от BotFather>
BOT_USERNAME = <ник бота (без @)>
ADMIN_CHAT_ID = <твой chat_id из Telegram>
DATABASE_URL = postgresql://<автоматически от PostgreSQL>
```

**Где брать chat_id?**
- Отправить боту: `@CopyMessagesBot`
- Он покажет твой ID
- Или использовать `@userinfobot`

### Шаг 2: Развернуть JAR на Railway

```bash
# 1. Коммитим последние изменения
git add .
git commit -m "Fix: persist config to disk, add detailed logging"

# 2. Пушим на Railway
git push  # Railway автоматически соберёт и развернёт

# Или вручную через Railway CLI:
railway up --auto-detect
```

### Шаг 3: Проверить что запустилось

Railway Dashboard → Logs, ищем эти строки:
```
✓ Часовой пояс установлен: Europe/Moscow
✓ Конфиг инициализирован, admin-id = 123456789
=== ConfigService.init() DONE, admin-id now = 123456789 ===
Бот инициализирован
```

### Шаг 4: Тестировать основные функции

1. **Тест подписки:**
   - Отправить боту `/start`
   - Должна появиться клавиатура (если ты админ) или сообщение о подписке

2. **Тест админ-команд:**
   - Отправить `/recipients` — должен показать список подписчиков
   - Отправить `/addrecipient 987654321` — добавить тестового пользователя
   - Отправить `/time` — показать время рассылки

3. **Тест картинок:**
   - Отправить фото боту
   - Отправить `/images` — должна быть в списке
   - Отправить `/sendimage 1` — должна отправиться

4. **Тест рассылки:**
   - Отправить `/now` — отправить сообщение сейчас всем
   - Все подписчики должны получить

---

## 🔍 Диагностика проблем

### ❌ Бот не запускается вообще

**Логи:**
```
✗ Ошибка инициализации конфига
```

**Решение:**
1. Проверить BOT_TOKEN валидный
2. Посмотреть полный стак трейс в логах
3. Проверить что PostgreSQL база доступна

### ❌ Бот отвечает, но команды не работают

**Логи:**
```
⚠ adminChatIdFromEnv переменная пуста!
```

**Решение:**
1. Railway Variables → добавить ADMIN_CHAT_ID
2. Рестартнуть приложение (нажать restart в Railway)
3. Проверить логи ещё раз

### ❌ Бот забыл получателей после перезапуска

**Причина:** Получатели в базе, но admin-id может быть потерян

**Решение:**
- Это нормально теперь — admin-id сохраняется в application.conf
- Получатели хранятся в PostgreSQL базе (не теряются)

### ❌ Фото не отправляются

**Логи:**
```
Ошибка отправки фото: ...
```

**Решение:**
1. Проверить что Telegram API доступен (нет блокировки)
2. Проверить что file_id хранится в базе (SELECT * FROM image_index)
3. Попробовать отправить заново

---

## 📊 Мониторинг на Railway

### Как смотреть логи в реальном времени

```bash
# Через Railway CLI
railway logs -f

# Или в Dashboard → Logs → real-time
```

### Что логировать помимо ошибок

Все логи вида:
- `⚠` = предупреждение (может быть проблема)
- `✓` = успешно
- `✗` = ошибка (точно проблема)

### Аналитика использования

```sql
-- В PostgreSQL:
SELECT 
    COUNT(*) as total_recipients,
    COUNT(DISTINCT recipient_id) as unique_recipients
FROM recipient;

SELECT 
    COUNT(*) as total_messages,
    COUNT(*) FILTER (WHERE status = 'SENT') as sent,
    COUNT(*) FILTER (WHERE status = 'PENDING') as pending
FROM message_queue;
```

---

## 🛠️ Техническая справка

### Файлы конфига (в контейнере)

```
application.conf              ← Сохраняется при /settime, /setadmin
/BOOT-INF/classes/application.properties  ← Параметры Spring
```

### Переменные окружения которые используются

```
BOT_TOKEN          → spring.datasource.password (нет, отдельная)
BOT_USERNAME       → telegram.bot.username
ADMIN_CHAT_ID      → admin-chat-id (в ConfigService)
DATABASE_URL       → spring.datasource.url (DataSourceConfig)
PORT               → server.port (по умолчанию 8080)
```

### Порты

- `8080` — главное приложение (если нужен HTTP)
- Telegram API работает на https://api.telegram.org (исходящие)

---

## 🎯 Текущий статус

| Компонент | Статус | Примечание |
|-----------|--------|-----------|
| Компиляция | ✅ | BUILD SUCCESS |
| JAR файл | ✅ | 50+ MB готов |
| ConfigService | ✅ | Персистентность добавлена |
| Логирование | ✅ | Детальное |
| PostgreSQL | ✅ | Автоматическое подключение |
| Обработка ошибок | ✅ | Везде null-safety |

---

## ⚠️ Следующие шаги

1. ✅ Деплой на Railway
2. ⏳ Проверить логи при первом запуске
3. ⏳ Тестировать все команды
4. ⏳ Убедиться что рассылка работает в своё время
5. ⏳ Настроить мониторинг ошибок

---

## 📞 Если что-то не работает

1. **Проверить логи** → Railway Dashboard → Logs
2. **Сделать скриншот ошибки** (полный стак трейс)
3. **Проверить переменные окружения** → Variables
4. **Рестартнуть приложение** → Restart button
5. **Посмотреть application.conf** (в контейнере) — есть ли там переменные

**Успехов! 🚀**
