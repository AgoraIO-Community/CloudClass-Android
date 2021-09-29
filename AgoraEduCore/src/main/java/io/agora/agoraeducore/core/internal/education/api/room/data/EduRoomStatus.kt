package io.agora.agoraeducore.core.internal.education.api.room.data

// TODO NEED CONFIRM
enum class EduRoomState(var value: Int) {
    /**未开始*/
    INIT(0),
    /**开始*/
    START(1),
    /**结束*/
    END(2)
}

data class EduRoomStatus(
        var courseState: EduRoomState,
        var startTime: Long,
        var isStudentChatAllowed: Boolean,
        var onlineUsersCount: Int,
        var createTime: Long
)

enum class EduRoomChangeType {
    AllStudentsChat,
    CourseState
}

enum class EduMuteState(var value: Int) {
    /**不禁*/
    Enable(0),
    /**禁*/
    Disable(1)
}
