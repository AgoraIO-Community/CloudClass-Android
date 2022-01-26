package com.hyphenate.easeim.modules.view.`interface`

import com.hyphenate.chat.EMMessage

interface ChatPagerListener {

    /**
     * 被禁言
     */
    fun onMuted(isMuted: Boolean)

    /**
     * 隐藏Icon点击
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

    /**
     * 表情图标点击
     */
    fun onFaceIconClick()

    /**
     * 图片消息点击事件
     */
    fun onImageClick(message: EMMessage)

    /**
     * 大图关闭事件
     */
    fun onCloseImage()

}