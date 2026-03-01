package com.rolly.webapp

import android.annotation.SuppressLint
import android.os.Bundle
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

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(true)
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")
    }

    private class AndroidBridge(private val context: android.content.Context) {
        @JavascriptInterface
        fun saveImage(dataUrl: String, filename: String): Boolean {
            return try {
                val base64 = dataUrl.substringAfter(',', "")
                if (base64.isBlank()) return false

                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    saveWithMediaStore(bytes, filename)
                } else {
                    saveLegacy(bytes, filename)
                }
            } catch (_: Exception) {
                false
            }
        }

        private fun saveWithMediaStore(bytes: ByteArray, filename: String): Boolean {
            val resolver = context.contentResolver
            val values = android.content.ContentValues()
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/Rolly"
            )
            values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)

            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false

            try {
                val output = resolver.openOutputStream(uri) ?: return false
                output.write(bytes)
                output.flush()
                output.close()

                val done = android.content.ContentValues()
                done.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, done, null, null)
                return true
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                return false
            }
        }

        private fun saveLegacy(bytes: ByteArray, filename: String): Boolean {
            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES
            )
            val appDir = File(picturesDir, "Rolly")
            if (!appDir.exists() && !appDir.mkdirs()) return false

            val outFile = File(appDir, filename)
            return try {
                val fos = FileOutputStream(outFile)
                fos.write(bytes)
                fos.flush()
                fos.close()

                context.sendBroadcast(
                    android.content.Intent(
                        android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        android.net.Uri.fromFile(outFile)
                    )
                )
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
