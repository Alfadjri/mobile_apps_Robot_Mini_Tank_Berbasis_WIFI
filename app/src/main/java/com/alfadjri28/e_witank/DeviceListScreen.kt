package com.alfadjri28.e_witank

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json // <-- Import tambahan
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface

// Data class untuk menampung hasil dari ESP
@Serializable
data class EspResponse(
    val status: String,
    val message: String? = null,
    val mode: String? = null
)

// Data class untuk perangkat yang ditemukan
data class ConnectedDevice(
    val ip: String,
    val message: String,
    val mode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State ---
    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET
    )
    var hasAllPermissions by remember {
        mutableStateOf(requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    var foundDevices by remember { mutableStateOf(listOf<ConnectedDevice>()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatusText by remember { mutableStateOf("Tekan tombol untuk memulai") }
    var connectionStatus by remember { mutableStateOf("Belum terhubung") }

    // --- ⭐ Ktor HTTP Client dengan Konfigurasi JSON yang Diperbaiki ⭐ ---
    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout)
            // Konfigurasi Ktor untuk mengerti JSON
            install(ContentNegotiation) {
                json(Json {
                    // Beri tahu parser untuk mengabaikan error jika ada koma di akhir
                    isLenient = true
                    ignoreUnknownKeys = true // Tambahan agar lebih fleksibel
                })
            }
        }
    }
    val espPassword = "kentanakaRCCamConnection"

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAllPermissions = permissions.values.all { it }
    }

    fun startNetworkScan() {
        if (isScanning) return

        coroutineScope.launch {
            isScanning = true
            foundDevices = emptyList()
            scanStatusText = "Mencari alamat IP Hotspot..."

            withContext(Dispatchers.IO) {
                var ipAddress: String? = null
                try {
                    val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                    while (networkInterfaces.hasMoreElements()) {
                        val intf = networkInterfaces.nextElement()
                        if (intf.isUp && (intf.name.contains("ap") || intf.name.contains("wlan"))) {
                            val addrs = intf.inetAddresses
                            while (addrs.hasMoreElements()) {
                                val addr = addrs.nextElement()
                                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                                    ipAddress = addr.hostAddress
                                    break
                                }
                            }
                        }
                        if (ipAddress != null) break
                    }
                } catch (e: Exception) {
                    Log.e("NETWORK_SCAN", "Error getting hotspot IP", e)
                }

                if (ipAddress == null) {
                    withContext(Dispatchers.Main) {
                        scanStatusText = "Gagal: Tidak dapat mendeteksi IP Hotspot. Pastikan hotspot aktif."
                        isScanning = false
                    }
                    return@withContext
                }

                val subnet = ipAddress.substringBeforeLast('.')
                Log.d("NETWORK_SCAN", "Hotspot IP Found: $ipAddress, Subnet to scan: $subnet")


                withContext(Dispatchers.Main) {
                    scanStatusText = "Memindai subnet: $subnet.x"
                }

                val timeout = 200
                val jobs = mutableListOf<Job>()

                for (i in 1..254) {
                    withContext(Dispatchers.Main) {
                        scanProgress = i / 254f
                    }

                    val host = "$subnet.$i"
                    if (host == ipAddress) continue

                    val job = launch {
                        try {
                            val socket = java.net.Socket()
                            socket.connect(InetSocketAddress(host, 80), timeout)
                            socket.close()

                            Log.d("NETWORK_SCAN", "Device responded at $host, verifying...")

                            val response: EspResponse = client.submitForm(
                                url = "http://$host/",
                                formParameters = parameters {
                                    append("password", espPassword)
                                }
                            ) {
                                timeout { requestTimeoutMillis = 3000 }
                            }.body()

                            Log.i("NETWORK_SCAN", "Response from $host | Parsed Status: ${response.status}")

                            if (response.status == "ok") {
                                Log.d("NETWORK_SCAN", "ESP Device VERIFIED at: $host")
                                withContext(Dispatchers.Main) {
                                    if (foundDevices.none { it.ip == host }) {
                                        foundDevices = foundDevices + ConnectedDevice(
                                            ip = host,
                                            message = response.message ?: "N/A",
                                            mode = response.mode ?: "N/A"
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e !is java.net.SocketTimeoutException && e !is java.net.ConnectException) {
                                Log.e("NETWORK_SCAN", "Error processing host $host", e)
                            }
                        }
                    }
                    jobs.add(job)
                }
                jobs.joinAll()
            }

            scanStatusText = if (foundDevices.isEmpty()) {
                "Pemindaian selesai. Tidak ada alat yang ditemukan."
            } else {
                "Pemindaian selesai. ${foundDevices.size} alat ditemukan."
            }
            isScanning = false
        }
    }

    fun connectToDevice(device: ConnectedDevice) {
        connectionStatus = "✅ Berhasil terhubung ke Alat di ${device.ip}!"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Perangkat di Jaringan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!hasAllPermissions) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Aplikasi membutuhkan izin untuk memindai jaringan.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { requestPermissionsLauncher.launch(requiredPermissions) }) {
                        Text("Berikan Izin")
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { startNetworkScan() },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isScanning) "Memindai..." else "Mulai Pindai Jaringan")
                    }

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = scanStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Perangkat Ditemukan:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (foundDevices.isEmpty()) {
                        Text(if (isScanning) "Menunggu hasil..." else "Belum ada perangkat ditemukan.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(foundDevices) { device ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            "Alat Terdeteksi (Controller)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D47A1)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("IP: ${device.ip}")
                                        Text("Message: ${device.message}")
                                        Text("Mode: ${device.mode}")
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { connectToDevice(device) },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Gunakan Perangkat Ini")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Status: $connectionStatus",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        }
    }
}
