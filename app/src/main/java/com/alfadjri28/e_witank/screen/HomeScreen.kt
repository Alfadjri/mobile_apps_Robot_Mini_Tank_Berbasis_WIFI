package com.alfadjri28.e_witank.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.alfadjri28.e_witank.model.ControllerData
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import com.alfadjri28.e_witank.screen.DeviceListItemCard
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.alfadjri28.e_witank.model.ConnectedDevice
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

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

    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

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
                PermissionRequestUI(
                    onRequestPermission = { permissionLauncher.launch(requiredPermissions) }
                )
            } else {
                HotspotConnectionUI(navController)
            }
        }
    }
}

@Composable
private fun PermissionRequestUI(onRequestPermission: () -> Unit) {
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
        Button(onClick = onRequestPermission) {
            Text("Berikan Izin")
        }
    }
}
fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo
        @Suppress("DEPRECATION")
        info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
    }
}

@Composable
private fun HotspotConnectionUI(navController: NavController) {
    val context = LocalContext.current
    val storage = remember { LocalStorageControllerRC(context) }

    var isHotspotActive by remember { mutableStateOf(false) }
    var isWifiConnectedNow by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf<List<ControllerData>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<ControllerData?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<ControllerData?>(null) }


    val wifiManager = remember(context) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    }

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

    // 🔄 Pantau status jaringan
    LaunchedEffect(Unit) {
        while (true) {
            val hotspotStatus = checkHotspotStatus()
            val wifiStatus = isWifiConnected(context)
            val currentDeviceList = storage.getController()

            isHotspotActive = hotspotStatus
            isWifiConnectedNow = wifiStatus

            val networkReady = hotspotStatus || wifiStatus

            if (!networkReady) {
                // Tidak ada hotspot & tidak ada WiFi → kosongkan list
                storage.clearAll()
                deviceList = emptyList()
                Log.d("Network", "❌ Tidak ada WiFi/Hotspot — data dihapus")
            } else {
                deviceList = currentDeviceList
            }

            delay(3000)
        }
    }

    // 🧹 Clear otomatis saat aplikasi terminate
    val lifecycleOwner = remember { ProcessLifecycleOwner.get() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                storage.clearAll()
                Log.d("LocalStorageControllerRC", "🧹 App terminate — semua data dihapus")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🖼️ UI
    val networkReady = isHotspotActive || isWifiConnectedNow

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 🔴 Tidak ada WiFi & tidak ada hotspot
            !networkReady -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Tidak ada koneksi",
                        tint = Color.Red,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hotspot dan WiFi tidak terhubung",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 🟢 Ada jaringan (WiFi / Hotspot) tapi belum ada device tersimpan
            networkReady && deviceList.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Jaringan Aktif",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Jaringan aktif — cari perangkat sekarang",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 🟢 Ada jaringan & ada device tersimpan
            networkReady && deviceList.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(deviceList.size) { index ->
                        val item = deviceList[index]
                        DeviceListItemCard(
                            device = ConnectedDevice(
                                ip = item.controllerIP ?: "Unknown",
                                model = item.model ?: "Unknown",
                                controllerId = item.controllerID ?: "Unknown",
                                camId = item.camID ?: "Unknown"
                            ),
                            isSelected = selectedDevice?.controllerID == item.controllerID,
                            onClick = { selected ->
                                selectedDevice = item
                                navController.navigate(
                                    "camera_search/${item.controllerIP}/${item.camID}"
                                )
                            },
                            onLongClick = {
                                deviceToDelete = item
                                showDeleteDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // 🔹 FAB: bisa muncul kalau ada jaringan aktif (WiFi atau Hotspot)
        if (networkReady) {
            FloatingActionButton(
                onClick = { navController.navigate("device_list") },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Cari & Hubungkan Perangkat",
                    tint = Color.White
                )
            }
        }

        if (showDeleteDialog && deviceToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    deviceToDelete = null
                },
                title = {
                    Text("Hapus Perangkat")
                },
                text = {
                    Text(
                        "Yakin ingin menghapus perangkat dengan IP:\n\n${deviceToDelete!!.controllerIP} ?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            storage.deleteControllerByIP(deviceToDelete!!.controllerIP)
                            deviceList = storage.getController()
                            showDeleteDialog = false
                            deviceToDelete = null
                        }
                    ) {
                        Text("Hapus", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            deviceToDelete = null
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

