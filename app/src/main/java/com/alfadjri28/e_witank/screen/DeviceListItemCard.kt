package com.alfadjri28.e_witank.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alfadjri28.e_witank.model.ConnectedDevice

@Composable
fun DeviceListItemCard(
    device: ConnectedDevice,
    isSelected: Boolean,
    onClick: (ConnectedDevice) -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(150.dp)
            .combinedClickable(
                onClick = { onClick(device) },
                onLongClick = { onLongClick() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = Color(0xFFB9F6CA))
        else
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Ikon Mobil",
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF0D47A1)
            )
        }
    }
}
