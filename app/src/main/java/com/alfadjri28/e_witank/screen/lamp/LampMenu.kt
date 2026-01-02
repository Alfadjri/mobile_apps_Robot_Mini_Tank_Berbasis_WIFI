package com.alfadjri28.e_witank.screen.lamp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LampMenu(
    camIp: String,
    lampViewModel: LampViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { lampViewModel.toggleLamp(camIp) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = "Lamp",
            tint = if (lampViewModel.isLampOn.value)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (lampViewModel.isLampOn.value)
                "Matikan Lampu"
            else
                "Nyalakan Lampu"
        )
    }
}
