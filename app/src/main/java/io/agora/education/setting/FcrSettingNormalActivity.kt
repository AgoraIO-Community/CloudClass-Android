package io.agora.education.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.education.R
import io.agora.education.databinding.ActivityNormalSettingBinding
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.utils.AppUtil
import io.agora.education.utils.FcrPrivateProtocolUtils

/**
 * author : wufang
 * date : 2022/8/16
 * description : 常规设置
 */
class FcrSettingNormalActivity : AppCompatActivity() {
    private val TAG = "ThemeSwitchActivity"
    lateinit var binding: ActivityNormalSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivityNormalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.rlNicknameSetting.setOnClickListener {
            val intent = Intent(this, FcrSettingNickNameActivity::class.java)
            startActivity(intent)
        }
        binding.rlLanguageSetting.setOnClickListener {
            val intent = Intent(this, FcrSettingLanguageActivity::class.java)
            startActivity(intent)
        }
        binding.rlRegionSetting.setOnClickListener {
            val intent = Intent(this, FcrSettingRegionActivity::class.java)
            startActivity(intent)
        }
        binding.rlThemeSetting.setOnClickListener {
            val intent = Intent(this, FcrSettingThemeActivity::class.java)
            startActivity(intent)
        }
        binding.rlAccountLogout.setOnClickListener {
            val intent = Intent(this, FcrSettingLogoutActivity::class.java)
            startActivity(intent)
        }

        binding.layoutPersonInfo.setOnClickListener {
            FcrWebviewActivity.startWebView(
                this,
                this.resources.getString(R.string.fcr_settings_label_user_info_notice),
                FcrPrivateProtocolUtils.dataCollectionUrl
            )
        }

        binding.rlThirdLib.setOnClickListener {
            FcrWebviewActivity.startWebView(
                this,
                this.resources.getString(R.string.fcr_settings_label_data_share),
                FcrPrivateProtocolUtils.dataShareUrl
            )
        }

        if (!AppUserInfoUtils.isLogin()) {
            binding.rlAccountLogout.visibility = ViewGroup.GONE
            binding.rlNicknameSetting.visibility = ViewGroup.GONE
        }

        if (AppUtil.isChina) {
            binding.rlThirdLib.visibility = View.VISIBLE
            binding.layoutPersonInfo.visibility = View.VISIBLE
        } else {
            binding.rlThirdLib.visibility = View.GONE
            binding.layoutPersonInfo.visibility = View.GONE
        }
    }
}