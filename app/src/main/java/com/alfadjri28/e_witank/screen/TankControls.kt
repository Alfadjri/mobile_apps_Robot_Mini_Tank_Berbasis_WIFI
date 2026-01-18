package com.alfadjri28.e_witank.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alfadjri28.e_witank.model.ControlViewModel


/* =========================================================
   1. COMMAND ENUM (SATU-SATUNYA TEMPAT STRING COMMAND)
   ========================================================= */


enum class TankCommand(
    val channel: String,
    val action: String
) {
    A_MAJU("a", "maju"),
    A_MUNDUR("a", "mundur"),
    B_MAJU("b", "maju"),
    B_MUNDUR("b", "mundur")
}

/* =========================================================
   2. CORE EXECUTOR (LOGIC + RTH TERPUSAT)
   ========================================================= */

private fun executeTankCommand(
    vm: ControlViewModel,
    ip: String,
    command: TankCommand,
    pressed: Boolean
) {
    if (pressed) {
        vm.rthRecorder.onPress(command.channel, command.action)
        vm.sendCommandSmooth(ip, command.channel, command.action)
    } else {
        vm.rthRecorder.onRelease(command.channel, command.action)
        vm.sendCommandSmooth(ip, command.channel, "stop")
    }
}

/* =========================================================
   3. HOLDABLE BUTTON (LOW LEVEL)
   ========================================================= */

@Composable
fun HoldableIconButton(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.pointerInput(Unit) {
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

/* =========================================================
   4. TANK CONTROL BUTTON (YANG DIPAKAI VIEW)
   ========================================================= */

@Composable
fun TankControlButton(
    ip: String,
    vm: ControlViewModel,
    command: TankCommand,
    content: @Composable () -> Unit
) {
    HoldableIconButton(
        onPress = { executeTankCommand(vm, ip, command, true) },
        onRelease = { executeTankCommand(vm, ip, command, false) }
    ) {
        content()
    }
}

/* =========================================================
   5. ICON HELPER (BIAR VIEW RAPI)
   ========================================================= */

@Composable
private fun ArrowIcon(
    size: Dp,
    bg: Color,
    rotation: Float
) {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .padding(16.dp)
            .rotate(rotation),
        tint = Color.White
    )
}

/* =========================================================
   6. FULLSCREEN CONTROLS (BERSIH)
   ========================================================= */

@Composable
fun FullscreenTankControls(
    ip: String,
    controlViewModel: ControlViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    val buttonSize = 72.dp
    val bg = Color.Black.copy(alpha = 0.35f)
    val smallBg = Color.Black.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .then(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                    Modifier.systemGestureExclusion()
                else Modifier
            )
    ) {

        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TankControlButton(ip, controlViewModel, TankCommand.A_MAJU) {
                ArrowIcon(buttonSize, bg, 90f)
            }
            TankControlButton(ip, controlViewModel, TankCommand.A_MUNDUR) {
                ArrowIcon(buttonSize, bg, -90f)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TankControlButton(ip, controlViewModel, TankCommand.B_MAJU) {
                ArrowIcon(buttonSize, bg, 90f)
            }
            TankControlButton(ip, controlViewModel, TankCommand.B_MUNDUR) {
                ArrowIcon(buttonSize, bg, -90f)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .zIndex(10f)
                .clip(RoundedCornerShape(12.dp))
                .background(smallBg)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
        }
    }
}

/* =========================================================
   7. PORTRAIT CONTROLS (BERSIH)
   ========================================================= */

@Composable
fun PortraitTankControls(
    ip: String,
    controlViewModel: ControlViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    val buttonSize = 64.dp
    val bg = Color.Black.copy(alpha = 0.25f)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = "Kontrol Tank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TankControlButton(ip, controlViewModel, TankCommand.A_MAJU) {
                    ArrowIcon(buttonSize, bg, 90f)
                }
                TankControlButton(ip, controlViewModel, TankCommand.A_MUNDUR) {
                    ArrowIcon(buttonSize, bg, -90f)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TankControlButton(ip, controlViewModel, TankCommand.B_MAJU) {
                    ArrowIcon(buttonSize, bg, 90f)
                }
                TankControlButton(ip, controlViewModel, TankCommand.B_MUNDUR) {
                    ArrowIcon(buttonSize, bg, -90f)
                }
            }
        }
    }
}
