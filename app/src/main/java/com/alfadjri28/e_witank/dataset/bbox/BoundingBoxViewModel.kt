package com.alfadjri28.e_witank.dataset.bbox

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class BoundingBoxViewModel : ViewModel() {

    var boxes by mutableStateOf<List<BoundingBox>>(emptyList())
        private set

    var currentLabel by mutableStateOf("manusia")

    // 🔥 MODE DATASET
    var isCapturing by mutableStateOf(false)

    fun startCapture() {
        boxes = emptyList()
        isCapturing = true
    }

    fun finishCapture() {
        isCapturing = false
    }

    fun addBox(box: BoundingBox) {
        boxes = boxes + box
    }
}

