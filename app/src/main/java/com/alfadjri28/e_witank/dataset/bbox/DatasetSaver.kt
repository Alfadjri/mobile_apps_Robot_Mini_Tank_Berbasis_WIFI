package com.alfadjri28.e_witank.dataset.bbox

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object DatasetSaver {

    fun saveImage(
        context: Context,
        bitmap: Bitmap,
        name: String
    ) {
        val dir = File(context.getExternalFilesDir(null), "dataset")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$name.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }
}
