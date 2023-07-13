package io.agora.education.home

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.TokenUtils
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.education.R
import io.agora.education.databinding.ActivityLoginBinding
import io.agora.education.utils.AppUtil


val LOGIN_URL_KEY = "io.agora.education.redirectUrl"

class FcrLoginActivity : AppCompatActivity() {
    val TAG = "LoginActivity"
    lateinit var binding: ActivityLoginBinding
    lateinit var agoraLoading: AgoraLoadingDialog
    var isLoginSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        AppUtil.hideStatusBar(window, true)
        setContentView(binding.root)
        agoraLoading = AgoraLoadingDialog(this)
        if (!this.isFinishing) {
            agoraLoading.show()
        }
        val webViewUrl = intent.extras?.getString(LOGIN_URL_KEY)
        Log.i(TAG, "url = $webViewUrl")

        if (TextUtils.isEmpty(webViewUrl)) {
            finish()
        }

        if (webViewUrl != null) {
            clearCookies()
            loadWebView(webViewUrl)
        }
    }

    fun clearCookies() {
        CookieSyncManager.createInstance(this)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        CookieManager.getInstance().flush()
    }

    fun loadWebView(webViewUrl: String) {
        //debug 模式 方便使用chrome://inspect
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        binding.webViewLogin.clearCache(true)
        //获取WebSettings类的实例，此类用于对WebView加载的网页进行设置
        val webSettings: WebSettings = binding.webViewLogin.settings
        //使WebView可以使用JavaScript
        binding.webViewLogin.settings.apply {
            javaScriptEnabled = true
            allowUniversalAccessFromFileURLs = true
            domStorageEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            webSettings.loadWithOverviewMode = true
            webSettings.domStorageEnabled = true
            webSettings.blockNetworkImage = false
            webSettings.useWideViewPort = true
        }

        if (PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false) == true) {
            webSettings.userAgentString =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"
        }
        //请求加载百度，并交由Webclient去处理
        binding.webViewLogin.loadUrl(webViewUrl)
        //使用WebViewClient设置监听并处理WebView的请求事件
        binding.webViewLogin.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.i(TAG, "url = $url")
                parseUrl(url)
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                agoraLoading.dismiss()
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                Log.e(TAG, "onReceivedSslError sslError=" + error?.primaryError)
                handler?.proceed()
//                if (error?.primaryError == android.net.http.SslError.SSL_INVALID) {// 校验过程遇到了bug
//                    handler?.proceed()
//                } else {
//                    handler?.cancel()
//                }
            }
        }
    }

    fun parseUrl(url: String) {
        if (isLoginSuccess) {
            return
        }

        val uri: Uri = Uri.parse(url)
        val refreshToken: String? = uri.getQueryParameter("refreshToken")
        val accessToken: String? = uri.getQueryParameter("accessToken")
        if (!TextUtils.isEmpty(refreshToken) && !TextUtils.isEmpty(accessToken)) {
            isLoginSuccess = true
            TokenUtils.putToken(refreshToken, accessToken)
            ToastManager.showShort(resources.getString(R.string.fcr_login_s))
            val intent = Intent()
            intent.putExtra("isLogin", true)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}