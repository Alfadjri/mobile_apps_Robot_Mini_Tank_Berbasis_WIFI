package com.alfadjri28.e_witank.screen.lamp

import android.R.attr.delay
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LampViewModel : ViewModel() {

    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 2000 }
    }

    val isLampOn = mutableStateOf(false)
    val isLoading = mutableStateOf(false)

    /** 🔁 Ambil status lampu dari ESP32-CAM */
    fun fetchLampStatus(camIp: String) {
        viewModelScope.launch {
            try {
                val response =
                    client.get("http://$camIp/lamp/status").bodyAsText()

                // ESP32-CAM kirim: {"lamp":"on"} / {"lamp":"off"}
                isLampOn.value = response.contains("\"lamp\":\"on\"")

            } catch (e: Exception) {
                Log.e("LAMP", "Gagal ambil status lampu: ${e.message}")
            }
        }
    }


    /** 🔘 Toggle lamp */
    fun toggleLamp(camIp: String) {
        viewModelScope.launch {
            try {
                if (isLampOn.value) {
                    client.get("http://$camIp/lamp/off")
                } else {
                    client.get("http://$camIp/lamp/on")
                }

                delay(150) // ⏱️ kasih waktu ESP update
                fetchLampStatus(camIp) // 🔥 WAJIB
            } catch (_: Exception) {}
        }
    }

}

