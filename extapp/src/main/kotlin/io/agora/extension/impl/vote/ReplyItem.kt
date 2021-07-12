package io.agora.extension.impl.vote

data class ReplyItem(
    val replyTime: String,
    val answer: Array<String>
)

data class ResultItem (
    val choice: String,
    var count: Int,
    var proportion: Int
)
