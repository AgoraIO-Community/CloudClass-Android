package io.agora.edu.common.bean.request

import io.agora.edu.common.bean.request.ChatTranslationLan.AUTO

data class ChatTranslateReq(val content: String) {
    var from: String = AUTO
    var to: String = AUTO

    constructor(content: String, to: String) : this(content) {
        this.to = to
    }

    constructor(content: String, from: String, to: String) : this(content) {
        this.from = from
        this.to = to
    }
}

/**详见: http://ai.youdao.com/DOCSIRMA/html/%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E7%BF%BB%E8%AF%91/API%E6%96%87%E6%A1%A3/%E6%96%87%E6%9C%AC%E7%BF%BB%E8%AF%91%E6%9C%8D%E5%8A%A1/%E6%96%87%E6%9C%AC%E7%BF%BB%E8%AF%91%E6%9C%8D%E5%8A%A1-API%E6%96%87%E6%A1%A3.html#section-9*/
object ChatTranslationLan {
    val AUTO = "auto"
    val CN = "zh-CHS"
    val EN = "en"
    val JA = "ja"
    val KO = "ko"
    val FR = "fr"
    val ES = "es"
    val PT = "pt"
    val IT = "it"
    val RU = "ru"
    val VI = "vi"
    val DE = "de"
    val AR = "ar"
}