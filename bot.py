import json
import random
import os
import logging
from datetime import datetime, timedelta
import requests
from flask import Flask, request, Response

# Логирование в файл и консоль
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s | %(levelname)s | %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    handlers=[
        logging.FileHandler('bot.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
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
    logger.info(f"[ОТПРАВЛЕНО] chat_id={chat_id}: {text[:50]}")
    return result

def get_user_info(chat_id, username):
    """Краткая инфа о пользователе"""
    if username:
        return f"@{username} (id={chat_id})"
    return f"id={chat_id}"

def process_command(text, chat_id, username=''):
    config = load_config()
    admin_id = str(config.get('admin_id', ''))
    recipient_id = str(config.get('recipient', ''))
    
    user = get_user_info(chat_id, username)
    
    logger.info(f"[КОМАНДА] от {user}: {text}")
    
    is_admin = (str(chat_id) == admin_id)
    is_recipient = (str(chat_id) == recipient_id)
    
    if not is_admin and not is_recipient:
        logger.warning(f"[ОТКАЗАНО] Неизвестный пользователь {user}")
        return
    
    # Получатель
    if is_recipient and not is_admin:
        if text == '/start':
            send_message(chat_id, "Спасибо что активировали бота :) Ожидайте сообщений.")
            logger.info(f"[АКТИВАЦИЯ] Получатель {user} активирован")
        else:
            admin = config.get('admin_id', '')
            try:
                send_message(admin, f"📬 Сообщение от {user}:\n{text}")
                send_message(chat_id, "Сообщение отправлено администратору!")
                logger.info(f"[ПЕРЕСЫЛКА] от {user} -> админ")
            except Exception as e:
                send_message(chat_id, "Не удалось отправить. Попробуйте позже.")
                logger.error(f"[ОШИБКА] Пересылка от {user}: {e}")
        return
    
    # Админ
    if text == '/start':
        send_message(chat_id, "Привет, админ!\n\nКоманды:\n/time - время\n/settime <время>\n/messages\n/add <текст>\n/recipient\n/msg <текст>\n/now\n/logs")
    
    elif text == '/logs':
        try:
            with open('bot.log', 'r') as f:
                lines = f.readlines()[-30:]
                logs_text = ''.join(lines)
            send_message(chat_id, f"Последние 30 строк логов:\n\n{logs_text[-3000:]}")
        except Exception as e:
            send_message(chat_id, f"Ошибка чтения логов: {e}")
    
    elif text.startswith('/msg '):
        msg_text = text[5:]
        recipient = config.get('recipient', '')
        try:
            send_message(recipient, msg_text)
            send_message(chat_id, f"Отправлено: {msg_text}")
            logger.info(f"[АДМИН->ПОЛУЧАТЕЛЬ] {user}: {msg_text[:50]}")
        except Exception as e:
            send_message(chat_id, f"Ошибка: {e}")
    
    elif text == '/time':
        t = config.get('time', '09:00')
        send_message(chat_id, f"Время отправки: {t} (МСК)")
    
    elif text.startswith('/settime '):
        new_time = text.split(' ')[1]
        config['time'] = new_time
        save_config(config)
        send_message(chat_id, f"Время изменено на {new_time} (МСК)")
        logger.info(f"[НАСТРОЙКА] {user} установил время {new_time}")
    
    elif text == '/messages':
        msgs = load_messages()
        if msgs:
            count = len(msgs)
            send_message(chat_id, f"Сообщений: {count}\n\n" + "\n".join(f"{i+1}. {m}" for i, m in enumerate(msgs[:10])))
        else:
            send_message(chat_id, "Список пуст!")
    
    elif text.startswith('/add '):
        text_msg = text[5:]
        msgs = load_messages()
        msgs.append(text_msg)
        save_messages(msgs)
        send_message(chat_id, f"Добавлено: {text_msg}")
        logger.info(f"[ДОБАВЛЕНО] {user}: {text_msg[:50]}")
    
    elif text == '/recipient':
        rec = config.get('recipient', 'не указан')
        send_message(chat_id, f"Получатель: {rec}")
    
    elif text.startswith('/setrecipient '):
        new_rec = text[14:]
        config['recipient'] = new_rec
        save_config(config)
        send_message(chat_id, f"Получатель: {new_rec}")
        logger.info(f"[НАСТРОЙКА] {user} изменил получателя на {new_rec}")
    
    elif text == '/now':
        msgs = load_messages()
        recipient = config.get('recipient')
        
        if msgs:
            msg = random.choice(msgs)
            try:
                send_message(recipient, msg)
                msgs.remove(msg)
                save_messages(msgs)
                send_message(chat_id, f"Отправлено: {msg}")
                logger.info(f"[РУЧНАЯ ОТПРАВКА] {user}: {msg[:50]}")
            except Exception as e:
                send_message(chat_id, f"Ошибка: {e}")
        else:
            send_message(chat_id, "Список пуст!")

def get_last_sent():
    config = load_config()
    return config.get('last_sent', '')

def set_last_sent(date):
    config = load_config()
    config['last_sent'] = date
    save_config(config)

def check_and_send():
    try:
        config = load_config()
        now = datetime.now() + timedelta(hours=3)
        cur_time = now.strftime("%H:%M")
        cur_date = now.strftime("%Y-%m-%d")
        target = config.get('time', '09:00')
        recipient = config.get('recipient')
        last_sent = get_last_sent()
        
        if cur_time == target and last_sent != cur_date:
            msgs = load_messages()
            if msgs:
                msg = random.choice(msgs)
                try:
                    send_message(recipient, msg)
                    msgs.remove(msg)
                    save_messages(msgs)
                    set_last_sent(cur_date)
                    logger.info(f"[АВТООТПРАВКА] В {cur_time} отправлено: {msg[:50]}")
                except Exception as e:
                    logger.error(f"[ОШИБКА АВТООТПРАВКИ] {e}")
    except Exception as e:
        logger.error(f"[ОШИБКА] {e}")

@app.route('/webhook', methods=['POST'])
def webhook():
    check_and_send()
    try:
        data = request.get_json()
        if data and 'message' in data:
            text = data['message'].get('text', '')
            chat_id = data['message']['chat']['id']
            username = data['message']['chat'].get('username', '')
            
            if text.startswith('/'):
                process_command(text, chat_id, username)
            else:
                process_command(text, chat_id, username)
    except Exception as e:
        logger.error(f"[WEBHOOK] {e}")
    return 'OK'

@app.route('/')
def home():
    check_and_send()
    return Response('OK', status=200)

if __name__ == '__main__':
    config = load_config()
    token = config['token']
    
    logger.info("=" * 50)
    logger.info("БОТ ЗАПУЩЕН")
    logger.info(f"Получатель: {config.get('recipient')}")
    logger.info(f"Время отправки: {config.get('time', '09:00')}")
    logger.info("=" * 50)
    
    railway_url = os.environ.get('RAILWAY_PUBLIC_DOMAIN')
    if railway_url:
        try:
            requests.post(f"https://api.telegram.org/bot{token}/setWebhook", 
                         json={'url': f"https://{railway_url}/webhook"})
        except:
            pass
    
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port)
