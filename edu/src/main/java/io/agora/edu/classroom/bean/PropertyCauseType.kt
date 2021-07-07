package io.agora.edu.classroom.bean

open class PropertyCauseType {
    companion object {
        const val CMD = "cmd"
        const val DATA = "data"

        // Recording status changed
        const val RECORD_STATE_CHANGED = 1

        // The local camera status has changed
        const val CAMERA_STATE = 4

        // There are rewards
        const val REWARD_CHANGED = 1101

        const val HANDSUP_ENABLE_CHANGED = 5

        const val COVIDEO_CHANGED = 501

        const val EXTAPP_CHANGED = 7;

        const val SIDE_CHAT_CREATE = 600
        const val SIDE_CHAT_DESTROY = 601
    }
}
