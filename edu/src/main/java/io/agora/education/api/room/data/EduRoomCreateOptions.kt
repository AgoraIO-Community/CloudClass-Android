package io.agora.education.api.room.data

import java.io.Serializable

/**超级小班课中会包含一个大班和N个小班*/
enum class ClassType(var value: Int) {
    Main(0),
    Sub(1)
}

enum class RoomType(var value: Int) {
    ONE_ON_ONE(0),

    // The old version of medium class
    SMALL_CLASS(4),
    LARGE_CLASS(2),
    BREAKOUT_CLASS_OBSOLETE(3),

    // The old version of small class
    MEDIUM_CLASS_OBSOLETE(1);

    companion object {
        fun roomTypeIsValid(value: Int): Boolean {
            return value == ONE_ON_ONE.value ||
                    value == SMALL_CLASS.value ||
                    value == LARGE_CLASS.value ||
                    value == BREAKOUT_CLASS_OBSOLETE.value ||
                    value == MEDIUM_CLASS_OBSOLETE.value
        }
    }
}

data class Property(
        val key: String,
        val value: String
) : Serializable {
    companion object {
        /**保留字段，请勿在业务中使用*/
        const val CAUSE = "cause"
        const val KEY_TEACHER_LIMIT = "TeacherLimit"
        const val KEY_STUDENT_LIMIT = "StudentLimit"
        const val KEY_ASSISTANT_LIMIT = "AssistantLimit"
    }
}


class RoomCreateOptions(
        var roomUuid: String,
        var roomName: String,
        val roomType: Int
) {
    val roomProperties: MutableMap<String, String> = mutableMapOf()

    init {
        roomProperties[Property.KEY_TEACHER_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "1"
            RoomType.LARGE_CLASS.value -> "1"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "1"
            /**-1表示不做限制*/
            else -> "-1"
        }
        roomProperties[Property.KEY_STUDENT_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "16"
            RoomType.LARGE_CLASS.value -> "-1"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "-1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "-1"
            else -> "-1"
        }
        roomProperties[Property.KEY_ASSISTANT_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "0"
            RoomType.SMALL_CLASS.value -> "0"
            RoomType.LARGE_CLASS.value -> "0"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "0"
            else -> "1"
        }
    }
}
