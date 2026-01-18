package com.alfadjri28.e_witank.dataset.bbox

data class BoundingBox(
    val label: String,
    val x: Float,   // 0..1 (normalized)
    val y: Float,
    val w: Float,
    val h: Float
)
