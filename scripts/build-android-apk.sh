#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
ASSET_HTML="$ANDROID_DIR/app/src/main/assets/index.html"
LOCAL_PROPERTIES="$ANDROID_DIR/local.properties"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Ошибка: gradle не найден в PATH. Установите Gradle 8+ или используйте Android Studio." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Ошибка: java не найдена в PATH. Нужна Java 17+." >&2
  exit 1
fi

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_DIR" && -f "$LOCAL_PROPERTIES" ]]; then
  SDK_DIR="$(sed -n 's/^sdk\.dir=//p' "$LOCAL_PROPERTIES" | head -n 1)"
fi

if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
  echo "Ошибка: Android SDK не найден." >&2
  echo "Задайте ANDROID_HOME (или ANDROID_SDK_ROOT) и повторите сборку." >&2
  echo "Пример: export ANDROID_HOME=\"$HOME/Android/Sdk\"" >&2
  exit 1
fi

if [[ ! -f "$LOCAL_PROPERTIES" ]]; then
  ESCAPED_SDK_DIR="${SDK_DIR//\\/\\\\}"
  printf 'sdk.dir=%s\n' "$ESCAPED_SDK_DIR" > "$LOCAL_PROPERTIES"
  echo "Создан $LOCAL_PROPERTIES" >&2
fi

echo "[1/3] Синхронизирую index.html в android assets..."
cp "$ROOT_DIR/index.html" "$ASSET_HTML"

echo "[2/3] Собираю debug APK..."
(
  cd "$ANDROID_DIR"
  gradle assembleDebug
)

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo "[3/3] Готово"
echo "APK: $APK_PATH"
