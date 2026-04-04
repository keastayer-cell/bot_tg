import json
import random
import os
import logging
from datetime import datetime
import asyncio

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# Flask для Render
try:
    from flask import Flask, Response
    flask_app = Flask(__name__)
    @flask_app.route('/')
    def home():
        return Response('OK', status=200)
except ImportError:
    flask_app = None

try:
    from telegram import Bot, Update
    from telegram.error import TelegramError
    from telegram.ext import Application, CommandHandler, ContextTypes
except ImportError:
    logger.error("python-telegram-bot не установлен!")
    exit(1)

# Глобальные переменные
bot = None
app = None

def load_config():
    with open('config.json', 'r', encoding='utf-8') as f:
        return json.load(f)

def save_config(config):
    with open('config.json', 'w', encoding='utf-8') as f:
        json.dump(config, f, ensure_ascii=False, indent=4)

def load_messages():
    if not os.path.exists('messages.txt'):
        with open('messages.txt', 'w', encoding='utf-8') as f:
            f.write("Доброе утро! ☀️\nС добрым утром!\n")
        return ["Доброе утро! ☀️"]
    with open('messages.txt', 'r', encoding='utf-8') as f:
        return [line.strip() for line in f if line.strip()]

def save_messages(messages):
    with open('messages.txt', 'w', encoding='utf-8') as f:
        for msg in messages:
            f.write(msg + '\n')

# Команды
async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "Привет! Я бот для утренних сообщений.\n\n"
        "Команды:\n"
        "/time - время\n"
        "/settime <время> - изменить\n"
        "/messages - список\n"
        "/add <текст> - добавить\n"
        "/recipient - получатель\n"
        "/now - отправить сейчас"
    )

async def time_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    await update.message.reply_text(f"Время: {config.get('time', '09:00')}")

async def settime_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("/settime 10:00")
        return
    config = load_config()
    config['time'] = context.args[0]
    save_config(config)
    await update.message.reply_text(f"Время: {config['time']}")

async def messages_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    messages = load_messages()
    if messages:
        await update.message.reply_text("Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(messages)))
    else:
        await update.message.reply_text("Пусто!")

async def add_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("/add Привет!")
        return
    text = " ".join(context.args)
    messages = load_messages()
    messages.append(text)
    save_messages(messages)
    await update.message.reply_text(f"Добавлено: {text}")

async def recipient_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    await update.message.reply_text(f"Получатель: {config.get('recipient', '?')}")

async def now_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    messages = load_messages()
    if not messages:
        await update.message.reply_text("Список пуст!")
        return
    msg = random.choice(messages)
    try:
        chat = await context.bot.get_chat(config.get('recipient'))
        await context.bot.send_message(chat_id=chat.id, text=msg)
        messages.remove(msg)
        save_messages(messages)
        await update.message.reply_text(f"Отправлено: {msg}")
    except Exception as e:
        await update.message.reply_text(f"Ошибка: {e}")

async def scheduled_send():
    """Проверка времени"""
    last_date = None
    while True:
        try:
            config = load_config()
            now = datetime.now()
            cur_time = now.strftime("%H:%M")
            cur_date = now.strftime("%Y-%m-%d")
            target = config.get('time', '09:00')
            
            if cur_time == target and last_date != cur_date:
                messages = load_messages()
                if messages:
                    msg = random.choice(messages)
                    try:
                        chat = await bot.get_chat(config.get('recipient'))
                        await bot.send_message(chat_id=chat.id, text=msg)
                        messages.remove(msg)
                        save_messages(messages)
                        logger.info(f"Отправлено: {msg}")
                    except Exception as e:
                        logger.error(f"Ошибка: {e}")
                last_date = cur_date
        except Exception as e:
            logger.error(f"Ошибка: {e}")
        await asyncio.sleep(30)

async def main():
    global bot, app
    
    config = load_config()
    token = config.get('token', '')
    
    if not token:
        logger.error("Нет токена!")
        return
    
    app = Application.builder().token(token).build()
    
    app.add_handler(CommandHandler("start", start_command))
    app.add_handler(CommandHandler("time", time_command))
    app.add_handler(CommandHandler("settime", settime_command))
    app.add_handler(CommandHandler("messages", messages_command))
    app.add_handler(CommandHandler("add", add_command))
    app.add_handler(CommandHandler("recipient", recipient_command))
    app.add_handler(CommandHandler("now", now_command))
    
    bot = app.bot
    
    # Запускаем планировщик
    asyncio.create_task(scheduled_send())
    
    logger.info("Бот запущен!")
    
    # Webhook для Render
    port = int(os.environ.get('PORT', 10000))
    
    await app.run_webhook(
        listen='0.0.0.0',
        port=port,
        url_path='',
        webhook_url=None
    )

if __name__ == '__main__':
    if flask_app:
        # Запускаем flask в отдельном потоке
        import threading
        def run_flask():
            port = int(os.environ.get('PORT', 10000))
            flask_app.run(host='0.0.0.0', port=port)
        threading.Thread(target=run_flask, daemon=True).start()
    
    asyncio.run(main())
