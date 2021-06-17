package io.agora.edu.common.bean.response

class SendChatRes(val code: Int, val msg: String, val data: SendChatResData)

class SendChatResData(val messageId: Int, sensitiveWords: MutableList<String>)

class ConversationRes(val code: Int, val msg: String, val data: ConversationResData)

class ConversationResData(val peerMessageId: String)