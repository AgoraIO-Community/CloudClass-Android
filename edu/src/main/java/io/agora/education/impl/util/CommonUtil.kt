package io.agora.education.impl.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.agora.education.api.room.data.RoomType
import io.agora.edu.BuildConfig

internal class CommonUtil {
    companion object {
        fun buildRtcOptionalInfo(tag: Int?): String {
            tag?.let {
                val info = JsonObject()
                info.addProperty("demo_ver", BuildConfig.VERSION_NAME)
                when (tag) {
                    RoomType.ONE_ON_ONE.value -> {
                        info.addProperty("demo_scenario", "One on One Classroom")
                    }
                    RoomType.SMALL_CLASS.value -> {
                        info.addProperty("demo_scenario", "Small Classroom")
                    }
                    RoomType.LARGE_CLASS.value -> {
                        info.addProperty("demo_scenario", "Lecture Hall")
                    }
                    RoomType.BREAKOUT_CLASS.value -> {
                        info.addProperty("demo_scenario", "Breakout Classroom")
                    }
                    RoomType.MEDIUM_CLASS.value -> {
                        info.addProperty("demo_scenario", "Intermediate Classroom")
                    }
                }
                return Gson().toJson(info)
            }
            return ""
        }
    }
}