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
fun WebStreamViewer(camIp: String) {
    var frameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(camIp) {
        frameBitmap = null
        errorText = null

        withContext(Dispatchers.IO) {
            val streamUrl = "http://$camIp:81/camp"
            Log.d("MJPEG", "Start MJPEG stream from $streamUrl")

            var conn: HttpURLConnection? = null
            try {
                val url = URL(streamUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    doInput = true
                    useCaches = false
                }
                conn.connect()

                val code = conn.responseCode
                val ctype = conn.contentType
                Log.d("MJPEG", "HTTP $code, contentType=$ctype")

                if (code != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        errorText = "HTTP error: $code"
                    }
                    return@withContext
                }

                val input = BufferedInputStream(conn.inputStream, 16 * 1024)

                var frameCount = 0

                while (isActive) {
                    // 1) Baca boundary, biasanya "--frame"
                    val boundaryLine = readMjpegLine(input) ?: break
                    if (!boundaryLine.startsWith("--")) {
                        // bisa jadi \r\n kosong, skip
                        continue
                    }
                    Log.d("MJPEG", "Boundary: $boundaryLine")

                    // 2) Baca header sampai baris kosong
                    var contentLength = -1
                    while (true) {
                        val headerLine = readMjpegLine(input) ?: return@withContext
                        if (headerLine.isEmpty()) {
                            break // end header
                        }
                        Log.d("MJPEG", "Header: $headerLine")
                        val lower = headerLine.lowercase()
                        if (lower.startsWith("content-length:")) {
                            contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: -1
                        }
                    }

                    if (contentLength <= 0) {
                        Log.w("MJPEG", "Content-Length invalid: $contentLength")
                        continue
                    }

                    // 3) Baca tepat Content-Length byte
                    val imgBytes = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = input.read(imgBytes, readTotal, contentLength - readTotal)
                        if (r == -1) {
                            Log.d("MJPEG", "EOF saat baca gambar")
                            return@withContext
                        }
                        readTotal += r
                    }
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    // 4) Decode JPEG
                    val bmp: Bitmap? = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size, opts)
                    if (bmp != null) {
                        frameCount++
                        if (frameCount % 3 == 0) { // ambil 1 dari 3 frame
                            withContext(Dispatchers.Main) {
                                frameBitmap = bmp
                            }
                        }
                    } else {
                        Log.w("MJPEG", "Gagal decode frame ke-$frameCount, size=${imgBytes.size}")
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
        val bmp = frameBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "MJPEG Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        errorText?.let { msg ->
            Log.d("MJPEG", "ErrorText: $msg")
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
        if (b == '\n'.code) {
            break
        }
        if (b != '\r'.code) {
            baos.write(b)
        }
    }
    return baos.toString(Charsets.US_ASCII.name())
}
