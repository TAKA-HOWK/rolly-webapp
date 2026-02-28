package com.rolly.webapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var pendingFilename: String? = null
    private val pendingBase64 = StringBuilder()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun saveJpegBase64(filename: String, base64Data: String) {
            saveFromBase64(filename, base64Data)
        }

        @JavascriptInterface
        fun beginJpegSave(filename: String) {
            pendingFilename = filename
            pendingBase64.setLength(0)
        }

        @JavascriptInterface
        fun appendJpegChunk(chunk: String) {
            pendingBase64.append(chunk)
        }

        @JavascriptInterface
        fun finishJpegSave(): Boolean {
            val name = pendingFilename ?: return false
            return try {
                saveFromBase64(name, pendingBase64.toString())
                true
            } catch (_: Exception) {
                false
            } finally {
                pendingFilename = null
                pendingBase64.setLength(0)
            }
        }
    }

    private fun saveFromBase64(filename: String, base64Data: String) {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        saveImageToGallery(filename, bytes)
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Сохранено: $filename", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToGallery(filename: String, data: ByteArray) {
        val resolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Rolly")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")

        resolver.openOutputStream(uri).use { stream ->
            if (stream == null) throw IOException("OutputStream is null")
            stream.write(data)
            stream.flush()
        }

        val done = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, done, null, null)
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
