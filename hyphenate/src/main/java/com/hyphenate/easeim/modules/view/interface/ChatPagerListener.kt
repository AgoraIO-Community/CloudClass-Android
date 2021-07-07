package com.hyphenate.easeim.modules.view.`interface`

interface ChatPagerListener {

    /**
     * 已全局禁言
     */
    fun onAllMemberMuted(isMuted: Boolean)

    /**
     * 已被单独禁言
     */
    fun onSingleMuted(isMuted: Boolean)

    /**
     * 隐藏Icon点击
     */
    fun onIconHideenClick()

}