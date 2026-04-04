import json
import random
import os
import logging
from datetime import datetime
from threading import Thread
import asyncio

# Настройка логов
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

# Импорт telegram
try:
    from telegram import Update
    from telegram.ext import Application, CommandHandler, ContextTypes
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

# Команды бота
async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "Привет! Я бот для отправки утренних сообщений.\n\n"
        "Команды:\n"
        "/time - показать время отправки\n"
        "/settime <время> - изменить время (например /settime 10:00)\n"
        "/messages - показать список сообщений\n"
        "/add <текст> - добавить сообщение\n"
        "/recipient - показать получателя\n"
        "/setrecipient <username> - изменить получателя\n"
        "/now - отправить сообщение сейчас"
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
        text = "Список сообщений пуст!"
    await update.message.reply_text(text)

async def add_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("Использование: /add <текст>")
        return
    
    text = " ".join(context.args)
    messages = load_messages()
    messages.append(text)
    save_messages(messages)
    await update.message.reply_text(f"Сообщение добавлено: {text}")

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
    await update.message.reply_text(f"Получатель изменен на {new_recipient}")

async def now_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    config = load_config()
    messages = load_messages()
    
    if not messages:
        await update.message.reply_text("Список сообщений пуст!")
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

async def run_scheduler(application):
    last_sent_date = None
    
    while True:
        try:
            config = load_config()
            now = datetime.now()
            current_date = now.strftime("%Y-%m-%d")
            current_time = now.strftime("%H:%M")
            target_time = config.get('time', "09:00")
            
            if current_time == target_time and last_sent_date != current_date:
                logger.info(f"Время {target_time} наступило!")
                
                messages = load_messages()
                if messages:
                    message = random.choice(messages)
                    recipient = config.get('recipient', '')
                    
                    try:
                        chat = await application.bot.get_chat(recipient)
                        await application.bot.send_message(chat_id=chat.id, text=message)
                        messages.remove(message)
                        save_messages(messages)
                        logger.info(f"Сообщение отправлено: {message}")
                    except Exception as e:
                        logger.error(f"Ошибка отправки: {e}")
                
                last_sent_date = current_date
            
            await asyncio.sleep(30)
        except Exception as e:
            logger.error(f"Ошибка: {e}")
            await asyncio.sleep(30)

def main():
    config = load_config()
    token = config.get('token', '')
    
    if not token:
        logger.error("Токен не найден!")
        return
    
    application = Application.builder().token(token).build()
    
    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(CommandHandler("time", time_command))
    application.add_handler(CommandHandler("settime", settime_command))
    application.add_handler(CommandHandler("messages", messages_command))
    application.add_handler(CommandHandler("add", add_command))
    application.add_handler(CommandHandler("recipient", recipient_command))
    application.add_handler(CommandHandler("setrecipient", setrecipient_command))
    application.add_handler(CommandHandler("now", now_command))
    
    logger.info("Бот запущен!")
    
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    
    def run_async():
        loop.run_until_complete(application.run_polling())
    
    def run_scheduler_async():
        loop.run_until_complete(run_scheduler(application))
    
    Thread(target=run_async, daemon=True).start()
    Thread(target=run_scheduler_async, daemon=True).start()
    
    if flask_app:
        port = int(os.environ.get('PORT', 10000))
        logger.info(f"Веб-сервер на порту {port}")
        flask_app.run(host='0.0.0.0', port=port)
    else:
        while True:
            import time
            time.sleep(60)

if __name__ == '__main__':
    main()
