package com.alfadjri28.e_witank.cnn

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)
