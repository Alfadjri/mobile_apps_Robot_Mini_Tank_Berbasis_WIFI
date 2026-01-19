package com.alfadjri28.e_witank.screen.distance

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DistanceViewModel : ViewModel() {

    enum class SafetyState {
        SAFE, WARNING, DANGER
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1500 }
    }

    val distanceCm = mutableStateOf<Int?>(null)
    val safetyState = mutableStateOf(SafetyState.SAFE)

    // 🔥 SIMPAN DATA TERAKHIR YANG VALID
    private var lastValidDistance: Int? = null

    private var pollingJob: Job? = null

    fun isDanger(): Boolean =
        safetyState.value == SafetyState.DANGER

    fun isWarning(): Boolean =
        safetyState.value == SafetyState.WARNING

    fun startPolling(camIp: String) {
        if (pollingJob != null) return

        pollingJob = viewModelScope.launch {
            while (isActive) {

                var currentDistance: Int? = null

                try {
                    val response =
                        client.get("http://$camIp/distance").bodyAsText()

                    currentDistance = Regex("\"distance\":(\\d+)")
                        .find(response)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt()

                } catch (_: Exception) {
                    // timeout / parsing error → currentDistance tetap null
                }

                when {
                    // ✅ DATA NORMAL
                    currentDistance != null -> {
                        lastValidDistance = currentDistance
                        distanceCm.value = currentDistance

                        safetyState.value = when {
                            currentDistance <= 18 -> SafetyState.DANGER
                            currentDistance <= 25 -> SafetyState.WARNING
                            else -> SafetyState.SAFE
                        }
                    }

                    // 🚨 SENSOR HILANG SETELAH PERNAH AKTIF
                    lastValidDistance != null -> {
                        distanceCm.value = null
                        safetyState.value = SafetyState.DANGER
                    }

                    // 💤 SENSOR BELUM READY
                    else -> {
                        distanceCm.value = null
                        safetyState.value = SafetyState.SAFE
                    }
                }

                delay(80)
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
