package com.alfadjri28.e_witank.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DevPortraitControls(
    onDatasetClick: () -> Unit,
    onRthClick: () -> Unit
) {
    // ⛔ hanya tampil kalau DEV MODE aktif
    // pengecekan isDevMode dilakukan di caller

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = onDatasetClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BOUNDING BOX")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRthClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("RETURN TO HOME")
        }
    }
}

