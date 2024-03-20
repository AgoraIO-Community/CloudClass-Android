package com.agora.edu.component.teachaids.webviewwidget

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
import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.BuildConfig
import io.agora.agoraeduuikit.databinding.FcrWebviewWidgetContentBinding
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl


/**
 * author : wufang
 * date : 2022/5/30
 * description :放webview的 widget
 */
class FcrWebViewWidget : AgoraBaseWidget() {
    private var webViewContainerLayout: ViewGroup? = null//用来放webView的container
    var webViewContent: FcrWebViewContent? = null
    private var context: Context? = null
    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
        }
    }

    override fun init(container: ViewGroup) {//parent: webView的Container
        super.init(container)
        widgetInfo?.let { info ->
            info.roomProperties?.let {
                val webViewUrl = (it as? Map<*, *>)?.get("webViewUrl") ?: "" as String ?: ""
                webViewContent = FcrWebViewContent(container, webViewUrl as String, container.context)
            }
        }

    }

    override fun release() {
        container?.removeAllViews()
        super.release()
    }


    //通过接收消息更新widget的view
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: FcrWebViewInteractionPacket? = Gson().fromJson(message, FcrWebViewInteractionPacket::class.java)
        packet?.let {
            val bodyStr = GsonUtil.toJson(packet.body) //body 就是webView的url

            when (packet.signal) {
                //打开大窗
                FcrWebViewInteractionSignal.FcrWebViewShowed -> {
                    bodyStr?.let {
                        (GsonUtil.jsonToObject<String>(bodyStr))?.let { webViewUrl ->
                            //拿到userDetailInfo显示大窗
                            webViewContainerLayout?.post {
                                webViewContent!!.loadWebView(bodyStr)
                                webViewContainerLayout?.addView(
                                    webViewContent!!, ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        } ?: Runnable {
                            LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                        }
                    }
                }
                //关闭大窗
                FcrWebViewInteractionSignal.FcrWebViewClosed -> {
                    bodyStr?.let {
                        (GsonUtil.jsonToObject<String>(bodyStr))?.let { webViewUrl ->
//                            webViewContent?.upsertUserDetailInfo(null)
                            webViewContainerLayout?.post { webViewContainerLayout!!.removeAllViews() }
                            webViewContent = null
                        } ?: Runnable {
                            LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
    }

    inner class FcrWebViewContent(container: ViewGroup, webViewUrl: String, context: Context) : View(context) {
        private val tag = "AgoraEduVideoComponent"
        private var mWebViewUrl = "https://www.baidu.com/"

        // widget's ui has be added to container in here，not repeat add
        val binding = FcrWebviewWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )

        init {
            initUI()
            loadWebView(webViewUrl)
        }

        fun loadWebView(webViewUrl: String) {
            widgetInfo?.roomProperties?.let {

            }
            mWebViewUrl = webViewUrl
            //debug 模式 方便使用chrome://inspect
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            //获取WebSettings类的实例，此类用于对WebView加载的网页进行设置
            val webSettings: WebSettings = binding.webViewContent.settings
            //使WebView可以使用JavaScript
            webSettings.javaScriptEnabled = true
            //webView 开启存储权限
            webSettings.domStorageEnabled = true
            webSettings.allowFileAccess = true
            webSettings.databaseEnabled = true
            //请求加载百度，并交由Webclient去处理
            binding.webViewContent.loadUrl(webViewUrl)
            //使用WebViewClient设置监听并处理WebView的请求事件
            binding.webViewContent.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    //根据url真正去加载网页的操作
                    view.loadUrl(url)
                    //在当前WebView中打开网页，而不在浏览器中
                    return false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()

                    /*val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    builder.setMessage("ssl certificate error")
                    builder.setPositiveButton("continue") { dialog, which -> handler?.proceed() }
                    builder.setNegativeButton("cancel") { dialog, which -> handler?.cancel() }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()*/
                }
            }

            binding.webViewContent.webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
//                    binding.fcrFileName.text = title//显示webview标题
                }
            }
        }

        private fun initUI() {
            binding.btnRefresh.setOnClickListener {
                binding.webViewContent.loadUrl(mWebViewUrl)
            }
        }

        //webveiw 防止内存泄漏
//        fun destroyWebView() {
//            binding.webViewContent.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
//            binding.webViewContent.clearHistory()
//            (binding.webViewContent.parent as ViewGroup).removeView(binding.webViewContent)
//            binding.webViewContent.destroy()
//        }
    }
}