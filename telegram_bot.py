import base64
import binascii
import json
import tempfile
from pathlib import Path

from aiogram import Bot, Dispatcher, types
from aiogram.types import KeyboardButton, ReplyKeyboardMarkup, WebAppInfo
from aiogram.utils import executor

# ВРЕМЕННО: токен захардкожен для локального запуска.
# Перед публикацией обязательно перенесите токен в переменные окружения.
TOKEN = "8224277435:AAESw0IeGllW1RfvHaiVjmqQiORJP3Ixh_o"
WEBAPP_URL = "https://taka-howk.github.io/rolly-webapp/"

bot = Bot(token=TOKEN)
dp = Dispatcher(bot)


@dp.message_handler(commands=["start"])
async def start(message: types.Message) -> None:
    keyboard = ReplyKeyboardMarkup(resize_keyboard=True)
    button = KeyboardButton(text="Открыть заказ", web_app=WebAppInfo(url=WEBAPP_URL))
    keyboard.add(button)
    await message.answer("Нажмите кнопку ниже:", reply_markup=keyboard)


@dp.message_handler(content_types=types.ContentType.WEB_APP_DATA)
async def handle_webapp(message: types.Message) -> None:
    raw_data = (message.web_app_data.data or "").strip()

    # Новый формат: JSON-данные заказа из Telegram WebApp.sendData
    if raw_data.startswith("{"):
        try:
            payload = json.loads(raw_data)
        except json.JSONDecodeError:
            await message.answer("Ошибка: не удалось прочитать данные заказа.")
            return

        rows = payload.get("rows") or []
        order_number = payload.get("orderNumber") or "-"
        customer = payload.get("customer") or "-"
        date = payload.get("date") or "-"

        text = (
            "✅ Заказ получен из WebApp\n"
            f"Дата: {date}\n"
            f"Номер: {order_number}\n"
            f"Заказчик: {customer}\n"
            f"Позиций: {len(rows)}"
        )
        await message.answer(text)
        return

    # Старый формат: base64 JPEG (оставлен для совместимости)
    if not raw_data.startswith("data:image/jpeg;base64,"):
        await message.answer("Некорректные данные из WebApp. Откройте форму через кнопку /start и попробуйте снова.")
        return

    _, encoded = raw_data.split(",", 1)

    try:
        image_bytes = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError):
        await message.answer("Некорректные данные изображения. Попробуйте снова.")
        return

    with tempfile.NamedTemporaryFile(suffix=".jpg", delete=False) as tmp:
        temp_path = Path(tmp.name)
        tmp.write(image_bytes)

    try:
        await message.answer_photo(types.InputFile(temp_path))
    finally:
        temp_path.unlink(missing_ok=True)


if __name__ == "__main__":
    executor.start_polling(dp, skip_updates=True)
