package com.example.radcar

import android.app.Activity
import android.app.PrintManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.File

/**
 * Sample integration for a trusted app-controlled GitHub Pages origin.
 * Add androidx.core:core to your Android project for FileProvider.
 */
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var chooserCallback: ValueCallback<Array<Uri>>? = null
    private var photoUri: Uri? = null
    private val requestFiles = 7207
    private val trustedOrigin = "https://mmatin79-beep.github.io"
    private val startUrl = "$trustedOrigin/radcar/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(245, 243, 237)
        window.navigationBarColor = Color.rgb(245, 243, 237)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        webView = WebView(this)
        setContentView(webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "radcar" && url.host == "print") {
                    printCurrentPage()
                    return true
                }
                if (url.scheme == "https" && url.host == "mmatin79-beep.github.io") return false
                if (url.scheme == "https" && url.host == "fonts.googleapis.com") return false
                if (url.scheme == "https" && url.host == "fonts.gstatic.com") return false
                return true // Don't open unknown URLs inside this WebView.
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                chooserCallback?.onReceiveValue(null)
                chooserCallback = filePathCallback
                val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                val intents = mutableListOf<Intent>()
                val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (capture.resolveActivity(packageManager) != null) {
                    try {
                        val photo = File.createTempFile("radcar_", ".jpg", cacheDir)
                        photoUri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${applicationContext.packageName}.fileprovider",
                            photo
                        )
                        capture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        intents.add(capture)
                    } catch (_: Exception) { photoUri = null }
                }
                val chooser = Intent.createChooser(picker, "عکس خودرو").apply {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
                }
                return try {
                    startActivityForResult(chooser, requestFiles)
                    true
                } catch (_: Exception) {
                    chooserCallback?.onReceiveValue(null)
                    chooserCallback = null
                    false
                }
            }
        }
        webView.loadUrl(startUrl)
    }

    @Deprecated("Used for compatible WebView file chooser integration")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode != requestFiles) return
        val uris = if (resultCode == RESULT_OK) {
            when {
                intent?.clipData != null -> Array(intent.clipData!!.itemCount) { i -> intent.clipData!!.getItemAt(i).uri }
                intent?.data != null -> arrayOf(intent.data!!)
                photoUri != null -> arrayOf(photoUri!!)
                else -> null
            }
        } else null
        chooserCallback?.onReceiveValue(uris)
        chooserCallback = null
        photoUri = null
    }

    private fun printCurrentPage() {
        val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = webView.createPrintDocumentAdapter("Radcar_Report")
        manager.print("Radcar_Report", adapter, null)
    }

    @Deprecated("Sample for broad Android compatibility")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        chooserCallback?.onReceiveValue(null)
        chooserCallback = null
        webView.destroy()
        super.onDestroy()
    }
}
