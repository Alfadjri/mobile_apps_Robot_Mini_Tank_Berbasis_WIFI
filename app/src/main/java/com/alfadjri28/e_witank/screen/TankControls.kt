package com.alfadjri28.e_witank.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * Tombol yang kirim onPress saat jari menyentuh,
 * dan onRelease saat jari diangkat / gesture selesai.
 */
@Composable
fun HoldableIconButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onPress()
                    try {
                        awaitRelease()
                    } finally {
                        onRelease()
                    }
                }
            )
        }
    ) {
        content()
    }
}

/**
 * Kontrol fullscreen (2 baris: A di atas, B di bawah)
 * + tombol menu (atas kanan) & exit fullscreen (bawah kanan)
 */
@Composable
fun FullscreenTankControls(
    ip: String,
    controlViewModel: ControlViewModel,
    modifier: Modifier = Modifier,
    onExitFullscreen: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val buttonSize = 72.dp
    val bg = Color.Black.copy(alpha = 0.35f)
    val smallButtonBg = Color.Black.copy(alpha = 0.45f)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 🔹 Baris ATAS = channel A
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ATAS KIRI = A / mundur
            HoldableIconButton(
                onPress = { controlViewModel.sendCommand(ip, "a", "mundur") },
                onRelease = { controlViewModel.sendCommand(ip, "a", "stop") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "A Mundur",
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(16.dp),
                    tint = Color.White
                )
            }

            // ATAS KANAN = A / maju
            HoldableIconButton(
                onPress = { controlViewModel.sendCommand(ip, "a", "maju") },
                onRelease = { controlViewModel.sendCommand(ip, "a", "stop") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "A Maju",
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(16.dp)
                        .rotate(180f), // style maju versi kamu
                    tint = Color.White
                )
            }
        }

        // 🔹 Baris BAWAH = channel B
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BAWAH KIRI = B / mundur
            HoldableIconButton(
                onPress = { controlViewModel.sendCommand(ip, "b", "mundur") },
                onRelease = { controlViewModel.sendCommand(ip, "b", "stop") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "B Mundur",
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(16.dp),
                    tint = Color.White
                )
            }

            // BAWAH KANAN = B / maju
            HoldableIconButton(
                onPress = { controlViewModel.sendCommand(ip, "b", "maju") },
                onRelease = { controlViewModel.sendCommand(ip, "b", "stop") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "B Maju",
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(16.dp)
                        .rotate(180f),
                    tint = Color.White
                )
            }
        }

        // 🔸 Tombol MENU di pojok kanan atas — bentuk agak kotak, warna sama
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(smallButtonBg)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = Color.White
            )
        }

        // 🔸 Tombol KELUAR FULLSCREEN di pojok kanan bawah — sama style
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(smallButtonBg)
                .clickable { onExitFullscreen() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = "Keluar Fullscreen",
                tint = Color.White
            )
        }
    }
}

/**
 * Kontrol non-fullscreen (potret) di bawah video.
 * 2 tombol kiri = channel "a"
 * 2 tombol kanan = channel "b"
 */
@Composable
fun PortraitTankControls(
    ip: String,
    controlViewModel: ControlViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    val buttonSize = 64.dp
    val bg = Color.Black.copy(alpha = 0.25f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Judul
        Text(
            text = "Kontrol Tank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Tombol menu (burger) — center di bawah tulisan
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Menu),
                contentDescription = "Menu",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔥 Tombol tank — diratakan sejajar posisi tombol menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),   // sekitar 0.5cm dari pinggir layar
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Kolom kiri = channel "a"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kiri - Atas (A maju)
                HoldableIconButton(
                    onPress = { controlViewModel.sendCommand(ip, "a", "maju") },
                    onRelease = { controlViewModel.sendCommand(ip, "a", "stop") }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "A Maju",
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(bg)
                            .padding(16.dp)
                            .rotate(90f),
                        tint = Color.White
                    )
                }

                // Kiri - Bawah (A mundur)
                HoldableIconButton(
                    onPress = { controlViewModel.sendCommand(ip, "a", "mundur") },
                    onRelease = { controlViewModel.sendCommand(ip, "a", "stop") }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "A Mundur",
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(bg)
                            .padding(16.dp)
                            .rotate(-90f),
                        tint = Color.White
                    )
                }
            }

            // Kolom kanan = channel "b"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kanan - Atas (B maju)
                HoldableIconButton(
                    onPress = { controlViewModel.sendCommand(ip, "b", "maju") },
                    onRelease = { controlViewModel.sendCommand(ip, "b", "stop") }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "B Maju",
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(bg)
                            .padding(16.dp)
                            .rotate(90f),
                        tint = Color.White
                    )
                }

                // Kanan - Bawah (B mundur)
                HoldableIconButton(
                    onPress = { controlViewModel.sendCommand(ip, "b", "mundur") },
                    onRelease = { controlViewModel.sendCommand(ip, "b", "stop") }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "B Mundur",
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(bg)
                            .padding(16.dp)
                            .rotate(-90f),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

