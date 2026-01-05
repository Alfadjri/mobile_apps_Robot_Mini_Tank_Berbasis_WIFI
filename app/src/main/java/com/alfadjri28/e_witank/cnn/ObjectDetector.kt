package com.alfadjri28.e_witank.cnn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val model = FileUtil.loadMappedFile(context, "detect.tflite")
        interpreter = Interpreter(model)
        labels = FileUtil.loadLabels(context, "labelmap.txt")
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val input = preprocess(bitmap)

        val outputLocations = Array(1) { Array(10) { FloatArray(4) } }
        val outputClasses = Array(1) { FloatArray(10) }
        val outputScores = Array(1) { FloatArray(10) }
        val numDetections = FloatArray(1)

        interpreter.runForMultipleInputsOutputs(
            arrayOf(input),
            mapOf(
                0 to outputLocations,
                1 to outputClasses,
                2 to outputScores,
                3 to numDetections
            )
        )

        val results = mutableListOf<DetectionResult>()

        for (i in 0 until numDetections[0].toInt()) {
            val score = outputScores[0][i]
            if (score < 0.5f) continue

            val labelIndex = outputClasses[0][i].toInt()
            val label = labels.getOrElse(labelIndex) { "unknown" }
            val box = outputLocations[0][i]

            results.add(
                DetectionResult(
                    label = label,
                    confidence = score,
                    boundingBox = RectF(
                        box[1], // left
                        box[0], // top
                        box[3], // right
                        box[2]  // bottom
                    )
                )
            )
        }
        return results
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val inputSize = 300   // SESUAI MODEL SSD / DETECT
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val buffer =
            ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
                .order(ByteOrder.nativeOrder())

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)

                buffer.putFloat(((pixel shr 16 and 0xFF) / 255f))
                buffer.putFloat(((pixel shr 8 and 0xFF) / 255f))
                buffer.putFloat(((pixel and 0xFF) / 255f))
            }
        }
        return buffer
    }
}
