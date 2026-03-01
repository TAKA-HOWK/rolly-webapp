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

unescape_gradle_path() {
  local raw="$1"
  # Gradle local.properties can contain escaped separators (\\), colons (\:) and spaces (\ )
  raw="${raw//\\:/:}"
  raw="${raw//\\ / }"
  raw="${raw//\\\\/\\}"
  printf '%s' "$raw"
}

get_sdk_from_local_properties() {
  if [[ ! -f "$LOCAL_PROPERTIES" ]]; then
    return 0
  fi

  local raw
  raw="$(sed -n 's/^sdk\.dir=//p' "$LOCAL_PROPERTIES" | head -n 1)"
  if [[ -z "$raw" ]]; then
    return 0
  fi

  unescape_gradle_path "$raw"
}

find_sdk_dir() {
  local candidate

  for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$(get_sdk_from_local_properties)" \
    "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/opt/android-sdk" "/usr/lib/android-sdk"; do
    if [[ -n "$candidate" && -d "$candidate" ]]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
}

SDK_DIR="$(find_sdk_dir)"

if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
  echo "Ошибка: Android SDK не найден." >&2
  echo "Проверьте переменные ANDROID_HOME / ANDROID_SDK_ROOT или sdk.dir в android/local.properties." >&2
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
