package io.agora.education.request.bean

/*拼接授权地址 body*/
class FcrRedirectUrlReq(val toRegion: String) {
    val redirectUrl: String = "https://sso2.agora.io/"
}