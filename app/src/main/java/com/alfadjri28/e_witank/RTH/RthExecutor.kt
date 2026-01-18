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

            val hasBackward = motions.any {
                it.motion == RobotMotion.MUNDUR ||
                        it.motion == RobotMotion.MUNDUR_BELOK_KIRI ||
                        it.motion == RobotMotion.MUNDUR_BELOK_KANAN
            }

            // 🔥 CASE KAMU: TIDAK ADA MUNDUR
            if (!hasBackward) {
                rotate180(ip)
            }

            for (cmd in motions) {
                val playMotion =
                    if (!hasBackward) {
                        cmd.copy(motion = RthMapper.invert(cmd.motion))
                    } else {
                        cmd
                    }
                executeSameMotion(ip, playMotion)
            }

            if (!hasBackward) {
                rotate180(ip)
            }

            controlViewModel.stopBoth(ip)
        }
    }

    private suspend fun rotate180(ip: String) {
        // PUTAR DI TEMPAT
        controlViewModel.sendCommandSmooth(ip, "a", "maju")
        controlViewModel.sendCommandSmooth(ip, "b", "mundur")
        delay(600) // kalibrasi 180°

        controlViewModel.stopBoth(ip)
        delay(80)

        // 🔥 MICRO-FORWARD (ANTI OFFSET)
        controlViewModel.sendCommandSmooth(ip, "a", "maju")
        controlViewModel.sendCommandSmooth(ip, "b", "maju")
        delay(10)

        controlViewModel.stopBoth(ip)
        delay(120)
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


    suspend fun executeSingleStep(
        ip: String,
        motionCmd: RthMotionCommand
    ) {
        when (motionCmd.motion) {

            RobotMotion.MAJU -> {
                controlViewModel.sendCommandSmooth(ip, "a", "maju")
                controlViewModel.sendCommandSmooth(ip, "b", "maju")
            }

            RobotMotion.MUNDUR -> {
                controlViewModel.sendCommandSmooth(ip, "a", "mundur")
                controlViewModel.sendCommandSmooth(ip, "b", "mundur")
            }

            RobotMotion.MAJU_BELOK_KANAN -> {
                controlViewModel.sendCommandSmooth(ip, "a", "maju")
                controlViewModel.sendCommandSmooth(ip, "b", "stop")
            }

            RobotMotion.MAJU_BELOK_KIRI -> {
                controlViewModel.sendCommandSmooth(ip, "a", "stop")
                controlViewModel.sendCommandSmooth(ip, "b", "maju")
            }

            RobotMotion.MUNDUR_BELOK_KIRI -> {
                controlViewModel.sendCommandSmooth(ip, "a", "mundur")
                controlViewModel.sendCommandSmooth(ip, "b", "stop")
            }

            RobotMotion.MUNDUR_BELOK_KANAN -> {
                controlViewModel.sendCommandSmooth(ip, "a", "stop")
                controlViewModel.sendCommandSmooth(ip, "b", "mundur")
            }

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
    }

}
