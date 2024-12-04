package com.hyphenate.easeimgroup.modules.exception

class ChatError(_code:Int, message: String) : RuntimeException(message) {
    val code = _code
}