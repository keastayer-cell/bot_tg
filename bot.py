import json
import random
import os
import logging
from datetime import datetime
from telegram import Bot
from telegram.error import TelegramError
import time

# Настройка логов
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

def load_config():
    """Загрузка конфигурации"""
    with open('config.json', 'r', encoding='utf-8') as f:
        return json.load(f)

def load_messages():
    """Загрузка сообщений из файла"""
    if not os.path.exists('messages.txt'):
        # Создаем файл с примером, если его нет
        with open('messages.txt', 'w', encoding='utf-8') as f:
            f.write("Доброе утро! ☀️\n")
            f.write("С добрым утром!\n")
            f.write("Утро доброе!\n")
        return ["Доброе утро! ☀️", "С добрым утром!", "Утро доброе!"]
    
    with open('messages.txt', 'r', encoding='utf-8') as f:
        messages = [line.strip() for line in f if line.strip()]
    return messages

def save_messages(messages):
    """Сохранение сообщений в файл"""
    with open('messages.txt', 'w', encoding='utf-8') as f:
        for msg in messages:
            f.write(msg + '\n')

def send_message(bot, chat_id, text):
    """Отправка сообщения"""
    try:
        bot.send_message(chat_id=chat_id, text=text)
        logger.info(f"Сообщение отправлено: {text}")
        return True
    except TelegramError as e:
        logger.error(f"Ошибка отправки: {e}")
        return False

def get_user_id_by_username(bot, username):
    """Получение ID пользователя по username"""
    try:
        # Убираем @ если есть
        username = username.lstrip('@')
        # Пробуем найти пользователя через бота
        chat = bot.get_chat(f"@{username}")
        return chat.id
    except TelegramError as e:
        logger.error(f"Не удалось получить ID для {username}: {e}")
        return None

def send_random_message(bot):
    """Отправка случайного сообщения"""
    config = load_config()
    messages = load_messages()
    
    if not messages:
        logger.warning("Список сообщений пуст!")
        return
    
    # Выбираем случайное сообщение
    message = random.choice(messages)
    
    # Получаем ID получателя
    recipient = config.get('recipient', '')
    chat_id = get_user_id_by_username(bot, recipient)
    
    if chat_id is None:
        # Если не удалось получить по username, пробуем как chat_id
        try:
            chat_id = int(recipient)
        except ValueError:
            logger.error("Неверный получатель в config.json")
            return
    
    if send_message(bot, chat_id, message):
        # Удаляем отправленное сообщение из списка
        messages.remove(message)
        save_messages(messages)
        logger.info(f"Сообщение удалено из списка. Осталось: {len(messages)}")

def check_time(config):
    """Проверка текущего времени"""
    now = datetime.now()
    current_time = now.strftime("%H:%M")
    target_time = config.get('time', "09:00")
    
    return current_time == target_time

def main():
    """Главная функция"""
    config = load_config()
    token = config.get('token', '')
    
    if not token:
        logger.error("Токен не найден в config.json!")
        return
    
    bot = Bot(token=token)
    
    logger.info("Бот запущен. Ожидание времени...")
    
    last_sent_date = None
    
    while True:
        try:
            config = load_config()  # Перезагружаем конфиг
            
            now = datetime.now()
            current_date = now.strftime("%Y-%m-%d")
            
            # Проверяем время и дату
            if check_time(config) and last_sent_date != current_date:
                logger.info(f"Время {config.get('time', '09:00')} наступило! Отправляем сообщение...")
                send_random_message(bot)
                last_sent_date = current_date
            
            time.sleep(30)  # Проверяем каждые 30 секунд
            
        except Exception as e:
            logger.error(f"Ошибка: {e}")
            time.sleep(30)

if __name__ == '__main__':
    main()
