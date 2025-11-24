package com.alfadjri28.e_witank.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json


// ====================== ViewModel ======================
class CameraViewModel : ViewModel() {
    var foundCameraIp by mutableStateOf<String?>(null)
    var showStream by mutableStateOf(false)
    var isScanning by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var statusText by mutableStateOf("Menunggu pemindaian kamera...")

    fun startScan(
        ip: String,
        camID: String,
        storage: LocalStorageControllerRC,
        client: HttpClient
    ) {
        if (isScanning) return
        isScanning = true
        val scanner = CameraScanner(ip, camID, storage, client)

        viewModelScope.launch {
            var found = false
            scanner.scan(this) { response, host ->
                if (host != null) {
                    Log.d("CAMERA_SCAN", "Kamera ditemukan! IP Kamera = $host (camID = $camID)")
                    foundCameraIp = host
                    statusText = "✅ Kamera ditemukan di $host"
                    showStream = true
                    found = true
                }
            }

            while (isScanning && !found) {
                progress = scanner.progress
                delay(50)
            }
            if (!found) statusText = "❌ Kamera tidak ditemukan."
            isScanning = false
        }
    }
}

class ControlViewModel : ViewModel() {
    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1500 }
    }

    private var commandJob: Job? = null

    fun sendCommand(controllerIp: String, command: String) {
        commandJob?.cancel()
        commandJob = viewModelScope.launch(Dispatchers.IO) {
            val url = "http://$controllerIp/a/$command"
            try {
                Log.d("CONTROL", "Mengirim perintah: $url")
                client.get(url) {
                    contentType(ContentType.Application.Json)
                }
            } catch (e: Exception) {
                Log.e("CONTROL", "Gagal mengirim perintah ke $url: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }
}



// ====================== HoldableButton ======================
@Composable
fun HoldableIconButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onPress()
                    try {
                        awaitRelease()
                    } finally {
                        onRelease()
                    }
                }
            )
        }
    ) {
        content()
    }
}

// ====================== MAIN SCREEN ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSearchAndStreamScreen(
    navController: NavController,
    ip: String,
    camID: String,
    cameraViewModel: CameraViewModel = viewModel(),
    controlViewModel: ControlViewModel = viewModel()
) {
    val context = LocalContext.current
    val storage = remember { LocalStorageControllerRC(context) }
    var isFullscreen by remember { mutableStateOf(false) }

    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout) { requestTimeoutMillis = 5000; connectTimeoutMillis = 3000 }
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    LaunchedEffect(camID) {
        Log.d("CONTROLLER_IP", "IP Controller yang digunakan = $ip")
        Log.d("CONTROLLER_IP", "camID = $camID")
        val cachedIP = storage.getCamIPByCamID(camID)
        if (!cachedIP.isNullOrEmpty()) {
            cameraViewModel.foundCameraIp = cachedIP
            cameraViewModel.statusText = "✅ Kamera tersimpan di $cachedIP"
            cameraViewModel.showStream = true
        } else {
            cameraViewModel.startScan(ip, camID, storage, client)
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(if (cameraViewModel.showStream) "Live Camera Stream" else "Pindai Kamera RC") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        if (cameraViewModel.showStream) {
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Toggle Fullscreen")
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
            if (!cameraViewModel.showStream) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Controller IP: $ip", fontWeight = FontWeight.Bold)
                    Text("Controller camID: $camID", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(16.dp))
                    if (cameraViewModel.isScanning) {
                        LinearProgressIndicator(
                            progress = { cameraViewModel.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(cameraViewModel.statusText)
                }
            } else {
                cameraViewModel.foundCameraIp?.let { camIp ->
                    Box(Modifier.fillMaxSize()) {
                        Log.d("CAMERA_STREAM", "Streaming menggunakan URL: http://$camIp:81/stream")

                        WebStreamViewer(camIp = camIp)

                        if (isFullscreen) {
                            val buttonSize = 80.dp
                            val pad = 24.dp
                            val bg = Color.Black.copy(alpha = 0.3f)

                            Column(
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = pad),
                                verticalArrangement = Arrangement.spacedBy(pad)
                            ) {
                                HoldableIconButton(
                                    onPress = { controlViewModel.sendCommand(ip, "maju") },
                                    onRelease = { controlViewModel.sendCommand(ip, "stop") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Maju",
                                        modifier = Modifier.size(buttonSize).clip(CircleShape)
                                            .background(bg).padding(16.dp).rotate(90f),
                                        tint = Color.White
                                    )
                                }

                                HoldableIconButton(
                                    onPress = { controlViewModel.sendCommand(ip, "mundur") },
                                    onRelease = { controlViewModel.sendCommand(ip, "stop") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Mundur",
                                        modifier = Modifier.size(buttonSize).clip(CircleShape)
                                            .background(bg).padding(16.dp).rotate(-90f),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FULLSCREEN + LOCK ORIENTATION
    LaunchedEffect(isFullscreen) {
        val activity = context as Activity
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}
