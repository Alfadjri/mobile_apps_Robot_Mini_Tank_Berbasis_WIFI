package com.alfadjri28.e_witank

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alfadjri28.e_witank.ui.theme.EWiTankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EWiTankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // --- ROUTE NAVIGATION ---
                    NavHost(navController = navController, startDestination = "splash_guide") {
                        composable("splash_guide") {
                            SplashScreen(
                                onGuideFinished = {
                                    navController.navigate("home") {
                                        popUpTo("splash_guide") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(navController = navController)
                        }

                        // 👉 ROUTE BARU UNTUK DEVICE LIST
                        composable("device_list") {
                            DeviceListScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    // Hanya izin lokasi yang benar-benar runtime di Android 10
    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var hasAllPermissions by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAllPermissions = permissions.values.all { it }
    }

    // Cek izin pertama kali saat tampilan dimuat
    LaunchedEffect(Unit) {
        val granted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            hasAllPermissions = true
        }
    }

    // --- UI utama ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        if (!hasAllPermissions) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aplikasi memerlukan izin lokasi untuk mendeteksi hotspot Anda.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(requiredPermissions) }) {
                    Text("Berikan Izin")
                }
            }
        } else {
            // 👇 lanjutkan ke logika hotspot kamu di sini
            HotspotConnectionUI(navController)
        }
    }
}

@Composable
fun HotspotConnectionUI(navController: NavController) {
    val context = LocalContext.current
    var hotspotDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isHotspotActive by remember { mutableStateOf(false) }
    var espConnected by remember { mutableStateOf(false) }

    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager

    // Fungsi untuk cek apakah hotspot aktif
    fun checkHotspotStatus(): Boolean {
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fungsi untuk membaca device yang terhubung
    fun getConnectedDevices(): List<String> {
        val result = mutableListOf<String>()
        try {
            val arpFile = java.io.File("/proc/net/arp")
            if (arpFile.exists()) {
                arpFile.forEachLine { line ->
                    if (line.contains("esp", ignoreCase = true)) {
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 4) {
                            val ip = parts[0]
                            val mac = parts[3]
                            result.add("$ip ($mac)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // Jalankan setiap beberapa detik untuk update status
    LaunchedEffect(Unit) {
        while (true) {
            isHotspotActive = checkHotspotStatus()
            if (isHotspotActive) {
                val devices = getConnectedDevices()
                hotspotDevices = devices
                espConnected = devices.any {
                    it.contains("esp-dc3f50", ignoreCase = true) ||
                            it.contains("esp32-693104", ignoreCase = true)
                }
            } else {
                hotspotDevices = emptyList()
                espConnected = false
            }
            kotlinx.coroutines.delay(3000) // cek tiap 3 detik
        }
    }

    // UI tampilan
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isHotspotActive) "Hotspot Aktif" else "Hotspot Tidak Aktif",
            style = MaterialTheme.typography.headlineSmall,
            color = if (isHotspotActive) Color(0xFF2E7D32) else Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isHotspotActive) {
            Text("Perangkat Terhubung:", fontWeight = FontWeight.Bold)
            if (hotspotDevices.isEmpty()) {
                Text("Belum ada perangkat yang terhubung", color = Color.Gray)
            } else {
                hotspotDevices.forEach { device ->
                    Text("- $device", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (espConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Device ESP Terdeteksi",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // TODO: navigasi ke halaman kontrol atau koneksi
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Hubungkan Device 1", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            Text(
                "Aktifkan hotspot untuk memulai koneksi dengan alat Anda.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

