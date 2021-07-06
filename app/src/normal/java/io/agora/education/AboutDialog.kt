package io.agora.education

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.BuildConfig

class AboutDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private var tvReleaseTime: AppCompatTextView
    private var tvSDKVersion: AppCompatTextView
    private var tvAppVersion: AppCompatTextView
    private lateinit var aboutLayout: LinearLayout
    private lateinit var disclaimersLayout: LinearLayout

    init {
        setContentView(R.layout.dialog_about)
        if (context is Activity) {
            setOwnerActivity(context)
        }
        setCancelable(false)
        findViewById<AppCompatTextView>(R.id.tv_close).setOnClickListener {
            dismiss()
        }
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            aboutLayout.visibility = VISIBLE
            disclaimersLayout.visibility = GONE
        }
        findViewById<RelativeLayout>(R.id.privacyOrdinance).setOnClickListener {
            ownerActivity?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.getString(R.string.privacy_url)))
                it.startActivity(intent)
            }
        }
        findViewById<RelativeLayout>(R.id.disclaimers).setOnClickListener {
            aboutLayout.visibility = GONE
            disclaimersLayout.visibility = VISIBLE
        }
        findViewById<RelativeLayout>(R.id.registerAgora).setOnClickListener {
            ownerActivity?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.getString(R.string.signup_url)))
                it.startActivity(intent)
            }
        }
        tvReleaseTime = findViewById(R.id.tv_releaseTime)
        tvReleaseTime.text = io.agora.education.BuildConfig.RELEASE_TIME
        tvSDKVersion = findViewById(R.id.tv_SDKVersion)
        tvAppVersion = findViewById(R.id.tv_AppVersion)
        aboutLayout = findViewById(R.id.about_Layout)
        disclaimersLayout = findViewById(R.id.disclaimers_Layout)
        ownerActivity?.let {
            tvSDKVersion.text = String.format(it.getString(R.string.version1), io.agora.education.BuildConfig.RTC_VERSION)
            tvAppVersion.text = String.format(it.getString(R.string.version1), BuildConfig.APAAS_VERSION)
        }
    }
}