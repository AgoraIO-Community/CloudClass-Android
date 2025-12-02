package io.agora.education.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import io.agora.education.R
import io.agora.education.home.FcrSplashActivity
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.utils.AppUtil

class FcrSettingActivity : AppCompatActivity() {
    private val TAG = "SettingPageActivity"
    private lateinit var rootLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        setContentView(R.layout.activity_setting_page)
        rootLayout = findViewById(R.id.activity_setting_page_root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        findViewById<RelativeLayout>(R.id.rl_normal_setting).setOnClickListener {
            val intent = Intent(this, FcrSettingNormalActivity::class.java)
            startActivity(intent)
        }
        //about us
        findViewById<RelativeLayout>(R.id.rl_about_us).setOnClickListener {
            val intent = Intent(this, FcrSettingAboutActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tv_fcr_logout).setOnClickListener {
            val intent = Intent(this, FcrSplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            AppUserInfoUtils.logout()
            finish()
        }

        if (!AppUserInfoUtils.isLogin()) {
            findViewById<TextView>(R.id.tv_fcr_logout).visibility = View.GONE
        }
    }
}