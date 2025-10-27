package com.alfadjri28.e_witank.model

import kotlinx.serialization.Serializable

@Serializable
data class EspResponse(
    val status: String,
    val model: String? = null,
    val controller_id: String? = null,
    val cam_id: String? = null
)

data class ConnectedDevice(
    val ip: String,
    val model: String,
    val controllerId: String,
    val camId: String
)