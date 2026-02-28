#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
ASSET_HTML="$ANDROID_DIR/app/src/main/assets/index.html"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Ошибка: gradle не найден в PATH. Установите Gradle 8+ или используйте Android Studio." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Ошибка: java не найдена в PATH. Нужна Java 17+." >&2
  exit 1
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
