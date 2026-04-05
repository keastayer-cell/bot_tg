# Telegram Bot на Spring Boot

## Структура проекта

```
src/main/java/com/example/bot/
├── BotApplication.java       # Главный класс
├── BotConfig.java            # Конфигурация бота
├── TelegramConfig.java       # Конфигурация Telegram клиента
├── TelegramService.java      # Сервис для отправки сообщений
├── MessageService.java       # Работа с сообщениями
├── MyTelegramBot.java        # Основной бот
└── MessageScheduler.java     # Планировщик отправки

src/main/resources/
└── application.properties    # Настройки приложения
```

## Сборка

```bash
mvn clean package
```

## Запуск

```bash
java -jar target/telegram-bot-1.0.0.jar
```

## Настройка

Параметры передаются через переменные окружения:

| Параметр | Описание | Пример |
|----------|----------|--------|
| `BOT_TOKEN` | Токен бота | `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11` |
| `BOT_USERNAME` | Имя бота | `MyBot` |
| `ADMIN_CHAT_ID` | Chat ID администратора | `123456789` |
| `TIMEZONE` | Часовой пояс | `Europe/Moscow` |
| `SCHEDULER_INTERVAL` | Интервал планировщика (мс) | `60000` |
| `PORT` | Порт (по умолчанию 8080) | `8080` |

Или через config.properties:

```properties
time=09:00
timezone=Europe/Moscow
admin-id=123456789
last_sent=
```

## Команды бота

### Админ команды:
- `/start` - Показать меню команд
- `/recipients` - Список получателей
- `/addrecipient <ID>` - Добавить получателя
- `/delrecipient <ID>` - Удалить получателя
- `/time` - Показать время отправки
- `/settime <HH:MM>` - Установить время отправки
- `/settimezone <timezone>` - Установить часовой пояс (например, Europe/Moscow)
- `/now` - Отправить сейчас
- `/messages` - Список текстов
- `/add <текст>` - Добавить текст в очередь
- `/queue` - Показать очередь
- `/fillqueue` - Заполнить очередь из текстов и картинок
- `/images` - Список картинок
- `/sendimage <N>` - Отправить N-ю картинку
- `/sendimageall` - Отправить все картинки
- `/stats` - Статистика
- `/logs` - Показать логи
- `/msg <текст>` - Отправить текст всем

### Пользовательские команды:
- `/start` - Подписаться на рассылку
- Кнопка "🚀 Старт" - то же
- Кнопка "📬 Админу" - написать админу
