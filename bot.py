import json
import random
import os
import logging
from datetime import datetime, timedelta
import time
import requests
from flask import Flask, request, Response

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s | %(levelname)s | %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
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
    result = requests.post(url, json={'chat_id': chat_id, 'text': text})
    logger.info(f"-> Отправлено {chat_id}: {text[:50]}...")
    return result

def process_command(text, chat_id, username=''):
    now = datetime.now() + timedelta(hours=3)
    time_str = now.strftime('%Y-%m-%d %H:%M:%S')
    
    user_info = f"@{username}" if username else f"id:{chat_id}"
    logger.info(f"<- Команда от {user_info}: {text}")
    
    if text == '/start':
        send_message(chat_id, "Привет! Команды:\n/time\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/setrecipient\n/now")
        logger.info(f"-> {user_info}: Отправлено приветствие")
    
    elif text == '/time':
        config = load_config()
        t = config.get('time', '09:00')
        send_message(chat_id, f"Время отправки: {t} (МСК)")
        logger.info(f"-> {user_info}: Показано время {t}")
    
    elif text.startswith('/settime '):
        new_time = text.split(' ')[1]
        config = load_config()
        config['time'] = new_time
        save_config(config)
        send_message(chat_id, f"Время изменено на {new_time} (МСК)")
        logger.info(f"-> {user_info}: Установлено время {new_time}")
    
    elif text == '/messages':
        msgs = load_messages()
        if msgs:
            count = len(msgs)
            send_message(chat_id, f"Сообщений в очереди: {count}\n\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs[:10])))
            logger.info(f"-> {user_info}: Показан список ({count} шт)")
        else:
            send_message(chat_id, "Список пуст!")
            logger.info(f"-> {user_info}: Список пуст")
    
    elif text.startswith('/add '):
        text_msg = text[5:]
        msgs = load_messages()
        msgs.append(text_msg)
        save_messages(msgs)
        send_message(chat_id, f"Добавлено: {text_msg}")
        logger.info(f"-> {user_info}: Добавлено сообщение '{text_msg[:30]}...' Всего: {len(msgs)}")
    
    elif text == '/recipient':
        config = load_config()
        rec = config.get('recipient', 'не указан')
        send_message(chat_id, f"Получатель: {rec}")
        logger.info(f"-> {user_info}: Показан получатель {rec}")
    
    elif text.startswith('/setrecipient '):
        new_rec = text[14:]
        if not new_rec.startswith('@'):
            new_rec = '@' + new_rec
        config = load_config()
        config['recipient'] = new_rec
        save_config(config)
        send_message(chat_id, f"Получатель изменен на {new_rec}")
        logger.info(f"-> {user_info}: Установлен получатель {new_rec}")
    
    elif text == '/now':
        config = load_config()
        msgs = load_messages()
        recipient = config.get('recipient')
        
        if msgs:
            msg = random.choice(msgs)
            try:
                send_message(recipient, msg)
                msgs.remove(msg)
                save_messages(msgs)
                send_message(chat_id, f"Отправлено получателю: {msg}")
                logger.info(f"-> {recipient}: Отправлено '{msg[:30]}...' Осталось: {len(msgs)}")
            except Exception as e:
                send_message(chat_id, f"Ошибка: {e}")
                logger.error(f"Ошибка отправки {recipient}: {e}")
        else:
            send_message(chat_id, "Список пуст!")
            logger.warning(f"-> {user_info}: Список сообщений пуст!")

# Глобальная переменная для отправки
last_sent_date = None

def check_and_send():
    """Проверка времени и отправка"""
    global last_sent_date
    try:
        config = load_config()
        now = datetime.now() + timedelta(hours=3)
        cur_time = now.strftime("%H:%M")
        cur_date = now.strftime("%Y-%m-%d")
        target = config.get('time', '09:00')
        recipient = config.get('recipient')

        if cur_time == target and last_sent_date != cur_date:
            msgs = load_messages()
            if msgs:
                msg = random.choice(msgs)
                try:
                    send_message(recipient, msg)
                    msgs.remove(msg)
                    save_messages(msgs)
                    logger.info(f"=== АВТООТПРАВКА === В {cur_time} отправлено '{msg[:30]}...' получателю {recipient}. Осталось: {len(msgs)}")
                except Exception as e:
                    logger.error(f"=== АВТООТПРАВКА === Ошибка: {e}")
            else:
                logger.warning(f"=== АВТООТПРАВКА === Список пуст")
            last_sent_date = cur_date
    except Exception as e:
        logger.error(f"Ошибка check_and_send: {e}")

# Webhook endpoint
@app.route('/webhook', methods=['POST'])
def webhook():
    check_and_send()  # Проверяем время при каждом запросе
    try:
        data = request.get_json()
        if data and 'message' in data:
            text = data['message'].get('text', '')
            chat_id = data['message']['chat']['id']
            username = data['message']['chat'].get('username', '')
            
            if text.startswith('/'):
                process_command(text, chat_id, username)
    except Exception as e:
        logger.error(f"Ошибка webhook: {e}")
    return 'OK'

# Проверка времени (Москва UTC+3)
last_sent_date = None

@app.route('/')
def home():
    global last_sent_date
    
    try:
        config = load_config()
        now = datetime.now() + timedelta(hours=3)
        cur_time = now.strftime("%H:%M")
        cur_date = now.strftime("%Y-%m-%d")
        target = config.get('time', '09:00')
        recipient = config.get('recipient')
        
        if cur_time == target and last_sent_date != cur_date:
            msgs = load_messages()
            if msgs:
                msg = random.choice(msgs)
                try:
                    send_message(recipient, msg)
                    msgs.remove(msg)
                    save_messages(msgs)
                    logger.info(f"=== АВТООТПРАВКА === В {cur_time} отправлено '{msg[:30]}...' получателю {recipient}. Осталось: {len(msgs)}")
                except Exception as e:
                    logger.error(f"=== АВТООТПРАВКА === Ошибка: {e}")
            else:
                logger.warning(f"=== АВТООТПРАВКА === Список пуст, отправка пропущена")
            last_sent_date = cur_date
    except Exception as e:
        logger.error(f"Ошибка: {e}")
    
    return Response('OK', status=200)

if __name__ == '__main__':
    config = load_config()
    token = config['token']
    
    logger.info("=" * 50)
    logger.info("БОТ ЗАПУЩЕН")
    logger.info(f"Время: {datetime.now() + timedelta(hours=3)} (МСК)")
    logger.info(f"Получатель: {config.get('recipient')}")
    logger.info(f"Время отправки: {config.get('time', '09:00')}")
    logger.info("=" * 50)
    
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
