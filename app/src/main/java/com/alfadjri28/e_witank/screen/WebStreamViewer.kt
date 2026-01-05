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
    rotationDegrees: Float = 0f
) {
    var frameBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(camIp) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            startMjpegStreamStable(camIp) { bmp ->
                // ⬇️ JANGAN pakai withContext di sini
                launch(Dispatchers.Main) {
                    frameBitmap?.recycle()
                    frameBitmap = bmp
                }
            }
        }

        onDispose {
            job.cancel()
            frameBitmap?.recycle()
            frameBitmap = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        frameBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotationDegrees },
                contentScale = ContentScale.FillBounds
            )
        }
    }
}




/* ================= MJPEG CORE ================= */

private suspend fun startMjpegStreamStable(
    camIp: String,
    onFrame: (Bitmap) -> Unit
) {
    val conn = (URL("http://$camIp:81/camp")
        .openConnection() as HttpURLConnection).apply {
        connectTimeout = 3000
        readTimeout = 0
        doInput = true
        useCaches = false
    }
    conn.connect()

    val input = BufferedInputStream(conn.inputStream, 32 * 1024)

    while (true) {
        val boundary = readMjpegLine(input) ?: break
        if (!boundary.startsWith("--")) continue

        var contentLength = -1
        while (true) {
            val line = readMjpegLine(input) ?: return
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
            if (r == -1) return
            read += r
        }

        val bmp = BitmapFactory.decodeByteArray(
            jpeg, 0, jpeg.size,
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: continue

        onFrame(bmp)
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



