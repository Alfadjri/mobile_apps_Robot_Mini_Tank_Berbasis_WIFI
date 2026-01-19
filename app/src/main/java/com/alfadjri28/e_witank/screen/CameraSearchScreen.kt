    package com.alfadjri28.e_witank.screen

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import com.alfadjri28.e_witank.screen.distance.DistanceIndicator
import com.alfadjri28.e_witank.screen.lamp.LampIndicator
import com.alfadjri28.e_witank.screen.lamp.LampViewModel
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
import com.alfadjri28.e_witank.screen.distance.DistanceViewModel
import com.alfadjri28.e_witank.screen.distance.ProximityAlertIndicator
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.alfadjri28.e_witank.RTH.RthExecutor
import com.alfadjri28.e_witank.model.CameraViewModel
import com.alfadjri28.e_witank.model.ControlViewModel

    // ====================== MAIN SCREEN ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSearchAndStreamScreen(
        navController: NavController,
        ip: String,
        camID: String,
        cameraViewModel: CameraViewModel = viewModel(),
        controlViewModel: ControlViewModel = viewModel(),
        lampViewModel: LampViewModel = viewModel()
) {
    val context = LocalContext.current
    val storage = remember { LocalStorageControllerRC(context) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showLampMenu by remember { mutableStateOf(false) }
    val distanceViewModel: DistanceViewModel = viewModel()
    val haptic = LocalHapticFeedback.current
    val rthExecutor = remember {
            RthExecutor(
                controlViewModel = controlViewModel,
                distanceViewModel = distanceViewModel
            )
        }


        val client = remember {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 3000
            }
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    LaunchedEffect(camID) {
        storage.getCamIPFast(camID)?.let {
            cameraViewModel.foundCameraIp = it
            cameraViewModel.statusText = "⚡ Kamera dari cache"
        }
        cameraViewModel.startScan(ip, camID, storage, client)
    }

    val camIp = cameraViewModel.getActiveCamIp()

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Text(
                            if (camIp != null) "Live Camera Stream"
                            else "Pindai Kamera RC"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (camIp != null) {
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    if (isFullscreen)
                                        Icons.Default.FullscreenExit
                                    else
                                        Icons.Default.Fullscreen,
                                    null
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

            // ====================== LOADING ======================
            if (camIp == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(cameraViewModel.statusText)
                }
                return@Box
            }

            // ====================== STREAM ======================
            DisposableEffect(camIp) {
                lampViewModel.fetchLampStatus(camIp)
                distanceViewModel.startPolling(camIp)
                onDispose { distanceViewModel.stopPolling() }
            }

            var lastState by remember {
                mutableStateOf(DistanceViewModel.SafetyState.SAFE)
            }

            LaunchedEffect(distanceViewModel.safetyState.value) {
                val state = distanceViewModel.safetyState.value
                controlViewModel.blockForward =
                    distanceViewModel.safetyState.value == DistanceViewModel.SafetyState.DANGER


                when (state) {
                    DistanceViewModel.SafetyState.DANGER -> {
                        if (!controlViewModel.isLocked) {
                            controlViewModel.isLocked = true
                            controlViewModel.stopBoth(ip)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    DistanceViewModel.SafetyState.WARNING -> {
                        if (lastState != DistanceViewModel.SafetyState.WARNING) {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.TextHandleMove
                            )
                        }
                        controlViewModel.isLocked = false
                    }

                    DistanceViewModel.SafetyState.SAFE -> {
                        controlViewModel.isLocked = false
                    }
                }
                lastState = state
            }

            if (isFullscreen) {
                // ===== FULLSCREEN =====
                Box(modifier = Modifier.fillMaxSize()) {

                    WebStreamViewer(camIp = camIp)

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 180.dp)
                    ) {
                        LampIndicator(isOn = lampViewModel.isLampOn.value)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                    ) {
                        when (distanceViewModel.safetyState.value) {
                            DistanceViewModel.SafetyState.SAFE ->
                                DistanceIndicator(distanceViewModel.distanceCm.value)

                            else ->
                                ProximityAlertIndicator(
                                    distance = distanceViewModel.distanceCm.value,
                                    state = distanceViewModel.safetyState.value
                                )
                        }
                    }

                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.FullscreenExit, null)
                    }

                    FullscreenTankControls(
                        ip = ip,
                        controlViewModel = controlViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onMenuClick = { showLampMenu = true }
                    )
                }

            } else {
                // ===== NORMAL =====
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

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            LampIndicator(isOn = lampViewModel.isLampOn.value)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            when (distanceViewModel.safetyState.value) {
                                DistanceViewModel.SafetyState.SAFE ->
                                    DistanceIndicator(distanceViewModel.distanceCm.value)

                                else ->
                                    ProximityAlertIndicator(
                                        distance = distanceViewModel.distanceCm.value,
                                        state = distanceViewModel.safetyState.value
                                    )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (controlViewModel.isDevMode && camIp != null) {
                        DevPortraitControls(
                            onDatasetClick = {
                                navController.navigate("dataset/$camIp")
                            },
                            onRthClick = {
                                navController.navigate("rth/$ip/$camIp")
                            }

                        )
                    } else {
                        PortraitTankControls(
                            ip = ip,
                            controlViewModel = controlViewModel,
                            modifier = Modifier.fillMaxWidth(),
                            onMenuClick = { showLampMenu = true }
                        )
                    }

                }
            }
        }

        if (showLampMenu) {
            ModalBottomSheet(onDismissRequest = { showLampMenu = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Camera Menu", fontWeight = FontWeight.Bold)
                    Divider()
                    ListItem(
                        headlineContent = {
                            Text(
                                if (lampViewModel.isLampOn.value)
                                    "Matikan Lampu"
                                else
                                    "Hidupkan Lampu"
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Default.FlashlightOn, null)
                        },
                        modifier = Modifier.clickable {
                            lampViewModel.toggleLamp(camIp!!)
                            showLampMenu = false
                        }
                    )
                    Divider()
                    ListItem(
                        headlineContent = {
                            Text(
                                if (controlViewModel.isDevMode)
                                    "Developer Mode (ON)"
                                else
                                    "Developer Mode (OFF)"
                            )
                        },
                        supportingContent = {
                            if (controlViewModel.isDevMode) {
                                Text(
                                    controlViewModel.lastDevInfo,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.MoreVert, null)
                        },
                        modifier = Modifier.clickable {
                            controlViewModel.toggleDevMode()
                        }
                    )

                }
            }
        }
    }

    // ===== SYSTEM UI =====
    LaunchedEffect(isFullscreen) {
        val activity = context as Activity
        if (isFullscreen) {
            setLandscape(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController
                    ?.hide(android.view.WindowInsets.Type.systemBars())
            }
        } else {
            setPortrait(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController
                    ?.show(android.view.WindowInsets.Type.systemBars())
            }
        }
    }
}

