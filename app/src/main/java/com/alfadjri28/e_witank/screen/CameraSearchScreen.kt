package com.alfadjri28.e_witank.screen

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

// ====================== ViewModel ======================
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
            Log.d("SCAN", "🚀 Mulai scan kamera (IP awal: $ip, camID: $camID)")
            repeat(retries) { attempt ->
                var found = false
                Log.d("SCAN", "Percobaan scan ke-${attempt + 1}")

                scanner.scan(this) { response, host ->
                    if (host != null) {
                        foundCameraIp = host
                        foundCamId = response?.cam_id ?: camID
                        statusText = "✅ Kamera ditemukan di $host"
                        showStream = true
                        found = true
                        Log.i("SCAN", "Kamera ditemukan di $host (camID: ${response?.cam_id})")
                    } else {
                        Log.w("SCAN", "Kamera tidak ditemukan pada percobaan ke-${attempt + 1}")
                        statusText = "⚠️ Kamera tidak ditemukan (percobaan ${attempt + 1}/$retries)"
                    }
                }

                while (isScanning && !found) {
                    progress = scanner.progress
                    delay(50)
                }

                if (found) {
                    Log.d("SCAN", "Berhenti scan karena kamera sudah ditemukan.")
                    return@launch
                }
            }

            if (!showStream) {
                statusText = "❌ Kamera tidak ditemukan setelah $retries percobaan."
                Log.e("SCAN", "Gagal menemukan kamera setelah $retries kali.")
            }
            isScanning = false
        }
    }
}

// ====================== MJPEG Streamer ======================
@Composable
fun MjpegStreamViewer(camIp: String, isFullscreen: Boolean) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(camIp) {
        val job = scope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://$camIp/stream")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "E_Witank_MJPEG_Viewer")
                conn.readTimeout = 0
                conn.connectTimeout = 5000
                conn.connect()

                val input = BufferedInputStream(conn.inputStream)
                val delimiter = "\r\n\r\n".toByteArray()
                val headerBuffer = ByteArrayOutputStream()

                while (isActive) {
                    headerBuffer.reset()
                    var curr: Int
                    while (true) {
                        curr = input.read()
                        if (curr == -1) return@launch
                        headerBuffer.write(curr)
                        val arr = headerBuffer.toByteArray()
                        if (arr.size >= delimiter.size &&
                            arr.copyOfRange(arr.size - delimiter.size, arr.size).contentEquals(delimiter)
                        ) break
                    }

                    val header = headerBuffer.toString(Charsets.ISO_8859_1.name())
                    val contentLength = Regex("Content-Length:\\s*(\\d+)").find(header)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: continue

                    val imageBytes = ByteArray(contentLength)
                    var offset = 0
                    while (offset < contentLength) {
                        val bytesRead = input.read(imageBytes, offset, contentLength - offset)
                        if (bytesRead == -1) break
                        offset += bytesRead
                    }

                    val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, contentLength)
                    bmp?.let {
                        withContext(Dispatchers.Main) {
                            bitmap = it.asImageBitmap()
                        }
                    }

                    input.read()
                    input.read()
                }

                input.close()
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("MJPEG", "❌ Stream error: ${e.message}")
            }
        }

        onDispose { job.cancel() }
    }

    // 🌀 UI Tampilan Stream
    Box(
        modifier = Modifier
            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16 / 9f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            // 🔥 Tambahkan rotasi 90° kalau fullscreen aktif
            val rotationAngle = if (isFullscreen) 90f else 0f
            Image(
                bitmap = it,
                contentDescription = "Live Stream",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle)
            )
        } ?: CircularProgressIndicator()
    }
}

private fun ByteArray.indexOfSequence(seq: ByteArray): Int {
    outer@ for (i in 0..this.size - seq.size) {
        for (j in seq.indices) if (this[i + j] != seq[j]) continue@outer
        return i
    }
    return -1
}

// ====================== Main Screen ======================
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

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

    // tombol back di fullscreen
    BackHandler(enabled = isFullscreen) {
        Log.d("UI", "🔙 Keluar dari fullscreen")
        isFullscreen = false
    }

    LaunchedEffect(camID) {
        val cachedIP = storage.getCamIPByCamID(camID)
        if (!cachedIP.isNullOrEmpty()) {
            viewModel.foundCameraIp = cachedIP
            viewModel.foundCamId = camID
            viewModel.statusText = "✅ Kamera tersimpan di $cachedIP"
            viewModel.showStream = true
        } else {
            Log.d("SCAN", "🚀 Mulai proses scan kamera...")
            viewModel.startScan(ip, camID, storage, client)
        }
    }

    Scaffold(
        topBar = {
            // 🔧 Tampilkan TopAppBar hanya jika fullscreen OFF
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(if (viewModel.showStream) "Live Camera Stream" else "Pindai Kamera RC") },
                    navigationIcon = {
                        IconButton(onClick = {
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
                                    imageVector = if (isFullscreen)
                                        Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
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
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (!viewModel.showStream) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Controller IP: $ip", fontWeight = FontWeight.Bold)
                    Text("Controller camID: $camID", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viewModel.isScanning) {
                        LinearProgressIndicator(progress = viewModel.progress, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(viewModel.statusText)
                }
            } else {
                viewModel.foundCameraIp?.let { camIp ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // tampilkan stream
                        MjpegStreamViewer(camIp, isFullscreen)

                        // 🔧 Tambahkan tombol exit fullscreen di pojok kanan atas
                        if (isFullscreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                IconButton(
                                    onClick = { isFullscreen = false },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ====================== Fullscreen Mode ======================
    LaunchedEffect(isFullscreen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        if (isFullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(android.view.WindowInsets.Type.systemBars())
                    it.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}

