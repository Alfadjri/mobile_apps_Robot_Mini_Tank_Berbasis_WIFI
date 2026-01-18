package com.alfadjri28.e_witank.screen.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alfadjri28.e_witank.RTH.RthExecutor
import com.alfadjri28.e_witank.model.ControlViewModel
import com.alfadjri28.e_witank.screen.PortraitTankControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RthExecutionScreen(
    navController: NavController,
    ip: String,
    controlViewModel: ControlViewModel
) {
    val rthExecutor = remember { RthExecutor(controlViewModel) }
    val rthRecorder = controlViewModel.rthRecorder

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RETURN TO HOME (DEV)",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                text = "RTH EXECUTION MODE",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )

            /* ================= RECORD BUTTON ================= */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = if (rthRecorder.isRecording())
                        "● RECORDING"
                    else
                        "○ NOT RECORDING",
                    color = if (rthRecorder.isRecording())
                        Color.Red
                    else
                        Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (rthRecorder.isRecording())
                        "STOP RECORD"
                    else
                        "START RECORD",
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (rthRecorder.isRecording()) {
                            rthRecorder.stopRecord()
                        } else {
                            rthRecorder.startRecord()
                        }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            /* ================= DEBUG LOG ================= */

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {

                Text(
                    "DEBUG LOG",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                rthRecorder
                    .getDebugLog()
                    .takeLast(12)
                    .forEach {
                        Text(
                            it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
            }

            Button(
                onClick = {
                    controlViewModel.stopRecordAndReturnHome(ip)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text(
                    "STOP RECORD & RETURN HOME",
                    fontWeight = FontWeight.Bold
                )
            }


            /* ================= TANK CONTROLS ================= */

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
