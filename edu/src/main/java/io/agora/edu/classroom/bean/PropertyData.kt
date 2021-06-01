package io.agora.edu.classroom.bean

object PropertyData {
    const val CMD = "cmd"
    const val DATA = "data"

    const val CUSTOM = "flexProps"

    // Recording status changed
    const val RECORD_STATE_CHANGED = 1

    // The local device status has changed(camera, mic, speaker)
    const val DEVICE_STATE = 4

    // There are rewards
    const val REWARD_CHANGED = 1101

    const val HANDSUP_ENABLE_CHANGED = 5

    const val MUTE_STATE_CHANGED = 6

    const val COVIDEO_CHANGED = 501

    const val EXTAPP_CHANGED = 7;

    const val SIDE_CHAT_CREATE = 600
    const val SIDE_CHAT_DESTROY = 601

    const val SWITCH_SCREENSHARE_COURSEWARE = 1301
}
