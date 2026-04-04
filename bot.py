import json
import random
import os
import logging
from datetime import datetime

logging.basicConfig(
    format='%(asctime)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

from flask import Flask, request, Response
from telegram import Bot, Update
from telegram.error import TelegramError

app = Flask(__name__)

# Загружаем настройки
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

# Telegram webhook
@app.route('/webhook', methods=['POST'])
def webhook():
    config = load_config()
    bot = Bot(token=config['token'])
    
    update = Update.de_json(request.get_json(), bot)
    
    if update.message and update.message.text:
        text = update.message.text
        chat_id = update.message.chat.id
        
        if text == '/start':
            bot.send_message(
                chat_id=chat_id,
                text="Привет! Команды:\n/time - время\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/setrecipient <username>\n/now"
            )
        elif text == '/time':
            config = load_config()
            bot.send_message(chat_id=chat_id, text=f"Время: {config.get('time', '09:00')}")
        elif text.startswith('/settime '):
            new_time = text.split(' ')[1]
            config = load_config()
            config['time'] = new_time
            save_config(config)
            bot.send_message(chat_id=chat_id, text=f"Время: {new_time}")
        elif text == '/messages':
            msgs = load_messages()
            if msgs:
                bot.send_message(chat_id=chat_id, text="Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs)))
            else:
                bot.send_message(chat_id=chat_id, text="Пусто!")
        elif text.startswith('/add '):
            text_msg = text[5:]
            msgs = load_messages()
            msgs.append(text_msg)
            save_messages(msgs)
            bot.send_message(chat_id=chat_id, text=f"Добавлено: {text_msg}")
        elif text == '/recipient':
            config = load_config()
            bot.send_message(chat_id=chat_id, text=f"Получатель: {config.get('recipient')}")
        elif text.startswith('/setrecipient '):
            new_rec = text[14:]
            if not new_rec.startswith('@'):
                new_rec = '@' + new_rec
            config = load_config()
            config['recipient'] = new_rec
            save_config(config)
            bot.send_message(chat_id=chat_id, text=f"Получатель: {new_rec}")
        elif text == '/now':
            config = load_config()
            msgs = load_messages()
            if not msgs:
                bot.send_message(chat_id=chat_id, text="Список пуст!")
                return 'OK'
            msg = random.choice(msgs)
            try:
                bot.send_message(chat_id=config['recipient'], text=msg)
                msgs.remove(msg)
                save_messages(msgs)
                bot.send_message(chat_id=chat_id, text=f"Отправлено: {msg}")
            except Exception as e:
                bot.send_message(chat_id=chat_id, text=f"Ошибка: {e}")
    
    return 'OK'

# Проверка времени (вызывается при каждом запросе к серверу)
last_sent_date = None

@app.route('/')
def home():
    global last_sent_date
    
    config = load_config()
    now = datetime.now()
    cur_time = now.strftime("%H:%M")
    cur_date = now.strftime("%Y-%m-%d")
    target = config.get('time', '09:00')
    
    # Проверяем время
    if cur_time == target and last_sent_date != cur_date:
        msgs = load_messages()
        if msgs:
            msg = random.choice(msgs)
            try:
                bot = Bot(token=config['token'])
                bot.send_message(chat_id=config['recipient'], text=msg)
                msgs.remove(msg)
                save_messages(msgs)
                logger.info(f"Отправлено: {msg}")
            except Exception as e:
                logger.error(f"Ошибка: {e}")
        last_sent_date = cur_date
    
    return Response('OK', status=200)

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port)
