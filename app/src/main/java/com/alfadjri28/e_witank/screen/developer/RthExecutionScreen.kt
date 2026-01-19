package com.alfadjri28.e_witank.screen.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alfadjri28.e_witank.RTH.RthExecutor
import com.alfadjri28.e_witank.model.ControlViewModel
import com.alfadjri28.e_witank.screen.PortraitTankControls
import com.alfadjri28.e_witank.screen.distance.DistanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RthExecutionScreen(
    navController: NavController,
    ip: String,
    camID: String,
    controlViewModel: ControlViewModel,
    distanceViewModel: DistanceViewModel
) {

    /* ================= SENSOR LIFECYCLE ================= */

    LaunchedEffect(ip) {
        distanceViewModel.startPolling(camID)
    }

    DisposableEffect(Unit) {
        onDispose {
            distanceViewModel.stopPolling()
        }
    }

    /* ================= CORE ================= */

    val rthExecutor = remember {
        RthExecutor(
            controlViewModel = controlViewModel,
            distanceViewModel = distanceViewModel
        )
    }

    val rthRecorder = controlViewModel.rthRecorder
    val safetyState by distanceViewModel.safetyState
    val distance = distanceViewModel.distanceCm.value

    val recordedMotions = rthRecorder.getRecordedMotions()
    val scope = rememberCoroutineScope()

    var previewIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RETURN TO HOME (DEV)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        rthExecutor.stop()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            /* ================= STATUS ================= */

            Text(
                "RTH EXECUTION MODE",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )

            Text(
                text = "SENSOR: " +
                        (distance?.let { "ACTIVE ($it cm)" } ?: "INACTIVE"),
                color = if (distance != null) Color.Cyan else Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Text(
                text = "SAFETY: $safetyState",
                color = when (safetyState) {
                    DistanceViewModel.SafetyState.SAFE -> Color.Green
                    DistanceViewModel.SafetyState.WARNING -> Color.Yellow
                    DistanceViewModel.SafetyState.DANGER -> Color.Red
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(8.dp))

            /* ================= RECORD ================= */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    if (rthRecorder.isRecording()) "● RECORDING" else "○ NOT RECORDING",
                    color = if (rthRecorder.isRecording()) Color.Red else Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (rthRecorder.isRecording()) "STOP RECORD" else "START RECORD",
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (rthRecorder.isRecording())
                            rthRecorder.stopRecord()
                        else
                            rthRecorder.startRecord()
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            /* ================= STEP PREVIEW ================= */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.DarkGray.copy(alpha = 0.6f))
            ) {

                Text("RTH STEP PREVIEW", color = Color.Yellow, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(6.dp))

                if (recordedMotions.isEmpty()) {
                    Text("Belum ada data record", color = Color.Gray)
                } else {

                    val current = recordedMotions.getOrNull(previewIndex)
                    val next = recordedMotions.getOrNull(previewIndex + 1)

                    Text(
                        "STEP ${previewIndex + 1} / ${recordedMotions.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "▶ CURRENT : ${current?.motion} (${current?.durationMs} ms)",
                        color = Color.Cyan
                    )

                    Text(
                        "→ NEXT : ${next?.let { "${it.motion} (${it.durationMs} ms)" } ?: "END"}",
                        color = Color.LightGray
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Button(
                            onClick = { if (previewIndex > 0) previewIndex-- },
                            enabled = previewIndex > 0
                        ) { Text("PREV") }

                        Button(
                            enabled = safetyState != DistanceViewModel.SafetyState.DANGER &&
                                    previewIndex < recordedMotions.size,
                            onClick = {
                                val step = recordedMotions[previewIndex]
                                scope.launch {
                                    rthExecutor.executeSingleStep(ip, step)
                                }
                                if (previewIndex < recordedMotions.size - 1)
                                    previewIndex++
                            }
                        ) {
                            Text(
                                if (safetyState == DistanceViewModel.SafetyState.DANGER)
                                    "BLOCKED ⚠"
                                else
                                    "NEXT ▶ RUN"
                            )
                        }
                    }
                }
            }

            /* ================= DEBUG ================= */

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {

                Text("DEBUG LOG", color = Color.Green, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(6.dp))

                rthRecorder.getDebugLog().takeLast(12).forEach {
                    Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            /* ================= EXECUTE ================= */

            Button(
                onClick = {
                    val motions = controlViewModel.stopRecord()
                    if (motions.isNotEmpty()) {
                        rthExecutor.execute(ip, motions)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("STOP RECORD & RETURN HOME", fontWeight = FontWeight.Bold)
            }

            /* ================= MANUAL CONTROL ================= */

            PortraitTankControls(
                ip = ip,
                controlViewModel = controlViewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}
