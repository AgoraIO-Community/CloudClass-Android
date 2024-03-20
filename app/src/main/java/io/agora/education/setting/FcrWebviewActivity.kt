package io.agora.education.setting

import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeduuikit.BuildConfig
import io.agora.education.databinding.ActivityWebviewBinding

/**
 * author : felix
 * date : 2023/7/31
 * description :
 */

class FcrWebviewActivity : AppCompatActivity() {
    lateinit var binding: ActivityWebviewBinding
    var webViewUrl: String? = null
    var title: String? = null

    companion object {
        val WEBVIEW_URL = "io.agora.education.setting.webview.url"
        val WEBVIEW_TITLE = "io.agora.education.setting.webview.title"

        fun startWebView(context: Context, webViewUrl: String) {
            val intent = Intent(context, FcrWebviewActivity::class.java)
            intent.putExtra(WEBVIEW_URL, webViewUrl)
            context.startActivity(intent)
        }

        fun startWebView(context: Context, title: String, webViewUrl: String) {
            val intent = Intent(context, FcrWebviewActivity::class.java)
            intent.putExtra(WEBVIEW_URL, webViewUrl)
            intent.putExtra(WEBVIEW_TITLE, title)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webViewUrl = intent.getStringExtra(WEBVIEW_URL)
        title = intent.getStringExtra(WEBVIEW_TITLE)
        initWebView()
    }

    fun initWebView() {
        binding.fcrBack.setOnClickListener {
            finish()
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val webSettings: WebSettings = binding.fcrWebview.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.databaseEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        binding.fcrWebview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return false
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
                super.onReceivedSslError(view, handler, error)
            }
        }

        if(TextUtils.isEmpty(title)) {
            binding.fcrWebview.webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    binding.fcrTitle.text = title
                }
            }
        }else{
            binding.fcrTitle.text = title
        }

        webViewUrl?.let {
            binding.fcrWebview.loadUrl(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //binding.fcrWebview.destroy()
    }
}