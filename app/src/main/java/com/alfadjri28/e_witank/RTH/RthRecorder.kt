package com.alfadjri28.e_witank.RTH

import androidx.compose.runtime.mutableStateListOf

class RthRecorder {

    private val motionLog = mutableStateListOf<RthMotionCommand>()
    private val debugLog = mutableStateListOf<String>()

    private var isRecording = false
    private var lastMotion: RobotMotion = RobotMotion.DIAM
    private var motionStartTime = 0L

    private var motorA = "stop"
    private var motorB = "stop"

    private val NOISE_THRESHOLD_MS = 80L

    /* ================= RECORD CONTROL ================= */

    fun startRecord() {
        motionLog.clear()
        debugLog.clear()

        isRecording = true
        motorA = "stop"
        motorB = "stop"
        lastMotion = RobotMotion.DIAM

        log("🟢 RECORD START")
    }

    fun stopRecord() {
        commitMotion()
        isRecording = false
        log("⛔ RECORD STOP")
    }

    fun isRecording() = isRecording

    /* ================= INPUT ================= */

    fun onPress(channel: String, action: String) {
        if (!isRecording) return

        if (channel == "a") motorA = action
        if (channel == "b") motorB = action

        resolveMotion()
    }

    fun onRelease(channel: String, action: String) {
        if (!isRecording) return

        if (channel == "a") motorA = "stop"
        if (channel == "b") motorB = "stop"

        resolveMotion()
    }

    /* ================= CORE ================= */

    private fun resolveMotion() {
        val newMotion = when {
            motorA == "maju" && motorB == "maju" -> RobotMotion.MAJU
            motorA == "mundur" && motorB == "mundur" -> RobotMotion.MUNDUR

            motorA == "maju" && motorB == "mundur" -> RobotMotion.PUTAR_KANAN
            motorA == "mundur" && motorB == "maju" -> RobotMotion.PUTAR_KIRI

            motorA == "maju" && motorB == "stop" -> RobotMotion.MAJU_BELOK_KANAN
            motorB == "maju" && motorA == "stop" -> RobotMotion.MAJU_BELOK_KIRI

            motorA == "mundur" && motorB == "stop" -> RobotMotion.MUNDUR_BELOK_KIRI
            motorB == "mundur" && motorA == "stop" -> RobotMotion.MUNDUR_BELOK_KANAN

            else -> RobotMotion.DIAM
        }

        if (newMotion == lastMotion) return

        commitMotion()

        if (newMotion != RobotMotion.DIAM) {
            lastMotion = newMotion
            motionStartTime = System.currentTimeMillis()
            log("▶ MOTION $newMotion")
        }
    }

    private fun commitMotion() {
        if (lastMotion == RobotMotion.DIAM) return

        val duration = System.currentTimeMillis() - motionStartTime
        if (duration >= NOISE_THRESHOLD_MS) {
            motionLog.add(RthMotionCommand(lastMotion, duration))
            log("■ END $lastMotion (${duration}ms)")
        } else {
            log("⚠ SKIP NOISE $lastMotion (${duration}ms)")
        }

        lastMotion = RobotMotion.DIAM
    }

    /* ================= GETTER ================= */

    fun getRecordedMotions(): List<RthMotionCommand> = motionLog
    fun getDebugLog(): List<String> = debugLog

    private fun log(msg: String) {
        debugLog.add("[${debugLog.size + 1}] $msg")
    }
}

