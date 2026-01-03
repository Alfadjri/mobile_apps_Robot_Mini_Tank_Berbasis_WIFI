package com.alfadjri28.e_witank.screen.distance


import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DistanceViewModel : ViewModel() {

    enum class SafetyState {
        SAFE,
        WARNING,
        DANGER
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1500 }
    }

    val distanceCm = mutableStateOf<Int?>(null)
    val safetyState = mutableStateOf(SafetyState.SAFE)

    private var pollingJob: Job? = null

    fun startPolling(camIp: String) {
        if (pollingJob != null) return

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val response =
                        client.get("http://$camIp/distance").bodyAsText()

                    val value = Regex("\"distance\":(\\d+)")
                        .find(response)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()

                    withContext(Dispatchers.Main) {
                        distanceCm.value = value

                        safetyState.value = when {
                            value == null -> SafetyState.SAFE
                            value <= 2 -> SafetyState.DANGER
                            value <= 18 -> SafetyState.WARNING
                            else -> SafetyState.SAFE
                        }
                    }
                } catch (_: Exception) {}

                delay(300)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        stopPolling()
        client.close()
        super.onCleared()
    }
}



