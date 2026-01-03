package com.alfadjri28.e_witank.screen.distance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProximityAlertIndicator(
    distance: Int?,
    state: DistanceViewModel.SafetyState
) {
    if (distance == null) return
    if (state == DistanceViewModel.SafetyState.SAFE) return

    val (bgColor, iconTint) = when (state) {
        DistanceViewModel.SafetyState.DANGER ->
            Color.Red.copy(alpha = 0.75f) to Color.White

        DistanceViewModel.SafetyState.WARNING ->
            Color(0xFFFFC107).copy(alpha = 0.75f) to Color.Black

        else -> return
    }

    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = "$distance cm",
            color = iconTint,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

