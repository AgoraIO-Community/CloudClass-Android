package io.agora.education

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView

class SettingActivity : AppCompatActivity() {
    private val TAG = "SettingActivity2"
    private lateinit var rootLayout: RelativeLayout
    private lateinit var tvReleaseTime: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        setContentView(R.layout.activity_setting)
        rootLayout = findViewById(R.id.root_Layout)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
        findViewById<RelativeLayout>(R.id.privacyOrdinance).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)))
            startActivity(intent)
        }
        findViewById<RelativeLayout>(R.id.termsOfService).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.terms_of_service_url)))
            startActivity(intent)
        }
        findViewById<RelativeLayout>(R.id.disclaimers).setOnClickListener {
            val intent = Intent(this, DisclaimersActivity::class.java)
            startActivity(intent)
        }
        findViewById<RelativeLayout>(R.id.registerAgora).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.signup_url)))
            startActivity(intent)
        }
        tvReleaseTime = findViewById(R.id.tv_releaseTime)
        tvReleaseTime.text = io.agora.education.BuildConfig.RELEASE_TIME
    }
}