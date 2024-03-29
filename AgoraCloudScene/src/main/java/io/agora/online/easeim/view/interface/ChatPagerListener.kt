package io.agora.online.easeim.view.`interface`

import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.chat.ChatMessage


interface ChatPagerListener {

    /**
     * 被禁言
     */
    fun onMuted(isMuted: Boolean)

    /**
     * 隐藏 Icon 点击
     */
    fun onIconHideenClick()

    /**
     * 显示未读
     */
    fun onShowUnread(show: Boolean)

    /**
     * 文本框点击
     */
    fun onMsgContentClick()

    fun onMsgReceiverClick()

    fun onPrivateReceiverClick(receiver: AgoraUIUserDetailInfo?)

    /**
     * 表情图标点击
     */
    fun onFaceIconClick()

    /**
     * 图片消息点击事件
     */
    fun onImageClick(message: ChatMessage)

    /**
     * 大图关闭事件
     */
    fun onCloseImage()

}