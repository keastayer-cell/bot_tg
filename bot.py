import json
import random
import os
import logging
from datetime import datetime, time as dt_time
import time

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
        return Response('Bot is running', status=200)
except ImportError:
    flask_app = None
    logger.warning("Flask не установлен")

try:
    from telegram import Update
    from telegram.ext import Application, CommandHandler, ContextTypes, JobQueue, Job
except ImportError:
    logger.error("python-telegram-bot не установлен!")
    exit(1)

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
        return ["Доброе утро! ☀️", "С добрым утром!"]
    with open('messages.txt', 'r', encoding='utf-8') as f:
        return [line.strip() for line in f if line.strip()]

def save_messages(messages):
    with open('messages.txt', 'w', encoding='utf-8') as f:
        for msg in messages:
            f.write(msg + '\n')

# Команды
async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "Привет! Я бот для отправки утренних сообщений.\n\n"
        "Команды:\n"
        "/time - время отправки\n"
        "/settime <время> - изменить время\n"
        "/messages - список сообщений\n"
        "/add <текст> - добавить сообщение\n"
        "/recipient - получатель\n"
        "/setrecipient <username> - изменить\n"
        "/now - отправить сейчас"
    )

async def time_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    await update.message.reply_text(f"Время отправки: {config.get('time', '09:00')}")

async def settime_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("Использование: /settime <время> (например 10:00)")
        return
    new_time = context.args[0]
    config = load_config()
    config['time'] = new_time
    save_config(config)
    await update.message.reply_text(f"Время изменено на {new_time}")

async def messages_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    messages = load_messages()
    if messages:
        text = "Список сообщений:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(messages))
    else:
        text = "Список пуст!"
    await update.message.reply_text(text)

async def add_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("Использование: /add <текст>")
        return
    text = " ".join(context.args)
    messages = load_messages()
    messages.append(text)
    save_messages(messages)
    await update.message.reply_text(f"Добавлено: {text}")

async def recipient_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    await update.message.reply_text(f"Получатель: {config.get('recipient', 'не указан')}")

async def setrecipient_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("Использование: /setrecipient <username>")
        return
    new_recipient = context.args[0]
    if not new_recipient.startswith('@'):
        new_recipient = '@' + new_recipient
    config = load_config()
    config['recipient'] = new_recipient
    save_config(config)
    await update.message.reply_text(f"Получатель: {new_recipient}")

async def now_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    messages = load_messages()
    if not messages:
        await update.message.reply_text("Список пуст!")
        return
    message = random.choice(messages)
    recipient = config.get('recipient', '')
    try:
        chat = await context.bot.get_chat(recipient)
        await context.bot.send_message(chat_id=chat.id, text=message)
        messages.remove(message)
        save_messages(messages)
        await update.message.reply_text(f"Отправлено: {message}")
    except Exception as e:
        await update.message.reply_text(f"Ошибка: {e}")

async def scheduled_job(context: ContextTypes.DEFAULT_TYPE):
    """Планировщик - отправка по времени"""
    config = load_config()
    now = datetime.now()
    current_time = now.strftime("%H:%M")
    target_time = config.get('time', "09:00")
    
    if current_time == target_time:
        messages = load_messages()
        if messages:
            message = random.choice(messages)
            recipient = config.get('recipient', '')
            try:
                chat = await context.bot.get_chat(recipient)
                await context.bot.send_message(chat_id=chat.id, text=message)
                messages.remove(message)
                save_messages(messages)
                logger.info(f"Отправлено: {message}")
            except Exception as e:
                logger.error(f"Ошибка: {e}")

def main():
    config = load_config()
    token = config.get('token', '')
    
    if not token:
        logger.error("Токен не найден!")
        return
    
    application = Application.builder().token(token).build()
    
    # Команды
    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(CommandHandler("time", time_command))
    application.add_handler(CommandHandler("settime", settime_command))
    application.add_handler(CommandHandler("messages", messages_command))
    application.add_handler(CommandHandler("add", add_command))
    application.add_handler(CommandHandler("recipient", recipient_command))
    application.add_handler(CommandHandler("setrecipient", setrecipient_command))
    application.add_handler(CommandHandler("now", now_command))
    
    # JobQueue - проверка каждую минуту
    job_queue = application.job_queue
    job_queue.run_repeating(scheduled_job, interval=60, first=10)
    
    logger.info("Бот запущен!")
    
    # Запуск
    if flask_app:
        port = int(os.environ.get('PORT', 10000))
        
        # Запускаем бота в отдельном потоке
        import threading
        
        def run_bot():
            application.run_webhook(
                listen='0.0.0.0',
                port=port,
                url_path='',
                webhook_url=f"https://morning-bot-cq6d.onrender.com"
            )
        
        threading.Thread(target=run_bot, daemon=True).start()
        logger.info(f"Бот и веб на порту {port}")
        
        flask_app.run(host='0.0.0.0', port=port)
    else:
        application.run_polling()

if __name__ == '__main__':
    main()
