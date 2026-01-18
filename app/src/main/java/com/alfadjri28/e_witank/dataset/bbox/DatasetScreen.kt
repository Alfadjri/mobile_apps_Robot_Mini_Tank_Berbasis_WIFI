package com.alfadjri28.e_witank.dataset.bbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfadjri28.e_witank.screen.WebStreamViewer

@Composable
fun DatasetScreen(
    camIp: String,
    viewModel: BoundingBoxViewModel = viewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 🎥 STREAM + OVERLAY
        WebStreamViewer(
            camIp = camIp,
            overlay = {
                BoundingBoxOverlay(
                    boxes = viewModel.boxes
                )
            }
        )

        // 🎛️ PANEL BAWAH
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding() // 🔥 INI KUNCINYA
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            // 🔤 LABEL SELECTOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LabelButton("manusia", viewModel)
                LabelButton("obstacle", viewModel)
            }

            // ➕ TAMBAH BOX DUMMY (TEST)
            Button(
                onClick = {
                    viewModel.startCapture()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (viewModel.isCapturing)
                        "Gambar Kotak di Layar"
                    else
                        "Ambil Gambar & Label"
                )
            }
        }
    }
}

@Composable
private fun LabelButton(
    label: String,
    viewModel: BoundingBoxViewModel
) {
    val selected = viewModel.currentLabel == label

    Button(
        onClick = { viewModel.currentLabel = label },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color.Red else Color.DarkGray
        )
    ) {
        Text(label)
    }
}
