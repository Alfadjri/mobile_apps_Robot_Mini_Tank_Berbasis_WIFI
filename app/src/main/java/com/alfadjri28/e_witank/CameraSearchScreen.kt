package com.alfadjri28.e_witank

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CameraSearchScreen(camId: String, camPassword: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Mencari Kamera dengan ID: $camId")
        // Di sini Anda akan menambahkan logika untuk memindai
        // dan menemukan IP kamera berdasarkan camId dan password
    }
}
    