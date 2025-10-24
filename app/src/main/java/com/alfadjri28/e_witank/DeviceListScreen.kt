package com.alfadjri28.e_witank

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar // <-- Ikon mobil
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview // <-- Import untuk @Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alfadjri28.e_witank.ui.theme.EWiTankTheme // Pastikan import theme Anda benar
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
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface

// Data class untuk menampung hasil dari ESP (disesuaikan dengan JSON dari ESP)
@Serializable
data class EspResponse(
    val status: String,
    val model: String? = null,
    val controller_id: String? = null,
    val cam_id: String? = null
)

// Data class untuk perangkat yang ditemukan (disesuaikan untuk menyimpan ID)
data class ConnectedDevice(
    val ip: String,
    val model: String,
    val controllerId: String,
    val camId: String
)

// =====================================================================
// ⭐ UI CARD YANG SUDAH DIPISAHKAN ⭐
// Di sinilah Anda bisa mengubah tampilan kartu sesuai keinginan.
// =====================================================================
@Composable
fun DeviceListItemCard(
    device: ConnectedDevice,
    isSelected: Boolean,
    onClick: (ConnectedDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(150.dp)
            .clickable { onClick(device) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = Color(0xFFB9F6CA))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Ikon Mobil",
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF0D47A1)
            )
        }
    }
}


// =====================================================================
// ⭐ FUNGSI UTAMA SCREEN (LOGIKA) ⭐
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE, // ✅ tambahan
        Manifest.permission.INTERNET
    )

    var hasAllPermissions by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    var foundDevices by remember { mutableStateOf(listOf<ConnectedDevice>()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatusText by remember { mutableStateOf("Tekan tombol untuk memulai") }
    var selectedDevice by remember { mutableStateOf<ConnectedDevice?>(null) }

    val client = remember {
        HttpClient(Android) {
            install(HttpTimeout)
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
        }
    }

    val espPassword = "kentanakaRCCamConnection"

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAllPermissions = permissions.values.all { it }
    }

    // =======================================================
    fun startNetworkScan() {
        if (isScanning) return

        coroutineScope.launch {
            isScanning = true
            foundDevices = emptyList()
            selectedDevice = null
            scanStatusText = "Mencari alamat IP Hotspot..."

            val ipAddress = withContext(Dispatchers.IO) {
                try {
                    NetworkInterface.getNetworkInterfaces().toList()
                        .filter { it.isUp && (it.name.contains("ap") || it.name.contains("wlan")) }
                        .flatMap { it.inetAddresses.toList() }
                        .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                        ?.hostAddress
                } catch (e: Exception) {
                    Log.e("NETWORK_SCAN", "Error getting hotspot IP", e)
                    null
                }
            }

            if (ipAddress == null) {
                scanStatusText = "Gagal: Tidak dapat mendeteksi IP Hotspot."
                isScanning = false
                return@launch
            }

            val subnet = ipAddress.substringBeforeLast('.')
            scanStatusText = "Memindai subnet: $subnet.x"
            val timeout = 200

            withContext(Dispatchers.IO) {
                for (i in 1..254) {
                    val host = "$subnet.$i"
                    if (host == ipAddress) continue

                    try {
                        val socket = java.net.Socket()
                        socket.connect(InetSocketAddress(host, 80), timeout)
                        socket.close()

                        val response: EspResponse = client.submitForm(
                            url = "http://$host/",
                            formParameters = parameters { append("password", espPassword) }
                        ) {
                            timeout { requestTimeoutMillis = 2000 }
                        }.body()

                        if (response.status == "ok" && response.model == "RC") {
                            Log.d("SCAN", "✅ Ditemukan: $host")
                            withContext(Dispatchers.Main) {
                                foundDevices = foundDevices + ConnectedDevice(
                                    ip = host,
                                    model = response.model,
                                    controllerId = response.controller_id ?: "N/A",
                                    camId = response.cam_id ?: "N/A"
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // Abaikan host yang tidak merespons
                    }

                    if (i % 10 == 0) { // update setiap 10 IP agar tidak lag
                        withContext(Dispatchers.Main) {
                            scanProgress = i / 254f
                        }
                    }
                }
            }

            scanStatusText = if (foundDevices.isEmpty())
                "Pemindaian selesai. Tidak ada alat yang ditemukan."
            else "Pemindaian selesai. ${foundDevices.size} alat ditemukan."

            isScanning = false
        }
    }

    // =======================================================
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Perangkat di Jaringan") },
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
                .padding(16.dp)
        ) {
            if (!hasAllPermissions) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aplikasi membutuhkan izin untuk memindai jaringan.", textAlign = TextAlign.Center)
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
                        LinearProgressIndicator(progress = scanProgress, modifier = Modifier.fillMaxWidth())
                        Text(scanStatusText, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Perangkat Ditemukan:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (foundDevices.isEmpty()) {
                        Text(if (isScanning) "Menunggu hasil..." else "Belum ada perangkat ditemukan.")
                    } else {
                        // ✅ Grid manual 3 kolom tanpa LazyVerticalGrid (aman & ringan)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            foundDevices.chunked(3).forEach { rowDevices ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowDevices.forEach { device ->
                                        DeviceListItemCard(
                                            device = device,
                                            isSelected = selectedDevice?.ip == device.ip,
                                            onClick = { selectedDevice = it }
                                        )
                                    }
                                }
                            }
                        }

                    }

                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (selectedDevice != null)
                            "✅ Terpilih: ${selectedDevice!!.ip}"
                        else "Belum ada perangkat dipilih",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        }
    }
}


// =====================================================================
// ⭐ PREVIEW UNTUK MELIHAT TAMPILAN CARD SECARA TERPISAH ⭐
// Buka panel "Split" atau "Design" di Android Studio untuk melihatnya.
// =====================================================================
@Preview(showBackground = true)
@Composable
fun DeviceListPreview() {
    EWiTankTheme {
        Row (
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Untuk Row, gunakan horizontalArrangement
        ){
            // Contoh tampilan Card dalam kondisi normal
            DeviceListItemCard(
                device = ConnectedDevice("192.168.1.10", "RC", "123456", "789012"),
                isSelected = false,
                onClick = {}
            )

            // Contoh tampilan Card dalam kondisi terpilih (selected)
            DeviceListItemCard(
                device = ConnectedDevice("192.168.1.11", "RC", "654321", "210987"),
                isSelected = true,
                onClick = {}
            )

        }
    }
}
