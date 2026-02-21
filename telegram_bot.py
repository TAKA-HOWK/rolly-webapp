import base64
import binascii
import os
import tempfile
from pathlib import Path

from aiogram import Bot, Dispatcher, types
from aiogram.types import KeyboardButton, ReplyKeyboardMarkup, WebAppInfo
from aiogram.utils import executor


def load_dotenv(path: str = ".env") -> None:
    env_path = Path(path)
    if not env_path.exists():
        return

    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")

        if key and key not in os.environ:
            os.environ[key] = value


load_dotenv()

TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
WEBAPP_URL = os.getenv("WEBAPP_URL", "https://taka-howk.github.io/rolly-webapp/")

if not TOKEN:
    raise RuntimeError("Set TELEGRAM_BOT_TOKEN in environment or .env file")

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

    if not raw_data.startswith("data:image/jpeg;base64,"):
        await message.answer("Ожидался JPEG из WebApp. Попробуйте снова.")
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
