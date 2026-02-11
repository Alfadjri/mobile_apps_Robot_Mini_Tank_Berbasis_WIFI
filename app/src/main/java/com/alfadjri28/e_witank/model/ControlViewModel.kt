package com.alfadjri28.e_witank.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfadjri28.e_witank.RTH.RthExecutor
import com.alfadjri28.e_witank.RTH.RthMotionCommand
import com.alfadjri28.e_witank.RTH.RthRecorder
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ====================== ViewModel Kontrol ======================
class ControlViewModel : ViewModel() {

    var isLocked by mutableStateOf(false)

    // 🔒 LOGIC SENSOR
    var blockForwardOnly by mutableStateOf(false)   // < 10cm
    var blockForwardHard by mutableStateOf(false) // < 5cm
    var slowMode by mutableStateOf(false)
    private var autoStopJob: Job? = null


    val rthRecorder = RthRecorder()



    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 1200 }
    }

    private var lastSendTime = 0L
    private var lastCommand = ""



    var isDevMode by mutableStateOf(false)
        private set

    var lastDevInfo by mutableStateOf("Belum ada input")

    private var pressStartTime = 0L



    /**
     * 🔥 COMMAND HALUS
     * - max 1 command / 120ms
     * - command sama tidak dikirim ulang
     */
    fun sendCommandSmooth(
        controllerIp: String,
        channel: String,
        command: String
    ) {
        // 🔴 GLOBAL LOCK (emergency total)
        if (isLocked && command != "stop") return

        // 🚨 HARD LOCK (<5cm)
        // semua command yang ada "maju" di dalamnya diblok
        if (blockForwardHard) {
            if (command.contains("maju")) return
        }

        // ⚠️ SOFT LOCK (<10cm)
        // hanya maju murni yang diblok
        if (blockForwardOnly) {
            if (command == "maju") return
        }

        val now = System.currentTimeMillis()
        val key = "$channel:$command"

        val interval = if (slowMode) 180L else 120L
        if (key == lastCommand && now - lastSendTime < interval) return

        lastCommand = key
        lastSendTime = now

        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.get("http://$controllerIp/$channel/$command")
            } catch (_: Exception) {}
        }
    }


    fun stopBoth(ip: String) {
        sendCommandSmooth(ip, "a", "stop")
        sendCommandSmooth(ip, "b", "stop")
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }


    fun autoStop(ip: String, delayMs: Long) {
        autoStopJob?.cancel()
        autoStopJob = viewModelScope.launch {
            delay(delayMs)
            sendCommandSmooth(ip, "a", "stop")
            sendCommandSmooth(ip, "b", "stop")
        }
    }


    fun toggleDevMode() {
        isDevMode = !isDevMode
    }

    fun devPress(channel: String, action: String) {
        pressStartTime = System.currentTimeMillis()
    }

    fun devRelease(channel: String, action: String) {
        val duration = System.currentTimeMillis() - pressStartTime
        lastDevInfo = "$channel - $action : ${duration} ms"
    }

    fun stopRecord(): List<RthMotionCommand> {
        rthRecorder.stopRecord()
        return rthRecorder.getRecordedMotions()
    }
}

