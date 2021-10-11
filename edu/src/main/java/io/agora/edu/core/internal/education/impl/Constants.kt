package io.agora.edu.core.internal.education.impl

import io.agora.edu.core.internal.log.LogManager

class Constants {
    companion object {
        lateinit var APPID: String
        lateinit var AgoraLog: LogManager
        val rtcConfigKey: Array<String> = arrayOf("rtc", "mode", "privateParams", "android", "android-acs1_1_5")
    }
}