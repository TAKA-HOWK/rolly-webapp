# Android APK build

Этот проект оборачивает текущий `index.html` в нативное Android-приложение (`WebView`).

## Требования

- Android SDK (platform 35, build-tools)
- Java 17+
- Gradle 8+

## Сборка debug APK

```bash
cd android
gradle assembleDebug
```

Готовый файл:

`app/build/outputs/apk/debug/app-debug.apk`

## Если меняете сайт

После правок в корневом `index.html` обновите копию для Android:

```bash
cp ../index.html app/src/main/assets/index.html
```
