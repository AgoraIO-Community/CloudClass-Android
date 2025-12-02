package com.hyphenate.easeim.modules.view.`interface`

import io.agora.chat.ChatMessage


interface ViewEventListener {

    /**
     * 公告栏点击
     */
    fun onAnnouncementClick()

    /**
     * 文本框点击
     */
    fun onMsgContentClick()

    /**
     * 表情图标点击
     */
    fun onFaceIconClick()

    /**
     * 图片图标点击
     */
    fun onPicIconClick()

    /**
     * 图片消息点击事件
     */
    fun onImageClick(message: ChatMessage)

    /**
     * 公告修改
     */
    fun onAnnouncementChange(content: String)
}