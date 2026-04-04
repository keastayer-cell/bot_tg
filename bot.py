import json
import random
import os
import logging
from datetime import datetime
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

from telegram import Bot, Update
from telegram.ext import Application, CommandHandler

def load_config():
    with open('config.json', 'r') as f:
        return json.load(f)

def save_config(config):
    with open('config.json', 'w') as f:
        json.dump(config, f, ensure_ascii=False, indent=4)

def load_messages():
    with open('messages.txt', 'r') as f:
        return [l.strip() for l in f if l.strip()]

def save_messages(messages):
    with open('messages.txt', 'w') as f:
        for m in messages:
            f.write(m + '\n')

async def start(update, context):
    await update.message.reply_text("Привет! Команды:\n/time\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/setrecipient\n/now")

async def time_cmd(update, context):
    config = load_config()
    await update.message.reply_text(f"Время: {config.get('time', '09:00')}")

async def settime(update, context):
    if context.args:
        config = load_config()
        config['time'] = context.args[0]
        save_config(config)
        await update.message.reply_text(f"Время: {config['time']}")

async def messages_cmd(update, context):
    msgs = load_messages()
    await update.message.reply_text("Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs)) if msgs else "Пусто!")

async def add_msg(update, context):
    if context.args:
        text = " ".join(context.args)
        msgs = load_messages()
        msgs.append(text)
        save_messages(msgs)
        await update.message.reply_text(f"Добавлено: {text}")

async def recipient_cmd(update, context):
    config = load_config()
    await update.message.reply_text(f"Получатель: {config.get('recipient')}")

async def setrecipient(update, context):
    if context.args:
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
    if msgs:
        msg = random.choice(msgs)
        try:
            await context.bot.send_message(chat_id=config['recipient'], text=msg)
            msgs.remove(msg)
            save_messages(msgs)
            await update.message.reply_text(f"Отправлено: {msg}")
        except Exception as e:
            await update.message.reply_text(f"Ошибка: {e}")
    else:
        await update.message.reply_text("Список пуст!")

# Главная функция
async def main():
    config = load_config()
    app = Application.builder().token(config['token']).build()
    
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("time", time_cmd))
    app.add_handler(CommandHandler("settime", settime))
    app.add_handler(CommandHandler("messages", messages_cmd))
    app.add_handler(CommandHandler("add", add_msg))
    app.add_handler(CommandHandler("recipient", recipient_cmd))
    app.add_handler(CommandHandler("setrecipient", setrecipient))
    app.add_handler(CommandHandler("now", now_cmd))
    
    logger.info("Бот запущен!")
    
    # Используем webhook
    await app.run_webhook(
        listen='0.0.0.0',
        port=int(os.environ.get('PORT', 8080)),
        url_path='webhook',
        webhook_url=None  # Без webhook URL
    )

if __name__ == '__main__':
    import asyncio
    asyncio.run(main())
