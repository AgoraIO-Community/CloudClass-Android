package io.agora.education.setting

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.education.utils.AppUtil
import io.agora.education.databinding.ActivitySettingLogoutBinding
import io.agora.education.home.FcrSplashActivity
import io.agora.education.request.AppUserInfoUtils

class FcrSettingLogoutActivity : AppCompatActivity() {
    private val TAG = "SettingLogoutActivity"
    lateinit var binding: ActivitySettingLogoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivitySettingLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.tvFcrDeleteAccount.setOnClickListener {
            if (binding.termsLogout.isChecked) {
                AppUserInfoUtils.deleteAccount {
                    if (it) {
                        val intent = Intent(this, FcrSplashActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }

}