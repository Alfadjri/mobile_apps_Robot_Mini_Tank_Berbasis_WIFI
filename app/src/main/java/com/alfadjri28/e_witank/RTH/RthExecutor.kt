package com.alfadjri28.e_witank.RTH

import com.alfadjri28.e_witank.model.ControlViewModel
import kotlinx.coroutines.*

class RthExecutor(
    private val controlViewModel: ControlViewModel
) {

    private var rthJob: Job? = null

    fun execute(ip: String, motions: List<RthMotionCommand>) {
        if (motions.isEmpty()) return

        rthJob?.cancel()

        rthJob = CoroutineScope(Dispatchers.Main).launch {

//            // ===============================
//            // 1️⃣ PUTAR TERUS (ORIENTASI RESET)
//            // ===============================
//            rotateContinuously(ip)

            // ===============================
            // 2️⃣ MAIN ULANG MOTION (TERBALIK)
            // ===============================
            for (motion in motions) {
                executeSameMotion(ip, motion)
            }
//            // ===============================
//            // 1️⃣ PUTAR TERUS (ORIENTASI AWAL)
//            // ===============================
//            rotateContinuously(ip)

            // ===============================
            // 3️⃣ STOP
            // ===============================
            controlViewModel.stopBoth(ip)
        }
    }

    private suspend fun rotateContinuously(ip: String) {
        // PUTAR KANAN DI TEMPAT
        controlViewModel.sendCommandSmooth(ip, "a", "maju")
        controlViewModel.sendCommandSmooth(ip, "b", "mundur")

        delay(300) // estimasi 180
    }

    private suspend fun executeSameMotion(
        ip: String,
        motionCmd: RthMotionCommand
    ) {
        when (motionCmd.motion) {

            // ===== LURUS =====
            RobotMotion.MAJU -> {
                controlViewModel.sendCommandSmooth(ip, "a", "maju")
                controlViewModel.sendCommandSmooth(ip, "b", "maju")
            }

            RobotMotion.MUNDUR -> {
                controlViewModel.sendCommandSmooth(ip, "a", "mundur")
                controlViewModel.sendCommandSmooth(ip, "b", "mundur")
            }

            // ===== BELOK MAJU =====
            RobotMotion.MAJU_BELOK_KANAN -> {
                // a/maju (SESUAI INPUT ASLI)
                controlViewModel.sendCommandSmooth(ip, "a", "maju")
                controlViewModel.sendCommandSmooth(ip, "b", "stop")
            }

            RobotMotion.MAJU_BELOK_KIRI -> {
                // b/maju
                controlViewModel.sendCommandSmooth(ip, "a", "stop")
                controlViewModel.sendCommandSmooth(ip, "b", "maju")
            }

            // ===== BELOK MUNDUR (INI YANG TADI SALAH) =====
            RobotMotion.MUNDUR_BELOK_KANAN -> {
                // b/mundur ❌ SALAH SEBELUMNYA
                controlViewModel.sendCommandSmooth(ip, "a", "stop")
                controlViewModel.sendCommandSmooth(ip, "b", "mundur")
            }

            RobotMotion.MUNDUR_BELOK_KIRI -> {
                // a/mundur ✅ BENAR
                controlViewModel.sendCommandSmooth(ip, "a", "mundur")
                controlViewModel.sendCommandSmooth(ip, "b", "stop")
            }

            // ===== PUTAR =====
            RobotMotion.PUTAR_KANAN -> {
                controlViewModel.sendCommandSmooth(ip, "a", "maju")
                controlViewModel.sendCommandSmooth(ip, "b", "mundur")
            }

            RobotMotion.PUTAR_KIRI -> {
                controlViewModel.sendCommandSmooth(ip, "a", "mundur")
                controlViewModel.sendCommandSmooth(ip, "b", "maju")
            }

            RobotMotion.DIAM -> return
        }

        delay(motionCmd.durationMs)
        controlViewModel.stopBoth(ip)
        delay(120)
    }
    fun stop() {
        rthJob?.cancel()
    }
}
