import json
import random
import os
import logging
from datetime import datetime
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

from telegram import Bot
from telegram.ext import Updater, CommandHandler, MessageHandler, filters

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

def start(update, context):
    update.message.reply_text("Привет! Команды:\n/time\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/setrecipient\n/now")

def time_cmd(update, context):
    config = load_config()
    update.message.reply_text(f"Время: {config.get('time', '09:00')}")

def settime(update, context):
    if context.args:
        config = load_config()
        config['time'] = context.args[0]
        save_config(config)
        update.message.reply_text(f"Время: {config['time']}")

def messages_cmd(update, context):
    msgs = load_messages()
    update.message.reply_text("Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs)) if msgs else "Пусто!")

def add_msg(update, context):
    if context.args:
        text = " ".join(context.args)
        msgs = load_messages()
        msgs.append(text)
        save_messages(msgs)
        update.message.reply_text(f"Добавлено: {text}")

def recipient_cmd(update, context):
    config = load_config()
    update.message.reply_text(f"Получатель: {config.get('recipient')}")

def setrecipient(update, context):
    if context.args:
        new_rec = context.args[0]
        if not new_rec.startswith('@'):
            new_rec = '@' + new_rec
        config = load_config()
        config['recipient'] = new_rec
        save_config(config)
        update.message.reply_text(f"Получатель: {new_rec}")

def now_cmd(update, context):
    config = load_config()
    msgs = load_messages()
    if msgs:
        msg = random.choice(msgs)
        try:
            context.bot.send_message(chat_id=config['recipient'], text=msg)
            msgs.remove(msg)
            save_messages(msgs)
            update.message.reply_text(f"Отправлено: {msg}")
        except Exception as e:
            update.message.reply_text(f"Ошибка: {e}")
    else:
        update.message.reply_text("Список пуст!")

# Планировщик
last_sent_date = None

def scheduled_send(bot):
    global last_sent_date
    
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
        
        time.sleep(30)

if __name__ == '__main__':
    config = load_config()
    
    updater = Updater(token=config['token'])
    dp = updater.dispatcher
    
    dp.add_handler(CommandHandler("start", start))
    dp.add_handler(CommandHandler("time", time_cmd))
    dp.add_handler(CommandHandler("settime", settime))
    dp.add_handler(CommandHandler("messages", messages_cmd))
    dp.add_handler(CommandHandler("add", add_msg))
    dp.add_handler(CommandHandler("recipient", recipient_cmd))
    dp.add_handler(CommandHandler("setrecipient", setrecipient))
    dp.add_handler(CommandHandler("now", now_cmd))
    
    # Запускаем планировщик
    import threading
    threading.Thread(target=scheduled_send, args=(updater.bot,), daemon=True).start()
    
    logger.info("Бот запущен!")
    updater.start_polling(drop_pending_updates=True)
    updater.idle()
