package com.alfadjri28.e_witank.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfadjri28.e_witank.logic.CameraScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    var foundCameraIp by mutableStateOf<String?>(null)
    var candidateIp by mutableStateOf<String?>(null)

    var isScanning by mutableStateOf(false)
    var statusText by mutableStateOf("Menunggu kamera...")

    fun startScan(
        ip: String,
        camID: String,
        storage: LocalStorageControllerRC,
        client: HttpClient
    ) {
        if (isScanning || foundCameraIp != null) return
        isScanning = true

        val scanner = CameraScanner(ip, camID, storage, client)

        viewModelScope.launch(Dispatchers.IO) {
            scanner.scan(this) { _, host ->
                if (host == null) return@scan

                // kandidat pertama → stream cepat
                if (candidateIp == null && foundCameraIp == null) {
                    candidateIp = host
                    statusText = "🔄 Menghubungkan ke kamera..."
                }

                // valid final (sekali saja)
                if (foundCameraIp == null) {
                    foundCameraIp = host
                    candidateIp = null
                    storage.saveCamIP(camID, host)
                    statusText = "✅ Kamera ditemukan di $host"
                    isScanning = false
                }
            }
        }
    }

    fun getActiveCamIp(): String? {
        return foundCameraIp ?: candidateIp
    }
}