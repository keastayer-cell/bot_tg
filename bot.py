import json
import random
import os
import logging
from datetime import datetime, timedelta
import time
import requests
from flask import Flask, request, Response

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

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

def send_message(chat_id, text):
    config = load_config()
    url = f"https://api.telegram.org/bot{config['token']}/sendMessage"
    requests.post(url, json={'chat_id': chat_id, 'text': text})

def process_command(text, chat_id):
    if text == '/start':
        send_message(chat_id, "Привет! Команды:\n/time\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/setrecipient\n/now")
    elif text == '/time':
        config = load_config()
        send_message(chat_id, f"Время: {config.get('time', '09:00')}")
    elif text.startswith('/settime '):
        new_time = text.split(' ')[1]
        config = load_config()
        config['time'] = new_time
        save_config(config)
        send_message(chat_id, f"Время: {new_time}")
    elif text == '/messages':
        msgs = load_messages()
        if msgs:
            send_message(chat_id, "Список:\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs)))
        else:
            send_message(chat_id, "Пусто!")
    elif text.startswith('/add '):
        text_msg = text[5:]
        msgs = load_messages()
        msgs.append(text_msg)
        save_messages(msgs)
        send_message(chat_id, f"Добавлено: {text_msg}")
    elif text == '/recipient':
        config = load_config()
        send_message(chat_id, f"Получатель: {config.get('recipient')}")
    elif text.startswith('/setrecipient '):
        new_rec = text[14:]
        if not new_rec.startswith('@'):
            new_rec = '@' + new_rec
        config = load_config()
        config['recipient'] = new_rec
        save_config(config)
        send_message(chat_id, f"Получатель: {new_rec}")
    elif text == '/now':
        config = load_config()
        msgs = load_messages()
        if msgs:
            msg = random.choice(msgs)
            try:
                send_message(config['recipient'], msg)
                msgs.remove(msg)
                save_messages(msgs)
                send_message(chat_id, f"Отправлено: {msg}")
            except Exception as e:
                send_message(chat_id, f"Ошибка: {e}")
        else:
            send_message(chat_id, "Список пуст!")

# Webhook endpoint
@app.route('/webhook', methods=['POST'])
def webhook():
    try:
        data = request.get_json()
        if data and 'message' in data:
            text = data['message'].get('text', '')
            chat_id = data['message']['chat']['id']
            if text.startswith('/'):
                process_command(text, chat_id)
    except Exception as e:
        logger.error(f"Ошибка: {e}")
    return 'OK'

# Проверка времени (Москва UTC+3)
last_sent_date = None

@app.route('/')
def home():
    global last_sent_date
    
    try:
        config = load_config()
        now = datetime.now() + timedelta(hours=3)  # Москва UTC+3
        cur_time = now.strftime("%H:%M")
        cur_date = now.strftime("%Y-%m-%d")
        target = config.get('time', '09:00')
        
        if cur_time == target and last_sent_date != cur_date:
            msgs = load_messages()
            if msgs:
                msg = random.choice(msgs)
                try:
                    send_message(config['recipient'], msg)
                    msgs.remove(msg)
                    save_messages(msgs)
                    logger.info(f"Отправлено: {msg}")
                except Exception as e:
                    logger.error(f"Ошибка: {e}")
            last_sent_date = cur_date
    except Exception as e:
        logger.error(f"Ошибка: {e}")
    
    return Response('OK', status=200)

if __name__ == '__main__':
    # Устанавливаем webhook при запуске
    config = load_config()
    token = config['token']
    
    # Получаем URL Railway
    railway_url = os.environ.get('RAILWAY_PUBLIC_DOMAIN')
    if railway_url:
        webhook_url = f"https://{railway_url}/webhook"
        try:
            requests.post(f"https://api.telegram.org/bot{token}/setWebhook", json={'url': webhook_url})
            logger.info(f"Webhook установлен: {webhook_url}")
        except Exception as e:
            logger.error(f"Ошибка webhook: {e}")
    
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port)
