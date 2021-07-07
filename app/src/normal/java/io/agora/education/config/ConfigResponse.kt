package io.agora.education.config

data class ConfigResponse(
        val msg: String,
        val code: Int,
        val ts: Long,
        val data: ConfigData
)

data class ConfigData(
        val rtmToken: String,
        val appId: String,
        val userUuid: String
)