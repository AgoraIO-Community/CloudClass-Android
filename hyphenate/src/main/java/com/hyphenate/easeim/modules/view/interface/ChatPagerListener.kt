package com.hyphenate.easeim.modules.view.`interface`

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

}