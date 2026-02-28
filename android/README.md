# Android APK build

Этот проект упаковывает текущий сайт (`index.html`) в Android-приложение (`WebView`) и собирает `.apk`.

## Быстрый старт (рекомендуется)

Из корня репозитория:

```bash
./scripts/build-android-apk.sh
```

Скрипт автоматически:
1. Копирует актуальный `index.html` в `android/app/src/main/assets/index.html`
2. Запускает `gradle assembleDebug`
3. Показывает путь к готовому APK

## Ручная сборка

```bash
cp index.html android/app/src/main/assets/index.html
cd android
gradle assembleDebug
```

Готовый файл:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Требования

- Java 17+
- Gradle 8+
- Android SDK (platform + build-tools, совместимые с настройками в `android/app/build.gradle`)

## Если сборка падает на скачивании зависимостей

Android Gradle Plugin тянется из Google Maven (`https://dl.google.com/dl/android/maven2/`).
Если сеть/прокси блокируют этот адрес, сборка не стартует.

Проверьте доступ:

```bash
curl -I https://dl.google.com/dl/android/maven2/
```

## Сборка в GitHub (через браузер)

Если локально не собирается, можно собрать APK на GitHub Actions:

1. Запушьте ветку в GitHub.
2. Откройте вкладку **Actions**.
3. Выберите workflow **Build Android APK**.
4. Нажмите **Run workflow**.
5. После завершения откройте run и скачайте артефакт **app-debug-apk**.

Внутри артефакта будет файл:

`android/app/build/outputs/apk/debug/app-debug.apk`
