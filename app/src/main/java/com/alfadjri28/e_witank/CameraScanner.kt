package com.alfadjri28.e_witank.logic

import android.util.Log
import com.alfadjri28.e_witank.model.ControllerData
import com.alfadjri28.e_witank.model.LocalStorageControllerRC
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.Serializable

@Serializable
data class CameraResponse(
    val status: String,
    val model: String? = null,
    val cam_id: String? = null
)

class CameraScanner(
    private val ip: String,
    private val camID: String,
    private val storage: LocalStorageControllerRC,
    private val client: HttpClient
) {
    var progress = 0f
        private set
    var statusText = "Menunggu pemindaian..."
        private set

    suspend fun scan(scope: CoroutineScope, onResult: (CameraResponse?, String?) -> Unit) {
        val subnet = ip.substringBeforeLast('.')
        val foundFlag = CompletableDeferred<Unit>()
        val semaphore = Semaphore(20)

        withContext(Dispatchers.IO) {
            val jobs = (1..254).map { i ->
                scope.async {
                    val host = "$subnet.$i"
                    if (host == ip) return@async

                    semaphore.acquire()
                    try {
                        if (foundFlag.isCompleted) return@async

                        val response: CameraResponse = client.submitForm(
                            url = "http://$host/",
                            formParameters = parameters { append("password", camID) }
                        ).body()

                        if (response.status == "ok" && response.model == "CAM") {
                            val foundCamId = response.cam_id ?: camID
                            val updatedController = ControllerData(
                                model = "RC",
                                controllerIP = ip,
                                controllerID = camID,
                                camIP = host,
                                camID = foundCamId
                            )
                            storage.updateController(camID, updatedController)
                            Log.d("CameraScanner", "📦 Kamera ditemukan: $updatedController")
                            foundFlag.complete(Unit)
                            onResult(response, host)
                        }
                    } catch (_: Exception) {
                        // Abaikan error koneksi
                    } finally {
                        progress += 1f / 254f
                        semaphore.release()
                    }
                }
            }

            try {
                jobs.awaitAll()
            } catch (_: CancellationException) { }

            if (!foundFlag.isCompleted) {
                onResult(null, null)
            }
        }
    }
}
