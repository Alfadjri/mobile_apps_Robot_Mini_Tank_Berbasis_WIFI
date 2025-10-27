package com.alfadjri28.e_witank.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSearchScreen(
    navController: NavController,
    ip: String,
    camID: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { LocalStorageControllerRC(context) }
    val coroutineScope = rememberCoroutineScope()

    var foundCameraIp by remember { mutableStateOf<String?>(null) }
    var foundCamId by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Menunggu pemindaian kamera...") }

    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 800
                connectTimeoutMillis = 500
                socketTimeoutMillis = 500
            }
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
        }
    }

    fun startScan() {
        if (isScanning) return
        isScanning = true
        val scanner = CameraScanner(ip, camID, storage, client)

        coroutineScope.launch {
            scanner.scan(this) { response, host ->
                if (host != null) {
                    foundCameraIp = host
                    foundCamId = response?.cam_id ?: camID
                    statusText = "✅ Kamera ditemukan di $host"
                } else {
                    statusText = "❌ Kamera tidak ditemukan di jaringan yang sama."
                }
                isScanning = false
            }

            while (isScanning) {
                progress = scanner.progress
                delay(50)
            }
        }
    }

    LaunchedEffect(Unit) {
        val existingCamIP = storage.getCamIPByCamID(camID) // ambil camIP jika ada
        if (!existingCamIP.isNullOrEmpty()) {
            // Kalau sudah ada camIP, tampilkan langsung
            foundCameraIp = existingCamIP
            foundCamId = camID
            statusText = "✅ Kamera sudah tersimpan di $existingCamIP"
        } else {
            // Kalau camIP belum ada → lakukan scanning
            startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Kamera RC") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Controller IP: $ip", fontWeight = FontWeight.Bold)
            Text("Controller camID: $camID", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusText)
            } else {
                Text(statusText)
            }

            Spacer(modifier = Modifier.height(24.dp))

            foundCameraIp?.let { camIp ->
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
                        Text(foundCamId ?: "-")
                    }
                }
            }
        }
    }
}
