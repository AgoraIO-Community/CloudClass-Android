package com.hyphenate.easeim.modules.exception

class ChatError(_code:Int, message: String) : RuntimeException(message) {
    val code = _code
}