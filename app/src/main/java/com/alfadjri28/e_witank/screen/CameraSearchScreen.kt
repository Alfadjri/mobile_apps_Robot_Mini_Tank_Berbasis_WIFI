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
            repeat(retries) { attempt ->
                var found = false
                scanner.scan(this) { response, host ->
                    if (host != null) {
                        foundCameraIp = host
                        foundCamId = response?.cam_id ?: camID
                        statusText = "✅ Kamera ditemukan di $host"
                        showStream = true
                        found = true
                    }
                }

                while (isScanning && !found) {
                    progress = scanner.progress
                    delay(50)
                }
                if (found) return@launch
            }
            if (!showStream) statusText = "❌ Kamera tidak ditemukan setelah $retries percobaan."
            isScanning = false
        }
    }
}

// ====================== MJPEG Streamer ======================
@Composable
fun MjpegStreamViewer(camIp: String, isFullscreen: Boolean, restartSignal: Boolean) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()
    var streamJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(camIp, restartSignal) {
        // Cancel stream lama jika ada
        streamJob?.cancel()
        bitmap = null

        streamJob = scope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            var input: BufferedInputStream? = null
            try {
                val url = URL("http://$camIp/stream")
                conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "E_Witank_MJPEG_Viewer")
                conn.readTimeout = 0
                conn.connectTimeout = 5000
                conn.connect()

                input = BufferedInputStream(conn.inputStream)
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
                        withContext(Dispatchers.Main) { bitmap = it.asImageBitmap() }
                    }

                    input.read()
                    input.read()
                }
            } catch (e: Exception) {
                Log.e("MJPEG", "❌ Stream error: ${e.message}")
            } finally {
                input?.close()
                conn?.disconnect()
            }
        }

        onDispose {
            streamJob?.cancel()
            bitmap = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            val rotationAngle = if (isFullscreen) 90f else 0f
            Image(bitmap = it, contentDescription = "Live Stream", modifier = Modifier.fillMaxSize().rotate(rotationAngle))
        } ?: CircularProgressIndicator()
    }
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
    var restartStream by remember { mutableStateOf(false) }

    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 3000
                socketTimeoutMillis = 5000
            }
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    LaunchedEffect(camID) {
        val cachedIP = storage.getCamIPByCamID(camID)
        if (!cachedIP.isNullOrEmpty()) {
            viewModel.foundCameraIp = cachedIP
            viewModel.foundCamId = camID
            viewModel.statusText = "✅ Kamera tersimpan di $cachedIP"
            viewModel.showStream = true
        } else {
            viewModel.startScan(ip, camID, storage, client)
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(if (viewModel.showStream) "Live Camera Stream" else "Pindai Kamera RC") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.showStream = false
                            navController.popBackStack()
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                    },
                    actions = {
                        if (viewModel.showStream) {
                            IconButton(onClick = {
                                restartStream = true
                                isFullscreen = !isFullscreen
                            }) {
                                Icon(imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Toggle Fullscreen")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!viewModel.showStream) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Controller IP: $ip", fontWeight = FontWeight.Bold)
                    Text("Controller camID: $camID", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viewModel.isScanning) LinearProgressIndicator(progress = viewModel.progress, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(viewModel.statusText)
                }
            } else {
                viewModel.foundCameraIp?.let { camIp ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(if (isFullscreen) 1f else 0.9f)
                                .aspectRatio(16 / 9f)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MjpegStreamViewer(camIp, isFullscreen, restartStream)
                            restartStream = false
                        }
                        if (!isFullscreen) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Kamera: $camIp", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    // Fullscreen handling
    LaunchedEffect(isFullscreen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        if (isFullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
                window.insetsController?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
