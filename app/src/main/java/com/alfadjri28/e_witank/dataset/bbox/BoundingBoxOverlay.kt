package com.alfadjri28.e_witank.dataset.bbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun BoundingBoxOverlay(
    boxes: List<BoundingBox>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        boxes.forEach {
            drawRect(
                color = Color.Red,
                topLeft = Offset(it.x * size.width, it.y * size.height),
                size = Size(it.w * size.width, it.h * size.height),
                style = Stroke(width = 3f)
            )
        }
    }
}
