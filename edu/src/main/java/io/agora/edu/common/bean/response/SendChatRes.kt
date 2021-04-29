package io.agora.edu.common.bean.response

class SendChatRes(val code: Int, val msg: String, val data: SendChatResData)

class SendChatResData(val messageId: Int, sensitiveWords: MutableList<String>)