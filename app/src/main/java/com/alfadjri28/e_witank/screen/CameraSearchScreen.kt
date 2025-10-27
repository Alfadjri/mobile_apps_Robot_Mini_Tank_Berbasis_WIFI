package com.alfadjri28.e_witank.screen

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.webkit.WebView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.alfadjri28.e_witank.logic.CameraScanner
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// -------------------- ViewModel --------------------
class CameraViewModel : ViewModel() {
    var foundCameraIp by mutableStateOf<String?>(null)
    var foundCamId by mutableStateOf<String?>(null)
    var showStream by mutableStateOf(false)
    var isScanning by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var statusText by mutableStateOf("Menunggu pemindaian kamera...")

    fun startScan(
        ip: String,
        camID: String,
        storage: LocalStorageControllerRC,
        client: HttpClient,
        retries: Int = 3
    ) {
        if (isScanning) return
        isScanning = true
        val scanner = CameraScanner(ip, camID, storage, client)

        viewModelScope.launch {
            repeat(retries) { attempt ->
                var found = false
                scanner.scan(this) { response, host ->
                    if (host != null) {
                        foundCameraIp = host
                        foundCamId = response?.cam_id ?: camID
                        statusText = "✅ Kamera ditemukan di $host"
                        showStream = true
                        found = true
                    } else {
                        statusText = "⚠️ Kamera tidak ditemukan (percobaan ${attempt + 1}/$retries)"
                    }
                }

                while (isScanning && !found) {
                    progress = scanner.progress
                    delay(50)
                }

                if (found) return@launch
            }

            if (!showStream) {
                statusText = "❌ Kamera tidak ditemukan setelah $retries percobaan."
            }
            isScanning = false
        }
    }
}

// -------------------- Helper cek jaringan --------------------
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// -------------------- Composable --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSearchAndStreamScreen(
    navController: NavController,
    ip: String,
    camID: String,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val storage = remember { LocalStorageControllerRC(context) }
    var isFullscreen by remember { mutableStateOf(false) }

    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 3000
                socketTimeoutMillis = 5000
            }
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
        }
    }

    // ---------------- Scan / load cached IP ----------------
    LaunchedEffect(camID) {
        if (!isNetworkAvailable(context)) {
            viewModel.statusText = "❌ Jaringan tidak tersedia"
            return@LaunchedEffect
        }

        val cachedIP = storage.getCamIPByCamID(camID)
        if (!cachedIP.isNullOrEmpty()) {
            viewModel.foundCameraIp = cachedIP
            viewModel.foundCamId = camID
            viewModel.statusText = "✅ Kamera sudah tersimpan di $cachedIP"
            viewModel.showStream = true
        } else {
            viewModel.startScan(ip, camID, storage, client)
        }
    }

    // ---------------- Scaffold ----------------
    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(if (viewModel.showStream) "Live Camera Stream" else "Pindai Kamera RC") },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Hentikan WebView stream
                            viewModel.showStream = false
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        if (viewModel.showStream) {
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen"
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!viewModel.showStream) {
                // ---------------- UI Scan ----------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Controller IP: $ip", fontWeight = FontWeight.Bold)
                    Text("Controller camID: $camID", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.isScanning) {
                        LinearProgressIndicator(progress = viewModel.progress, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(viewModel.statusText)
                    } else {
                        Text(viewModel.statusText)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    viewModel.foundCameraIp?.let { camIp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("camIP:", fontWeight = FontWeight.Bold)
                                Text(camIp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("camID:", fontWeight = FontWeight.Bold)
                                Text(viewModel.foundCamId ?: "-")
                            }
                        }
                    }
                }
            } else {
                // ---------------- UI Streaming ----------------
                viewModel.foundCameraIp?.let { camIp ->
                    key(camIp) {
                        var webView: WebView? = null
                        AndroidView(
                            factory = { context ->
                                webView = WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val js = """
                                                javascript:(function() {
                                                    document.body.style.backgroundColor = 'black';
                                                    document.body.style.margin='0';
                                                    document.body.style.padding='0';
                                                    Array.from(document.getElementsByTagName('video')).forEach(v=>{
                                                        v.style.width='100%';
                                                        v.style.height='100%';
                                                        v.style.objectFit='cover';
                                                    });
                                                    Array.from(document.getElementsByTagName('img')).forEach(i=>{
                                                        i.style.width='100%';
                                                        i.style.height='100%';
                                                        i.style.objectFit='contain';
                                                    });
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(js, null)
                                        }

                                        override fun onReceivedError(
                                            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            viewModel.statusText = "⚠️ Gagal load stream"
                                            Log.e("WebViewError", "Gagal load $camIp: ${error?.description}")
                                        }
                                    }
                                    loadUrl("http://$camIp/stream")
                                }
                                webView!!
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // ---------------- Dispose WebView ----------------
                        DisposableEffect(Unit) {
                            onDispose {
                                webView?.apply {
                                    stopLoading()
                                    removeAllViews()
                                    destroy()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- Fullscreen ----------------
    LaunchedEffect(isFullscreen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        activity.window.decorView.systemUiVisibility =
            if (isFullscreen) android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            else android.view.View.SYSTEM_UI_FLAG_VISIBLE
    }
}
