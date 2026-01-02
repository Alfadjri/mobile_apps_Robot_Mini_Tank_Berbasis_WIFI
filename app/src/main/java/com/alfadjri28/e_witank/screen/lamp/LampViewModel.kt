package com.alfadjri28.e_witank.screen.lamp

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LampViewModel : ViewModel() {

    var isLampOn = mutableStateOf(false)
        private set

    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1500 }
    }

    fun toggleLamp(camIp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = if (isLampOn.value) "/lamp/off" else "/lamp/on"
                client.get("http://$camIp$path")
                isLampOn.value = !isLampOn.value
            } catch (e: Exception) {
                Log.e("LAMP", "Lamp error: ${e.message}")
            }
        }
    }
}
