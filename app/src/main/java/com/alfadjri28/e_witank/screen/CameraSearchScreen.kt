package com.alfadjri28.e_witank.screen

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.alfadjri28.e_witank.logic.CameraScanner
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import com.alfadjri28.e_witank.utils.setLandscape
import com.alfadjri28.e_witank.utils.setPortrait
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

// ====================== ViewModel Kamera ======================
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
            scanner.scan(this) { _, host ->
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

// ====================== ViewModel Kontrol ======================
class ControlViewModel : ViewModel() {
    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1500 }
    }

    private val commandJobs = mutableMapOf<String, Job?>()

    private fun launchForKey(key: String, block: suspend () -> Unit) {
        commandJobs[key]?.cancel()
        commandJobs[key] = viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }

    fun sendCommand(controllerIp: String, channel: String, command: String) {
        val url = "http://$controllerIp/$channel/$command"
        launchForKey(channel) {
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

    fun sendBoth(controllerIp: String, command: String) {
        sendCommand(controllerIp, "a", command)
        sendCommand(controllerIp, "b", command)
    }

    override fun onCleared() {
        commandJobs.values.forEach { it?.cancel() }
        client.close()
        super.onCleared()
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
                    title = {
                        Text(
                            if (cameraViewModel.showStream)
                                "Live Camera Stream"
                            else
                                "Pindai Kamera RC"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        if (cameraViewModel.showStream) {
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    if (isFullscreen) Icons.Default.FullscreenExit
                                    else Icons.Default.Fullscreen,
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
            if (!cameraViewModel.showStream) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
                    if (isFullscreen) {
                        // ===================== MODE FULLSCREEN =====================
                        Box(modifier = Modifier.fillMaxSize()) {

                            // 🎥 VIDEO FULL
                            WebStreamViewer(
                                camIp = camIp,
                                rotationDegrees = 0f
                            )

                            // 🔴 TOMBOL EXIT FULLSCREEN (KANAN ATAS)
                            IconButton(
                                onClick = { isFullscreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 🕹️ KONTROL RC (KIRI & KANAN BAWAH)
                            FullscreenTankControls(
                                ip = ip,
                                controlViewModel = controlViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                    } else {
                        // ===================== MODE NON-FULLSCREEN =====================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                WebStreamViewer(camIp = camIp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PortraitTankControls(
                                ip = ip,
                                controlViewModel = controlViewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // FULLSCREEN: hide/show system bar
    LaunchedEffect(isFullscreen) {
        val activity = context as Activity

        if (isFullscreen) {
            // 🔥 MASUK FULLSCREEN → LANDSCAPE
            setLandscape(activity)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController
                    ?.hide(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        } else {
            // 🔁 KELUAR FULLSCREEN → PORTRAIT
            setPortrait(activity)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController
                    ?.show(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

}
