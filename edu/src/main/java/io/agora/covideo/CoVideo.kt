package io.agora.covideo

object CoVideoState {
    /**
     * 初始状态
     */
    const val DisCoVideo = 0

    /**
     * 申请中
     */
    const val Applying = 1

    /**
     * 连麦中
     */
    const val CoVideoing = 2
}

object CoVideoActionType {
    /**
     * student apply co-video
     */
    var APPLY = 1

    /**
     * teacher accept apply
     */
    var ACCEPT = 3

    /**
     * teacher reject apply
     */
    var REJECT = 4

    /**
     * student cancel apply
     */
    var CANCEL = 5

    /**
     * teacher abort co-video
     */
    var ABORT = 6

    /**
     * student exit co-video
     */
    var EXIT = 7
}

object CoVideoSwitchState {
    const val DISABLE = 0

    /*可以举手*/
    const val ENABLE = 1
}

object CoVideoApplySwitchState {
    const val DISABLE = 0

    /*需要申请才能上台*/
    const val ENABLE = 1
}