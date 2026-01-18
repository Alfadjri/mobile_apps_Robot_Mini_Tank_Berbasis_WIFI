package com.alfadjri28.e_witank.RTH

data class RthCommand(
    val channel: String,
    val action: String,
    val durationMs: Long,
    val timestamp: Long
)

