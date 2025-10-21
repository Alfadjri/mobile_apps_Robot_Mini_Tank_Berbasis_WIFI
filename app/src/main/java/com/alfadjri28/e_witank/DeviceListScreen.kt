package com.alfadjri28.e_witank
import android.Manifest

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader

data class ConnectedDevice(
    val ip: String,
    val mac: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen() {
    val context = LocalContext.current

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    )

    var hasAllPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val requestPermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasAllPermissions = permissions.values.all { it }
        }

    var devices by remember { mutableStateOf(listOf<ConnectedDevice>()) }

    // Fungsi untuk baca /proc/net/arp
    suspend fun getConnectedDevices(): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        val deviceList = mutableListOf<ConnectedDevice>()
        try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            br.readLine() // skip header
            while (br.readLine().also { line = it } != null) {
                val parts = line!!.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val ip = parts[0]
                    val mac = parts[3]
                    if (mac.matches("..:..:..:..:..:..".toRegex())) {
                        deviceList.add(ConnectedDevice(ip, mac))
                    }
                }
            }
            br.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        deviceList
    }

    // Auto refresh setiap 3 detik
    LaunchedEffect(hasAllPermissions) {
        if (hasAllPermissions) {
            while (true) {
                devices = getConnectedDevices()
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Daftar Perangkat Tersambung") })
        }
    ) { padding ->
        Box(
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
                        "Aplikasi membutuhkan izin lokasi & WiFi",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { requestPermissionsLauncher.launch(requiredPermissions) }) {
                        Text("Berikan Izin")
                    }
                }
            } else if (devices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Menunggu perangkat terhubung...")
                }
            } else {
                LazyColumn {
                    items(devices) { device ->
                        val macPrefix = device.mac.substring(0, 8).lowercase()
                        val isESP =
                            macPrefix.startsWith("dc:3f:50") || macPrefix.startsWith("69:31:04")

                        if (isESP) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(MaterialTheme.colorScheme.surface),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDDEEFF))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        "Connection Device 1",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text("IP: ${device.ip}")
                                    Text("MAC: ${device.mac}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        // TODO: Lakukan koneksi ke device di sini
                                    }) {
                                        Text("Hubungkan ke Device 1")
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Perangkat Lain")
                                    Text("IP: ${device.ip}")
                                    Text("MAC: ${device.mac}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
