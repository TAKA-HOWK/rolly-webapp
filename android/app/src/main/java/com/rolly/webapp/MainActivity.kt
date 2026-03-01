package com.rolly.webapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // Основные настройки
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(true)
            // Разрешаем доступ к файлам
            allowFileAccess = true
            allowContentAccess = true
        }

        // WebViewClient с поддержкой навигации
        webView.webViewClient = WebViewClient()

        // WebChromeClient с поддержкой permissions и консоли
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request != null) {
                    request.grant(request.resources)
                }
            }
        }

        // Нативный мост для сохранения JPEG из JS в память телефона
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        webView.loadUrl("file:///android_asset/index.html")
    }

    private class AndroidBridge(private val context: Context) {
        @JavascriptInterface
        fun saveImage(dataUrl: String, filename: String): Boolean {
            return try {
                val base64 = dataUrl.substringAfter(',', "")
                if (base64.isBlank()) return false

                val bytes = Base64.decode(base64, Base64.DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(context, bytes, filename)
                } else {
                    saveLegacy(context, bytes, filename)
                }
            } catch (_: Exception) {
                false
            }
        }

        private fun saveWithMediaStore(context: Context, bytes: ByteArray, filename: String): Boolean {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Rolly")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            return try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                false
            }
        }

        private fun saveLegacy(context: Context, bytes: ByteArray, filename: String): Boolean {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "Rolly")
            if (!appDir.exists() && !appDir.mkdirs()) return false

            val outFile = File(appDir, filename)
            return try {
                FileOutputStream(outFile).use { it.write(bytes) }
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
