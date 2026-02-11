package com.alfadjri28.e_witank.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

@Composable
fun WebStreamViewer(
    camIp: String,
    onStreamError: (Boolean) -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current

    val imageView = remember {
        android.widget.ImageView(context).apply {
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.BLACK)

            // 🔥 INI KUNCINYA
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> false }
        }
    }


    DisposableEffect(camIp) {

        val job = CoroutineScope(Dispatchers.IO).launch {

            while (isActive) {
                try {
                    startMjpegStreamStable(
                        camIp = camIp,
                        isActive = { isActive }
                    ) { bmp ->
                        withContext(Dispatchers.Main) {
                            onStreamError(false) // stream normal
                            imageView.setImageBitmap(bmp)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onStreamError(true) // stream error
                    }
                    delay(2000) // tunggu sebelum reconnect
                }
            }
        }

        onDispose { job.cancel() }
    }


    Box(modifier = Modifier.fillMaxSize()) {

        // 🎥 VIDEO (native)
        AndroidView(
            factory = { imageView },
            modifier = Modifier.fillMaxSize()
        )

        // 🔥 OVERLAY COMPOSE (lampu, distance, dll)
        overlay()
    }
}

/* ================= MJPEG CORE ================= */

private suspend fun startMjpegStreamStable(
    camIp: String,
    isActive: () -> Boolean,
    onFrame: suspend (Bitmap) -> Unit
) {
    val url = URL("http://$camIp:81/camp")
    val conn = url.openConnection() as HttpURLConnection

    conn.connectTimeout = 3000
    conn.readTimeout = 0
    conn.doInput = true
    conn.useCaches = false
    conn.connect()

    val input = BufferedInputStream(conn.inputStream, 64 * 1024)

    try {
        while (isActive()) {
            val boundary = readMjpegLine(input) ?: break
            if (!boundary.startsWith("--")) continue

            var contentLength = -1
            while (true) {
                val line = readMjpegLine(input) ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length")) {
                    contentLength = line.substringAfter(":").trim().toInt()
                }
            }

            if (contentLength <= 0) continue

            val jpeg = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val r = input.read(jpeg, read, contentLength - read)
                if (r < 0) break
                read += r
            }

            val bmp = BitmapFactory.decodeByteArray(
                jpeg, 0, jpeg.size,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )

            if (bmp != null) onFrame(bmp)
        }
    } finally {
        input.close()
        conn.disconnect()
    }
}


private fun readMjpegLine(input: InputStream): String? {
    val buffer = ByteArrayOutputStream()
    while (true) {
        val b = input.read()
        if (b == -1) {
            return if (buffer.size() > 0)
                buffer.toString(Charsets.US_ASCII.name())
            else null
        }
        if (b == '\n'.code) break
        if (b != '\r'.code) buffer.write(b)
    }
    return buffer.toString(Charsets.US_ASCII.name())
}



