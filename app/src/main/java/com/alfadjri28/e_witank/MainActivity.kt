package com.alfadjri28.e_witank

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                            // Ini sekarang akan memanggil SplashScreen dari file SplashScreen.kt Anda
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

                        // Rute untuk layar pencarian dan koneksi perangkat
                        composable("device_list") {
                            DeviceListScreen(navController = navController) // Baris ini sudah benar
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

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

    // Cek izin saat tampilan pertama kali dimuat
    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // --- UI utama ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-WiTank Controller") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasAllPermissions) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Aplikasi memerlukan izin lokasi untuk mendeteksi hotspot dan perangkat.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(requiredPermissions) }) {
                        Text("Berikan Izin")
                    }
                }
            } else {
                // Lanjutkan ke UI utama jika izin sudah diberikan
                HotspotConnectionUI(navController)
            }
        }
    }
}

@Composable
fun HotspotConnectionUI(navController: NavController) {
    val context = LocalContext.current
    var isHotspotActive by remember { mutableStateOf(false) }

    // Dapatkan WifiManager dari context
    val wifiManager = remember(context) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    }

    // Fungsi untuk memeriksa status hotspot
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

    // LaunchedEffect untuk memeriksa status hotspot secara berkala
    LaunchedEffect(Unit) {
        while (true) {
            isHotspotActive = checkHotspotStatus()
            kotlinx.coroutines.delay(3000) // Cek setiap 3 detik
        }
    }

    // Tampilan UI utama
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tampilkan ikon dan status hotspot
        if (isHotspotActive) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Hotspot Aktif",
                tint = Color(0xFF2E7D32), // Warna hijau
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hotspot Aktif",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF2E7D32)
            )
        } else {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Hotspot Tidak Aktif",
                tint = Color.Red,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hotspot Tidak Aktif",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isHotspotActive) {
            Text(
                "Hotspot Anda siap untuk dihubungkan oleh alat.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Tombol untuk menavigasi ke layar pencarian perangkat
            Button(
                onClick = {
                    // Navigasi ke layar DeviceListScreen
                    navController.navigate("device_list")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Cari & Hubungkan Perangkat")
            }
        } else {
            Text(
                "Silakan aktifkan hotspot pada ponsel Anda untuk memulai koneksi dengan alat.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
