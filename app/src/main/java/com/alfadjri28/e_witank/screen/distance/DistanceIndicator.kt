package com.alfadjri28.e_witank.screen.distance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun DistanceIndicator(distance: Int?) {
    if (distance == null) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Text(
            text = "$distance cm",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ProximityAlertIndicator(distance: Int?) {
    if (distance == null) return

    val (color, alpha) = when {
        distance <= 2 -> Color.Red to 0.9f
        distance <= 18 -> Color.Yellow to 0.7f
        else -> return
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .background(color.copy(alpha = alpha), shape = CircleShape)
    )
}


