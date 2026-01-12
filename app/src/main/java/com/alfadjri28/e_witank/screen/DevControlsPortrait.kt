package com.alfadjri28.e_witank.screen
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp


@Composable
fun DevLogCenter(
    controlViewModel: ControlViewModel
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DEV LOG",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = controlViewModel.lastDevInfo,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
@Composable
fun DevTankChannelControls(
    ip: String,
    channel: String,
    controlViewModel: ControlViewModel
) {
    val buttonSize = 56.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // MAJU
        HoldableIconButton(
            onPress = {
                controlViewModel.devPress(channel.uppercase(), "MAJU")
                controlViewModel.sendCommandSmooth(ip, channel, "maju")
            },
            onRelease = {
                controlViewModel.devRelease(channel.uppercase(), "MAJU")
                controlViewModel.sendCommandSmooth(ip, channel, "stop")
            }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Maju",
                modifier = Modifier
                    .size(buttonSize)
                    .rotate(90f)
            )
        }

        // MUNDUR
        HoldableIconButton(
            onPress = {
                controlViewModel.devPress(channel.uppercase(), "MUNDUR")
                controlViewModel.sendCommandSmooth(ip, channel, "mundur")
            },
            onRelease = {
                controlViewModel.devRelease(channel.uppercase(), "MUNDUR")
                controlViewModel.sendCommandSmooth(ip, channel, "stop")
            }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Mundur",
                modifier = Modifier
                    .size(buttonSize)
                    .rotate(-90f)
            )
        }
    }
}

@Composable
fun DevPortraitControls(
    ip: String,
    controlViewModel: ControlViewModel
) {
    if (!controlViewModel.isDevMode) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // KIRI (CHANNEL A)
        DevTankChannelControls(
            ip = ip,
            channel = "a",
            controlViewModel = controlViewModel
        )

        // TENGAH (LOG)
        DevLogCenter(controlViewModel)

        // KANAN (CHANNEL B)
        DevTankChannelControls(
            ip = ip,
            channel = "b",
            controlViewModel = controlViewModel
        )
    }
}


