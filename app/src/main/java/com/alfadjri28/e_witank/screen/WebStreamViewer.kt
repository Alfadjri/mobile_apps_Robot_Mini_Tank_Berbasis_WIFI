package com.alfadjri28.e_witank.screen

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStreamViewer(camIp: String) {
    val context = LocalContext.current
    val url = "http://$camIp:81/stream"

    // WebView hanya dibuat sekali
    val webView = remember(camIp) {
        WebView(context).apply {

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = "Mozilla/5.0"
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = false
            }

            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    Log.e("WEBVIEW", "Error: $description ($errorCode)")
                }
            }

            loadUrl(url)
        }
    }

    // Tampilkan WebView tanpa reload
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { webView }
    )

    // Destroy aman ketika composable hilang / IP berubah
    DisposableEffect(camIp) {
        onDispose {
            try {
                Log.d("WEBVIEW", "Destroy WebView untuk IP $camIp")

                webView.stopLoading()

                (webView.parent as? ViewGroup)?.removeView(webView)

                webView.loadUrl("about:blank")
                webView.removeAllViews()
                webView.destroy()

            } catch (e: Exception) {
                Log.e("WEBVIEW", "Destroy error: ${e.message}")
            }
        }
    }
}
