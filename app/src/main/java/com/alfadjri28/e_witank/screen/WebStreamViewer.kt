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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun WebStreamViewer(
    camIp: String,
    rotationDegrees: Float = 0f   // 0f potret normal, 90f / -90f untuk koreksi arah
) {
    var errorText by remember { mutableStateOf<String?>(null) }
    var frameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val currentFrame by rememberUpdatedState(frameBitmap)



    // ⚠️ LaunchedEffect hanya pakai camIp, jangan pakai rotationDegrees
    LaunchedEffect(camIp) {
        frameBitmap = null
        errorText = null

        withContext(Dispatchers.IO) {
            val streamUrl = "http://$camIp:81/camp"

            var conn: HttpURLConnection? = null
            try {
                val url = URL(streamUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    doInput = true
                    useCaches = false
                    setRequestProperty("Connection", "Keep-Alive")
                }
                conn.connect()

                val code = conn.responseCode
                val ctype = conn.contentType

                if (code != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        errorText = "HTTP error: $code"
                    }
                    return@withContext
                }

                val input = BufferedInputStream(conn.inputStream, 16 * 1024)
                var frameCount = 0
                val targetFps = 15
                val frameIntervalMs = 1000L / targetFps
                var lastFrameTime = 0L

                while (isActive) {
                    val boundaryLine = readMjpegLine(input) ?: break
                    if (!boundaryLine.startsWith("--")) continue


                    var contentLength = -1
                    while (true) {
                        val headerLine = readMjpegLine(input) ?: return@withContext
                        if (headerLine.isEmpty()) break
                        val lower = headerLine.lowercase()
                        if (lower.startsWith("content-length:")) {
                            contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: -1
                        }
                    }

                    if (contentLength <= 0) {
                        Log.w("MJPEG", "Content-Length invalid: $contentLength")
                        continue
                    }

                    val imgBytes = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = input.read(imgBytes, readTotal, contentLength - readTotal)
                        if (r == -1) {
                            return@withContext
                        }
                        readTotal += r
                    }

                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val rawBmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size, opts)

                    // 🔥 ROTASI bitmap di sini, bukan di Compose
                    val bmp = rawBmp ?: continue

                    val now = System.currentTimeMillis()
                    if (now - lastFrameTime >= frameIntervalMs) {
                        lastFrameTime = now
                        withContext(Dispatchers.Main) {
                            frameBitmap = bmp
                        }
                    } else {
                        bmp.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("MJPEG", "Stream error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    errorText = "Gagal memuat stream: ${e.message}"
                }
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bmp = currentFrame

        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "MJPEG Stream",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotationDegrees
                    },
                contentScale = ContentScale.FillBounds   // 🔥 INI KUNCI UTAMA
            )
        }
    }
}

/**
 * Baca satu "line" MJPEG (diakhiri '\n'), balikin string tanpa \r\n
 * Return null kalau EOF.
 */
private fun readMjpegLine(input: InputStream): String? {
    val baos = ByteArrayOutputStream()
    while (true) {
        val b = input.read()
        if (b == -1) {
            return if (baos.size() > 0) baos.toString(Charsets.US_ASCII.name()) else null
        }
        if (b == '\n'.code) break
        if (b != '\r'.code) baos.write(b)
    }
    return baos.toString(Charsets.US_ASCII.name())
}

