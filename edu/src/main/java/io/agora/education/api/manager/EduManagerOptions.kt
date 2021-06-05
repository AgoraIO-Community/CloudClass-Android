package io.agora.education.api.manager

import android.content.Context
import io.agora.education.api.logger.LogLevel

data class EduManagerOptions(
        var context: Context?,
        val appId: String,
        val rtmToken: String,
        val userUuid: String,
        val userName: String
) {
    var logLevel: LogLevel = LogLevel.NONE
    var logFileDir: String? = null
}
