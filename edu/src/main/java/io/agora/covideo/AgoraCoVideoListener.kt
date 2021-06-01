package io.agora.covideo

interface AgoraCoVideoListener {

    /**发起申请连麦*/
    fun onCoVideoApply()

    /**发起取消连麦*/
    fun onCoVideoCancel()

    /**连麦被老师强制终止*/
    fun onCoVideoAborted()

    /**连麦申请被接受*/
    fun onCoVideoAccepted()

    /**连麦申请被拒绝*/
    fun onCoVideoRejected()

}