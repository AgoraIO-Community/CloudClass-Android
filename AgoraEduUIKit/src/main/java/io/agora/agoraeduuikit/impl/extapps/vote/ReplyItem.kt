package io.agora.agoraeduuikit.impl.extapps.vote

data class ReplyItem(
    val startTime: String,
    val replyTime: String,
    val answer: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplyItem

        if (startTime != other.startTime) return false
        if (replyTime != other.replyTime) return false
        if (!answer.contentEquals(other.answer)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + replyTime.hashCode()
        result = 31 * result + answer.contentHashCode()
        return result
    }
}

data class ResultItem (
    val choice: String,
    var count: Int,
    var proportion: Int
)
