package com.alfadjri28.e_witank.RTH

object RthMapper {

    fun invert(motion: RobotMotion): RobotMotion = when (motion) {
        RobotMotion.MAJU -> RobotMotion.MAJU
        RobotMotion.MUNDUR -> RobotMotion.MUNDUR

        RobotMotion.MAJU_BELOK_KIRI -> RobotMotion.MAJU_BELOK_KIRI
        RobotMotion.MAJU_BELOK_KANAN -> RobotMotion.MAJU_BELOK_KANAN

        RobotMotion.MUNDUR_BELOK_KIRI -> RobotMotion.MUNDUR_BELOK_KIRI
        RobotMotion.MUNDUR_BELOK_KANAN -> RobotMotion.MUNDUR_BELOK_KANAN

        RobotMotion.PUTAR_KIRI -> RobotMotion.PUTAR_KIRI
        RobotMotion.PUTAR_KANAN -> RobotMotion.PUTAR_KANAN

        RobotMotion.DIAM -> RobotMotion.DIAM
    }

}

