package io.agora.online.component.teachaids.webviewwidget

import android.content.Context
import android.net.http.SslError
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.agora.online.widget.FcrWidgetManager
import io.agora.online.widget.bean.FcrWebViewInfo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessage
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetInfo
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.BuildConfig
import io.agora.online.databinding.FcrOnlineWebviewWidgetContentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal


/**
 * author : felix
 * date : 2023/6/14
 * description : webview
 * https://github.com/PierfrancescoSoffritti/android-youtube-player/tree/master
 */
class FcrWebViewWidget : AgoraBaseWidget() {
    override val TAG = "FcrWebViewWidget"
    var webViewContent: FcrWebViewContent? = null
    var isYouTubeVideo = false
    var webViewInfo: FcrWebViewInfo? = null

    companion object {
        val youtubeList = listOf(
            "https://youtube.com/watch?v=",
            "https://youtube.com/embed/",
            "https://youtu.be/",
            "https://youtube.com/embed/",
        )

        fun getWebViewWidgetInfo(roomProperties: MutableMap<String, Any>?): FcrWebViewInfo? {
            var info: FcrWebViewInfo? = null
            roomProperties?.let {
                val str = GsonUtil.toJson(roomProperties)
                info = GsonUtil.jsonToObject<FcrWebViewInfo>(str)
            }
            return info
        }

        fun getTitle(widgetInfo: AgoraWidgetInfo?): String {
            widgetInfo?.roomProperties?.let {
                return "" + it[FcrWidgetManager.WIDGET_WEBVIEW_TITLE]
            }
            return ""
        }

        fun isYouTubeVideo(url: String?): Boolean {
            var isYouTubeVideo = false
            url?.let {
                youtubeList.forEach {
                    if (url.startsWith(it)) {
                        isYouTubeVideo = true
                    }
                }
            }
            return isYouTubeVideo
        }

        fun getYouTubeVideoId(url: String): String {
            var videoId = ""
            youtubeList.forEach {
                if (url.startsWith(it)) {
                    val arr = url.split(it)
                    if (arr.size == 2) {
                        videoId = arr[1]
                    }
                }
            }
            return videoId
        }
    }

    override fun init(container: ViewGroup) {
        super.init(container)
        ContextCompat.getMainExecutor(container.context).execute {
            widgetInfo?.roomProperties?.let {
                webViewInfo = getWebViewWidgetInfo(it)
                isYouTubeVideo = isYouTubeVideo(webViewInfo?.webViewUrl)
                webViewContent = FcrWebViewContent(container, webViewInfo, container.context)
                webViewContent?.onSyncWebViewInfo = { map ->
                    eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid?.let {
                        map.put("operatorId", it)
                        updateRoomProperties(map, mutableMapOf(), null)
                    }
                }
            }

            if (isYouTubeVideo) {
                eduCore?.eduContextPool()?.widgetContext()
                    ?.addWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
            }
            LogX.i("WebViewWidget", "roomProperties = ${widgetInfo?.roomProperties}")
        }
    }

    private var localUserGranted = false //当前本地用户是否授权
    private val whiteBoardWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet2 = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet2.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                    if (localUser.role == AgoraEduContextUserRole.Student) {
                        var granted = false
                        if (packet2.body is MutableList<*>) { // 白板开关的格式
                            granted = (packet2.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                        } else { // 白板授权的格式
                            val bodyStr = GsonUtil.gson.toJson(packet2.body)
                            val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                            if (agoraBoard.granted) {
                                granted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                            }
                        }
                        localUserGranted = granted

                        ContextCompat.getMainExecutor(container?.context!!).execute {
                            webViewContent?.setCanOperation(localUserGranted)
                        }
                    }
                }
            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>?,
        keys: MutableList<String>,
        operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        LogX.i(TAG, "onWidgetRoomPropertiesUpdated properties = ${properties}")

        webViewInfo = getWebViewWidgetInfo(properties)
        webViewContent?.updateWebViewInfo(webViewInfo)
        if (webViewInfo?.operatorId != eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid) {
            webViewContent?.updateYouTubeView()
        }
        webViewContent?.updateMyTag()
    }

    override fun onReceiveWidgetMessage(message: AgoraWidgetMessage) {
        if (message.action == 1) {
            webViewContent?.refresh()
        }
    }

    override fun release() {
        container?.removeAllViews()
        webViewContent?.release()
        if (isYouTubeVideo) {
            eduCore?.eduContextPool()?.widgetContext()
                ?.removeWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        }
        super.release()
    }

    inner class FcrWebViewContent(container: ViewGroup, var webViewInfo: FcrWebViewInfo?, context: Context) :
        View(context) {
        val binding = FcrOnlineWebviewWidgetContentBinding.inflate(LayoutInflater.from(container.context), container, true)
        var isImOperation = false

        val youTubePlayerListener = object : FcrYouTubePlayerListener() {
            var time = System.currentTimeMillis()
            var TIME_GAP = 5000

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                super.onStateChange(youTubePlayer, state)
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    time = System.currentTimeMillis()
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                webViewInfo?.currentTime = second

                if (localUserGranted && isImOperation && System.currentTimeMillis() - time >= TIME_GAP) {
                    LogX.i(TAG, "sync youtube progress")
                    time = System.currentTimeMillis()
                    // syn to another
                    val data = mutableMapOf<String, Any>()
                    data.put("currentTime", tracker.currentSecond)
                    webViewInfo?.isPlaying?.let {
                        data.put("isPlaying", it)
                    }
                    onSyncWebViewInfo?.invoke(data)
                    time = System.currentTimeMillis()
                }
            }
        }
        var onSyncWebViewInfo: ((MutableMap<String, Any>) -> Unit)? = null

        init {
            loadWebView()
        }

        fun loadWebView() {
            if (isYouTubeVideo) {
                binding.webViewContent.visibility = View.GONE
                binding.youtubePlayerView.visibility = View.VISIBLE
                updateMyTag()
                setYoutubeWebView()
            } else {
                binding.youtubePlayerMask.visibility = View.GONE
                binding.webViewContent.visibility = View.VISIBLE
                binding.youtubePlayerView.visibility = View.GONE
                setWebView()
            }
        }

        fun setWebView() {
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            val webSettings: WebSettings = binding.webViewContent.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.allowFileAccess = true
            webSettings.databaseEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            binding.webViewContent.loadUrl(webViewInfo?.webViewUrl ?: "")
            //binding.fcrFileName.text = title
            binding.webViewContent.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }
            }
            binding.webViewContent.webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    // binding.fcrFileName.text = title
                }
            }
        }

        val tracker = YouTubePlayerTracker()
        var youTubePlayer: YouTubePlayer? = null

        fun setYoutubeWebView() {
            val iframePlayerOptions = IFramePlayerOptions.Builder().controls(0).mute(0).build()
            binding.youtubePlayerView.initialize(youTubePlayerListener, iframePlayerOptions)
            binding.root.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(binding.youtubePlayerView)
            binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    youTubePlayer = player

                    loadYouTubeVideo()

                    // controller ui
                    val defaultPlayerUiController =
                        FcrDefaultPlayerUiController(binding.youtubePlayerView, youTubePlayer!!)
                    defaultPlayerUiController.onClickSeekListener = { time ->
                        webViewInfo?.let {
                            webViewInfo?.currentTime = time
                            val data = mutableMapOf<String, Any>()
                            data.put("currentTime", time)
                            webViewInfo?.isPlaying?.let {
                                data.put("isPlaying", it)
                            }
                            onSyncWebViewInfo?.invoke(data)
                        }
                    }
                    defaultPlayerUiController.onClickPlayPauseListener = { playStatus ->
                        webViewInfo?.let {
                            webViewInfo?.currentTime = tracker.currentSecond
                            webViewInfo?.isPlaying = playStatus

                            val data = mutableMapOf<String, Any>()
                            data.put("currentTime", tracker.currentSecond)
                            data.put("isPlaying", playStatus)
                            onSyncWebViewInfo?.invoke(data)
                        }
                    }
                    binding.youtubePlayerView.setCustomPlayerUi(defaultPlayerUiController.rootView)
                }
            })

            binding.youtubePlayerView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    LogX.e(TAG, "youtubePlayer getYouTubePlayerWhenReady")
                    youTubePlayer.addListener(tracker)
                }
            })

            binding.youtubePlayerMask.setOnClickListener {
                LogX.e(TAG, "youtubePlayerMask")
            }
        }

        fun updateYouTubeView() {
            updateMyTag()
            ContextCompat.getMainExecutor(container?.context!!).execute {
                if (isYouTubeVideo) {
                    webViewInfo?.currentTime?.let {
                        if (Math.abs(tracker.currentSecond - it) > 3f) { // 3s ignore
                            youTubePlayer?.seekTo(it)
                        }
                    }

                    webViewInfo?.isPlaying?.let {
                        if (it) {
                            youTubePlayer?.play()
                        } else {
                            youTubePlayer?.pause()
                        }
                    }
                }
            }
        }

        fun updateWebViewInfo(webViewInfo: FcrWebViewInfo?) {
            this.webViewInfo = webViewInfo
        }

        fun updateMyTag() {
            isImOperation =
                webViewInfo?.operatorId == eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid
        }

        fun setCanOperation(isGranted: Boolean) {
            if (isYouTubeVideo) {
                if (isGranted) {
                    binding.youtubePlayerMask.visibility = View.GONE
                } else {
                    binding.youtubePlayerMask.visibility = View.VISIBLE
                }
            }else{
                binding.youtubePlayerMask.visibility = View.GONE
            }
        }

        fun loadYouTubeVideo() {
            val videoId = getYouTubeVideoId(webViewInfo?.webViewUrl ?: "")
            LogX.e(TAG, "youtubePlayer play videoId = $videoId")
            binding.root.findViewTreeLifecycleOwner()?.lifecycle?.let { lifecycle ->
                webViewInfo?.apply {
                    if (isPlaying == true) {
                        youTubePlayer?.loadVideo(videoId, currentTime ?: 0f)
                    } else {
                        youTubePlayer?.cueVideo(videoId, currentTime ?: 0f)
                    }
                }
            }
        }

        fun refresh() {
            if (isYouTubeVideo) {
                webViewInfo?.currentTime = tracker.currentSecond

                loadYouTubeVideo()

                if (localUserGranted) {
                    val data = mutableMapOf<String, Any>()
                    data.put("currentTime", tracker.currentSecond)
                    webViewInfo?.isPlaying?.let {
                        data.put("isPlaying", it)
                    }
                    onSyncWebViewInfo?.invoke(data)
                }
            } else {
                //binding.webViewContent.reload()
                binding.webViewContent.loadUrl(webViewInfo?.webViewUrl ?: "")
            }
        }

        fun release() {
            binding.webViewContent.destroy()
            binding.youtubePlayerView.release()
        }
    }
}