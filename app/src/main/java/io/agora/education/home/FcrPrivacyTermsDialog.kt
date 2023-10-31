package io.agora.education.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatButton
import io.agora.agoraeduuikit.util.MultiLanguageUtil
import io.agora.education.R
import java.util.*

/**
 * author : wufang
 * date : 2022/6/14
 * description :隐私政策和服务条款 弹框
 */
class FcrPrivacyTermsDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private var checkBox: CheckBox
    private var btnAccept: AppCompatButton
    private var btnDecline: AppCompatButton
    private var mDialogListener: OnPrivacyTermsDialogListener? = null

    init {
        setContentView(R.layout.fcr_privacy_terms_dialog)
        this.checkBox = this.findViewById(R.id.termsCheck)
        btnAccept = this.findViewById(R.id.btn_accept)
        btnDecline = this.findViewById(R.id.btn_decline)
        this.setCancelable(false)
        val window = this.window
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        window?.decorView?.setBackgroundResource(android.R.color.white)
        initWebView(this.findViewById(R.id.webview))
        this.btnAccept.isEnabled = false
        this.btnAccept.setOnClickListener { mDialogListener?.onPositiveClick() }
        this.btnDecline.setOnClickListener { mDialogListener?.onNegativeClick() }
        this.checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            this.btnAccept.isEnabled = isChecked
        }
        this.checkBox.requestFocus()
    }

    private fun initWebView(webView: WebView) {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        if (getLocalLanguage().equals("cn", ignoreCase = true)) {
            webView.loadUrl("https://agora-adc-artifacts.s3.cn-north-1.amazonaws.com.cn/demo/education/privacy.html")
        } else {
            webView.loadUrl("https://agora-adc-artifacts.s3.cn-north-1.amazonaws.com.cn/demo/education/privacy_en.html")
        }
        //        webView.loadUrl(String.format(Locale.US, "file:android_asset/privacy_%s.html", getLocalLanguage()));
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
    }

    private fun getLocalLanguage(): String {
        val locale = MultiLanguageUtil.getAppLocale(context.applicationContext)

        if (locale.language.equals(Locale.SIMPLIFIED_CHINESE.language, ignoreCase = true) &&
            locale.country.equals(Locale.SIMPLIFIED_CHINESE.country, ignoreCase = true)
        ) {
            return "cn"
        } else {
            return "en"
        }
    }

    override fun show() {
        super.show()
    }

    fun setPrivacyTermsDialogListener(listener: OnPrivacyTermsDialogListener?) {
        mDialogListener = listener
    }

    interface OnPrivacyTermsDialogListener {
        fun onPositiveClick()
        fun onNegativeClick()
    }
}

