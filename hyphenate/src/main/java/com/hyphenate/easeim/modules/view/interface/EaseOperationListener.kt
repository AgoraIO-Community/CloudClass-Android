package com.hyphenate.easeim.modules.view.`interface`

import com.hyphenate.chat.EMMessage

interface EaseOperationListener {

    /**
     * 加载本地消息完成
     */
    fun loadMessageFinish(messages: List<EMMessage>)

    /**
     * 漫游消息完成
     */
    fun loadHistoryMessageFinish()

    /**
     * 获取聊天室公告完成
     */
    fun fetchAnnouncementFinish(announcement: String)

    /**
     * 获取聊天室全员禁言状态
     */
    fun fetchChatRoomAllMutedStatus(isMuted: Boolean)

    /**
     * 获取聊天室自己的禁言状态
     */
    fun fetchChatRoomSingleMutedStatus(isMuted: Boolean)

}