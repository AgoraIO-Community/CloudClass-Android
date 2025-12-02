package io.agora.education.request.bean

/*拼接授权地址 body*/
/**
 * https://dev-docs.agoralab.co/blog/sso-v2-usage-guidelines/
 * toRegion :
 * 如需 SSO 跳转到中国站，可添加参数 toRegion=cn
 * 如需 SSO 跳转到海外站，可添加参数 toRegion=en
 */
class FcrRedirectUrlReq(val toRegion: String) {
    val redirectUrl: String = "https://sso2.agora.io/"
}