package com.alfadjri28.e_witank.RTH

import com.alfadjri28.e_witank.model.ControlViewModel
import com.alfadjri28.e_witank.screen.distance.DistanceViewModel
import kotlinx.coroutines.*

class RthExecutor(
    private val controlViewModel: ControlViewModel,
    private val distanceViewModel: DistanceViewModel
) {

    private var rthJob: Job? = null
    private val SAFETY_STEP_MS = 50L
    private val WARNING_CM = 10

    // 🔥 waktu maju yang terpotong karena danger
    private var timeDebtMs: Long = 0L

    fun execute(ip: String, motions: List<RthMotionCommand>) {
        if (motions.isEmpty()) return

        rthJob?.cancel()
        timeDebtMs = 0L

        rthJob = CoroutineScope(Dispatchers.Default).launch {

            val hasBackward = motions.any { it.motion.isBackward() }

            if (!hasBackward) rotate180(ip)

            for (cmd in motions) {

                // ⏱️ kompensasi durasi
                val adjustedDuration =
                    (cmd.durationMs - timeDebtMs).coerceAtLeast(0L)

                timeDebtMs = 0L

                runMotionWithRecovery(
                    ip = ip,
                    motion = if (!hasBackward) RthMapper.invert(cmd.motion) else cmd.motion,
                    durationMs = adjustedDuration
                )
            }

            if (!hasBackward) rotate180(ip)

            controlViewModel.stopBoth(ip)
        }
    }

    suspend fun executeSingleStep(ip: String, cmd: RthMotionCommand) {
        runMotionWithRecovery(ip, cmd.motion, cmd.durationMs)
    }

    fun stop() {
        rthJob?.cancel()
        controlViewModel.stopBoth("")
    }

    /* ================= CORE ================= */

    private suspend fun runMotionWithRecovery(
        ip: String,
        motion: RobotMotion,
        durationMs: Long
    ) {
        sendMotion(ip, motion)

        var elapsed = 0L

        while (elapsed < durationMs) {

            if (motion.isForward() && distanceViewModel.isDanger()) {
                // 🔥 simpan sisa waktu maju
                timeDebtMs = durationMs - elapsed
                recoverFromDanger(ip)
                return
            }

            delay(SAFETY_STEP_MS)
            elapsed += SAFETY_STEP_MS
        }

        controlViewModel.stopBoth(ip)
        delay(120)
    }

    /* ================= RECOVERY ================= */

    private suspend fun recoverFromDanger(ip: String) {
        controlViewModel.stopBoth(ip)
        delay(60)

        var backElapsed = 0L

        while (
            distanceViewModel.distanceCm.value?.let { it <= WARNING_CM } == true &&
            backElapsed < timeDebtMs
        ) {
            controlViewModel.sendCommandSmooth(ip, "a", "mundur")
            controlViewModel.sendCommandSmooth(ip, "b", "mundur")

            delay(60)
            backElapsed += 60
        }

        // 🔥 hitung sisa debt
        timeDebtMs = (timeDebtMs - backElapsed).coerceAtLeast(0L)

        controlViewModel.stopBoth(ip)
        delay(120)
    }

    /* ================= ROTATE ================= */

    private suspend fun rotate180(ip: String) {
        sendMotion(ip, RobotMotion.PUTAR_KANAN)
        delay(600)

        controlViewModel.stopBoth(ip)
        delay(80)

        sendMotion(ip, RobotMotion.MAJU)
        delay(10)

        controlViewModel.stopBoth(ip)
        delay(120)
    }

    /* ================= MOTOR MAP ================= */

    private fun sendMotion(ip: String, motion: RobotMotion) {
        when (motion) {
            RobotMotion.MAJU -> drive(ip, "maju", "maju")
            RobotMotion.MUNDUR -> drive(ip, "mundur", "mundur")

            RobotMotion.MAJU_BELOK_KANAN -> drive(ip, "maju", "stop")
            RobotMotion.MAJU_BELOK_KIRI -> drive(ip, "stop", "maju")

            RobotMotion.MUNDUR_BELOK_KIRI -> drive(ip, "mundur", "stop")
            RobotMotion.MUNDUR_BELOK_KANAN -> drive(ip, "stop", "mundur")

            RobotMotion.PUTAR_KANAN -> drive(ip, "maju", "mundur")
            RobotMotion.PUTAR_KIRI -> drive(ip, "mundur", "maju")

            RobotMotion.DIAM -> controlViewModel.stopBoth(ip)
        }
    }

    private fun drive(ip: String, a: String, b: String) {
        controlViewModel.sendCommandSmooth(ip, "a", a)
        controlViewModel.sendCommandSmooth(ip, "b", b)
    }
}

/* ================= EXTENSION ================= */

private fun RobotMotion.isBackward(): Boolean =
    this == RobotMotion.MUNDUR ||
            this == RobotMotion.MUNDUR_BELOK_KIRI ||
            this == RobotMotion.MUNDUR_BELOK_KANAN

private fun RobotMotion.isForward(): Boolean =
    this == RobotMotion.MAJU ||
            this == RobotMotion.MAJU_BELOK_KANAN ||
            this == RobotMotion.MAJU_BELOK_KIRI
