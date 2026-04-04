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
| `TELEGRAM_TOKEN` | Токен бота | `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11` |
| `TELEGRAM_BOT_NAME` | Имя бота | `MyBot` |
| `RECIPIENT_CHAT_ID` | Chat ID получателя | `123456789` |
| `ADMIN_CHAT_ID` | Chat ID администратора | `123456789` |
| `PORT` | Порт (по умолчанию 8080) | `8080` |

Или через application.properties:

```properties
bot.token=ВАШ_ТОКЕН
bot.username=ИМЯ_БОТА
bot.recipient=CHAT_ID
bot.admin-id=CHAT_ID
bot.time=09:00
```

## Команды бота

- `/start` - Запуск
- `/time` - Показать время отправки
- `/settime <время>` - Установить время
- `/messages` - Показать список сообщений
- `/add <текст>` - Добавить сообщение
- `/recipient` - Показать получателя
- `/msg <текст>` - Отправить сообщение получателю
- `/now` - Отправить сообщение сейчас
- `/logs` - Показать логи
