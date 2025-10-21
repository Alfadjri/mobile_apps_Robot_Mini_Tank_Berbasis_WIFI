package com.alfadjri28.e_witank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(onGuideFinished: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .safeDrawingPadding()
    ) {
        // 1. KONTEN YANG BISA DI-SCROLL
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Panduan Setup RC E-WiTank",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- PERUBAHAN DI SINI ---
            // Langkah 1 diupdate dengan info URL
            SetupStep(
                stepNumber = "1",
                instruction = buildAnnotatedString {
                    append("Jika RC menyala, sambungkan WiFi HP Anda ke jaringan:\n\n")
                    append("Nama WiFi: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF90CAF9))) {
                        append("LutungKasarung")
                    }
                    append("\nPassword: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF90CAF9))) {
                        append("BijiHilangApaBijiKecepti?")
                    }
                    append("\n\nSetelah terhubung, buka browser dan akses URL:\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF90CAF9))) {
                        append("192.168.4.1")
                    }
                }
            )
            // --- AKHIR PERUBAHAN ---

            Spacer(modifier = Modifier.height(16.dp))

            // Langkah 2
            SetupStep(
                stepNumber = "2",
                instruction = buildAnnotatedString {
                    append("Setelah terhubung ke WiFi RC, aplikasi akan meminta Anda untuk memasukan data hostpot anda.")
                }
            )
        }

        // 2. TOMBOL YANG SELALU DI BAWAH
        Button(
            onClick = { onGuideFinished() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Mengerti & Lanjutkan",
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 16.sp
            )
        }
    }
}
@Composable
fun SetupStep(stepNumber: String, instruction: AnnotatedString) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNumber, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = instruction,
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
