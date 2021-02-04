package io.agora.edu.classroom.bean

open class PropertyCauseType {
    companion object {
        const val CMD = "cmd"
        const val DATA = "data"

        /*录制状态发生改变*/
        const val RECORDSTATECHANGED = 1
        /**有奖励发生*/
        const val REWARDCHANGED = 1101
    }
}
