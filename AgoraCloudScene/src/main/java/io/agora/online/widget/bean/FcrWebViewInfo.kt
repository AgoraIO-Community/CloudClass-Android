package io.agora.online.widget.bean

/**
 * author : felix
 * date : 2023/6/16
 * description :
 */
class FcrWebViewInfo {
    var webviewTitle: String? = null
    var webViewUrl: String? = null
    var isPlaying: Boolean? = null
    var isMuted: Boolean? = null
    var currentTime: Float? = null
    var volume: Float? = null

    /**
     * userUuid
     */
    var operatorId: String? = null
    override fun toString(): String {
        return "FcrWebViewState{" +
                "webviewTitle='" + webviewTitle + '\'' +
                ", webViewUrl='" + webViewUrl + '\'' +
                ", isPlaying=" + isPlaying +
                ", isMuted=" + isMuted +
                ", currentTime=" + currentTime +
                ", volume=" + volume +
                ", operatorId='" + operatorId + '\'' +
                '}'
    }
}