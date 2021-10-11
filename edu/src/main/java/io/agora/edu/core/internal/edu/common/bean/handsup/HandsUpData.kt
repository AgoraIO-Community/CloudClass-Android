package io.agora.edu.core.internal.edu.common.bean.handsup

class HandsUpConfig(
        val enabled: Int,
        val type: Int,
        val timeout: Long,
        val maxWait: Int,
        val maxAccept: Int,
        val progress: MutableList<HandsUpProgress>?,
        val accepted: MutableList<HandsUpAccept>?
) {
    companion object {
        const val processesKey = "processes"
        const val handsUpKey = "handsUp"
    }
}

enum class HandsUpEnableState(val value: Int) {
    Disable(0),
    Enable(1)
}

class HandsUpResData(
        val processUuid: String,
        val addProgress: MutableList<HandsUpProgress>?,
        val removeProgress: MutableList<HandsUpProgress>?,
        val addAccepted: MutableList<HandsUpAccept>?,
        val removeAccepted: MutableList<HandsUpAccept>?,
        val actionType: Int
) {
}

data class HandsUpProgress(
        val userUuid: String
) {
}

data class HandsUpAccept(
        val userUuid: String) {
}