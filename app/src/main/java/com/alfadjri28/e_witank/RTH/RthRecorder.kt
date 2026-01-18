package com.alfadjri28.e_witank.RTH

import androidx.compose.runtime.mutableStateListOf

class RthRecorder {

    private val motionLog = mutableListOf<RthMotionCommand>()
    private val debugLog = mutableStateListOf<String>()

    private var isRecording = false
    private var isStopping = false

    private var lastMotion: RobotMotion = RobotMotion.DIAM
    private var motionStartTime = 0L

    private var motorA = "stop"
    private var motorB = "stop"

    // 🔥 INI KUNCI UTAMA
    private var lastChangedMotor: String? = null

    private val MOTION_MIN_DURATION_MS = 120L

    /* ================= RECORD CONTROL ================= */

    fun startRecord() {
        motionLog.clear()
        debugLog.clear()

        isRecording = true
        isStopping = false

        lastMotion = RobotMotion.DIAM
        motorA = "stop"
        motorB = "stop"
        lastChangedMotor = null

        log("🟢 RECORD START")
    }

    fun stopRecord() {
        isStopping = true
        closeMotion()
        isRecording = false
        log("⛔ RECORD STOP")
        isStopping = false
    }

    fun isRecording() = isRecording

    /* ================= INPUT ================= */

    fun onPress(channel: String, action: String) {
        if (!isRecording || isStopping) return

        lastChangedMotor = channel

        if (channel == "a") motorA = action
        if (channel == "b") motorB = action

        updateMotion()
    }

    fun onRelease(channel: String, action: String) {
        if (!isRecording || isStopping) return

        lastChangedMotor = channel

        if (channel == "a") motorA = "stop"
        if (channel == "b") motorB = "stop"

        updateMotion()
    }

    /* ================= CORE LOGIC ================= */

    private fun updateMotion() {
        val newMotion = when {
            // ===== MAJU / MUNDUR LURUS =====
            motorA == "maju" && motorB == "maju" ->
                RobotMotion.MAJU

            motorA == "mundur" && motorB == "mundur" ->
                RobotMotion.MUNDUR

            // ===== PUTAR DI TEMPAT =====
            motorA == "maju" && motorB == "mundur" ->
                RobotMotion.PUTAR_KANAN

            motorA == "mundur" && motorB == "maju" ->
                RobotMotion.PUTAR_KIRI

            // ===== MAJU SAMBIL BELOK =====
            motorA == "maju" && motorB == "stop" ->
                RobotMotion.MAJU_BELOK_KANAN

            motorB == "maju" && motorA == "stop" ->
                RobotMotion.MAJU_BELOK_KIRI

            // ===== MUNDUR SAMBIL BELOK =====
            motorA == "mundur" && motorB == "stop" ->
                RobotMotion.MUNDUR_BELOK_KIRI

            motorB == "mundur" && motorA == "stop" ->
                RobotMotion.MUNDUR_BELOK_KANAN

            else ->
                RobotMotion.DIAM
        }

        if (newMotion == lastMotion) return

        if (newMotion == RobotMotion.DIAM) {
            closeMotion()
            lastMotion = RobotMotion.DIAM
            return
        }

        closeMotion()
        lastMotion = newMotion
        motionStartTime = System.currentTimeMillis()
        log("▶ MOTION $newMotion")
    }


    private fun closeMotion() {
        if (lastMotion == RobotMotion.DIAM) return

        val duration = System.currentTimeMillis() - motionStartTime
        motionLog.add(RthMotionCommand(lastMotion, duration))
        log("■ END $lastMotion (${duration}ms)")
    }

    /* ================= GETTER ================= */

    fun getRecordedMotions(): List<RthMotionCommand> = motionLog.toList()
    fun getDebugLog(): List<String> = debugLog

    /* ================= DEBUG ================= */

    private fun log(msg: String) {
        debugLog.add("[${debugLog.size + 1}] $msg")
    }
}
