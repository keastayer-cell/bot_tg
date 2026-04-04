import json
import random
import os
import logging
from datetime import datetime
from threading import Thread

logging.basicConfig(
    format='%(asctime)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

from flask import Flask, request, Response
from telegram import Bot, Update
from telegram.error import TelegramError
from telegram.ext import Application, CommandHandler, MessageHandler, filters

app = Flask(__name__)

def load_config():
    with open('config.json', 'r', encoding='utf-8') as f:
        return json.load(f)

def save_config(config):
    with open('config.json', 'w', encoding='utf-8') as f:
        json.dump(config, f, ensure_ascii=False, indent=4)

def load_messages():
    if not os.path.exists('messages.txt'):
        with open('messages.txt', 'w', encoding='utf-8') as f:
            f.write("Доброе утро! ☀️\n")
        return ["Доброе утро! ☀️"]
    with open('messages.txt', 'r', encoding='utf-8') as f:
        return [line.strip() for line in f if line.strip()]

def save_messages(messages):
    with open('messages.txt', 'w', encoding='utf-8') as f:
        for msg in messages:
            f.write(msg + '\n')

# Команды
async def start(update, context):
    await update.message.reply_text(
        "Привет! Команды:\n"
        "/time - время\n"
        "/settime <время> - изменить\n"
        "/messages - список\n"
        "/add <текст> - добавить\n"
        "/recipient - получатель\n"
        "/setrecipient <username> - изменить\n"
        "/now - отправить сейчас"
    )

async def time_cmd(update, context):
    config = load_config()
    await update.message.reply_text(f"Время: {config.get('time', '09:00')}")

async def settime(update, context):
    if not context.args:
        await update.message.reply_text("/settime 10:00")
        return
    config = load_config()
    config['time'] = context.args[0]
    save_config(config)
    await update.message.reply_text(f"Время: {config['time']}")

async def messages_cmd(update, context):
    msgs = load_messages()
    if msgs:
        await update.message.reply_text("Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs)))
    else:
        await update.message.reply_text("Пусто!")

async def add_msg(update, context):
    if not context.args:
        await update.message.reply_text("/add Привет!")
        return
    text = " ".join(context.args)
    msgs = load_messages()
    msgs.append(text)
    save_messages(msgs)
    await update.message.reply_text(f"Добавлено: {text}")

async def recipient_cmd(update, context):
    config = load_config()
    await update.message.reply_text(f"Получатель: {config.get('recipient')}")

async def setrecipient(update, context):
    if not context.args:
        await update.message.reply_text("/setrecipient @username")
        return
    new_rec = context.args[0]
    if not new_rec.startswith('@'):
        new_rec = '@' + new_rec
    config = load_config()
    config['recipient'] = new_rec
    save_config(config)
    await update.message.reply_text(f"Получатель: {new_rec}")

async def now_cmd(update, context):
    config = load_config()
    msgs = load_messages()
    if not msgs:
        await update.message.reply_text("Список пуст!")
        return
    msg = random.choice(msgs)
    try:
        await context.bot.send_message(chat_id=config['recipient'], text=msg)
        msgs.remove(msg)
        save_messages(msgs)
        await update.message.reply_text(f"Отправлено: {msg}")
    except Exception as e:
        await update.message.reply_text(f"Ошибка: {e}")

# Фоновая отправка
last_sent_date = None

def run_bot():
    global last_sent_date
    
    config = load_config()
    token = config.get('token', '')
    
    if not token:
        logger.error("Нет токена!")
        return
    
    application = Application.builder().token(token).build()
    
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("time", time_cmd))
    application.add_handler(CommandHandler("settime", settime))
    application.add_handler(CommandHandler("messages", messages_cmd))
    application.add_handler(CommandHandler("add", add_msg))
    application.add_handler(CommandHandler("recipient", recipient_cmd))
    application.add_handler(CommandHandler("setrecipient", setrecipient))
    application.add_handler(CommandHandler("now", now_cmd))
    
    logger.info("Бот запущен!")
    application.run_polling(drop_pending_updates=True)

def scheduled_send():
    global last_sent_date
    config = load_config()
    bot = Bot(token=config['token'])
    
    while True:
        try:
            config = load_config()
            now = datetime.now()
            cur_time = now.strftime("%H:%M")
            cur_date = now.strftime("%Y-%m-%d")
            target = config.get('time', '09:00')
            
            if cur_time == target and last_sent_date != cur_date:
                msgs = load_messages()
                if msgs:
                    msg = random.choice(msgs)
                    try:
                        bot.send_message(chat_id=config['recipient'], text=msg)
                        msgs.remove(msg)
                        save_messages(msgs)
                        logger.info(f"Отправлено: {msg}")
                    except Exception as e:
                        logger.error(f"Ошибка: {e}")
                last_sent_date = cur_date
        except Exception as e:
            logger.error(f"Ошибка: {e}")
        
        import time
        time.sleep(30)

# Flask routes
@app.route('/webhook', methods=['POST'])
def webhook():
    return 'OK'

@app.route('/')
def home():
    return Response('OK', status=200)

if __name__ == '__main__':
    # Запускаем бота в отдельном потоке
    Thread(target=run_bot, daemon=True).start()
    Thread(target=scheduled_send, daemon=True).start()
    
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port)
