package com.alfadjri28.e_witank

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alfadjri28.e_witank.model.ConnectedDevice
import com.alfadjri28.e_witank.model.ControllerData
import com.alfadjri28.e_witank.model.EspResponse
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import com.alfadjri28.e_witank.screen.DeviceListItemCard
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storage = remember { LocalStorageControllerRC(context) }

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
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

    // 🔴 Pesan error khusus kalau tidak ada WiFi / hotspot
    var networkErrorMessage by remember { mutableStateOf<String?>(null) }

    // Ambil data yang sudah disimpan di local storage
    var savedControllers by remember { mutableStateOf(storage.getController()) }

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

    fun startNetworkScan() {
        if (isScanning) return

        coroutineScope.launch {
            isScanning = true
            foundDevices = emptyList()
            scanProgress = 0f
            scanStatusText = "Mendeteksi jaringan lokal..."
            networkErrorMessage = null   // reset error dulu

            val ipAddress = withContext(Dispatchers.IO) {
                try {
                    NetworkInterface.getNetworkInterfaces().toList()
                        .filter { it.isUp && !it.isLoopback }
                        .flatMap { it.inetAddresses.toList() }
                        .firstOrNull { addr ->
                            addr is Inet4Address && addr.isSiteLocalAddress
                        }
                        ?.hostAddress
                } catch (e: Exception) {
                    Log.e("NETWORK_SCAN", "Gagal mendeteksi IP", e)
                    null
                }
            }

            if (ipAddress == null) {
                // 🔴 Kondisi: tidak ada IP lokal → kemungkinan WiFi & hotspot tidak aktif
                networkErrorMessage = "Hotspot dan WiFi tidak terhubung.\n" +
                        "Silakan aktifkan salah satu dulu, lalu coba pindai lagi."
                scanStatusText = "Tidak dapat memulai pemindaian."
                isScanning = false
                return@launch
            }

            val subnet = ipAddress.substringBeforeLast('.')
            scanStatusText = "Memindai subnet: $subnet.x"

            val timeout = 400
            val maxParallel = 5
            val jobs = mutableListOf<Deferred<Unit>>()

            withContext(Dispatchers.IO) {
                for (i in 1..254) {
                    val host = "$subnet.$i"
                    if (host == ipAddress) continue

                    val job = async {
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(host, 80), timeout)
                            }

                            val response: EspResponse = client.submitForm(
                                url = "http://$host/",
                                formParameters = parameters { append("password", espPassword) }
                            ) {
                                timeout { requestTimeoutMillis = 1500 }
                            }.body()

                            if (response.status == "ok" && response.model == "RC") {
                                withContext(Dispatchers.Main) {
                                    foundDevices = foundDevices + ConnectedDevice(
                                        ip = host,
                                        model = response.model,
                                        controllerId = response.controller_id ?: "N/A",
                                        camId = response.cam_id ?: "N/A"
                                    )
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    jobs.add(job)
                    if (jobs.size >= maxParallel) {
                        jobs.awaitAll()
                        jobs.clear()
                        withContext(Dispatchers.Main) { scanProgress = i / 254f }
                    }
                    delay(50)
                }
                if (jobs.isNotEmpty()) jobs.awaitAll()
            }

            scanStatusText = if (foundDevices.isEmpty())
                "Tidak ada controller ditemukan."
            else
                "Selesai: ${foundDevices.size} controller ditemukan."

            isScanning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Perangkat di Jaringan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
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

                    // 🔴 Tampil pesan kalau WiFi / hotspot tidak aktif
                    if (networkErrorMessage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = networkErrorMessage!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

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
                            progress = scanProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            scanStatusText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            scanStatusText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Perangkat Ditemukan:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (foundDevices.isEmpty()) {
                        Text(
                            if (isScanning) "Menunggu hasil..." else "Belum ada perangkat ditemukan."
                        )
                    } else {
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
                                        val isSaved = savedControllers.any { it.controllerIP == device.ip }

                                        DeviceListItemCard(
                                            device = device,
                                            isSelected = isSaved,
                                            onClick = { d ->
                                                if (!isSaved) {
                                                    storage.saveController(
                                                        ControllerData(
                                                            model = d.model,
                                                            controllerIP = d.ip,
                                                            controllerID = d.controllerId,
                                                            camIP = null,
                                                            camID = d.camId
                                                        )
                                                    )
                                                    savedControllers = storage.getController()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

