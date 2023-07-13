package io.agora.education.config

data class ConfigResponse(
    val msg: String,
    val code: Int,
    val ts: Long,
    val data: ConfigData
)

data class ConfigData(
    val appId: String,
    val userUuid: String,
    val token: String
)
