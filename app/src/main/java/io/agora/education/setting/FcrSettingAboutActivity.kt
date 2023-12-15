package io.agora.education.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.education.utils.AppUtil
import io.agora.education.BuildConfig
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.utils.FcrPrivateProtocolUtils

class FcrSettingAboutActivity : AppCompatActivity() {
    private val TAG = "SettingActivity2"
    private lateinit var rootLayout: RelativeLayout
    private lateinit var tvReleaseTime: AppCompatTextView
    private lateinit var tvReleaseVer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        setContentView(R.layout.activity_setting)
        rootLayout = findViewById(R.id.root_Layout)
        tvReleaseVer = findViewById(R.id.tv_version)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
        findViewById<RelativeLayout>(R.id.disclaimers).setOnClickListener {
            val intent = Intent(this, FcrSettingDisclaimersActivity::class.java)
            startActivity(intent)
        }
        findViewById<RelativeLayout>(R.id.registerAgora).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.signup_url)))
            startActivity(intent)
        }
        tvReleaseTime = findViewById(R.id.tv_releaseTime)
        tvReleaseTime.text = BuildConfig.RELEASE_TIME
        tvReleaseVer.text = BuildConfig.AppSDKVersion

        initRegion()
    }

    fun initRegion() {
        // 国内外协议不一样
        if (AppUtil.isChina) {
            findViewById<RelativeLayout>(R.id.privacyOrdinance).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FcrPrivateProtocolUtils.privacyPolicy))
                startActivity(intent)
            }
            findViewById<RelativeLayout>(R.id.termsOfService).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FcrPrivateProtocolUtils.userAgreement))
                startActivity(intent)
            }
        } else {
            findViewById<RelativeLayout>(R.id.privacyOrdinance).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.termsOfService).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FcrPrivateProtocolUtils.userService))
                startActivity(intent)
            }
        }
    }
}