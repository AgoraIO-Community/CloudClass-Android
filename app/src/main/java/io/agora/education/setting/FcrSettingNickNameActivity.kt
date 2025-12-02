package io.agora.education.setting

import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.education.utils.AppUtil
import io.agora.education.config.AppConstants
import io.agora.education.databinding.ActivityNicknameSettingBinding
import io.agora.education.request.AppUserInfoUtils

/**
 * author : wufang
 * date : 2022/8/16
 * description : 设置昵称
 */
class FcrSettingNickNameActivity : AppCompatActivity() {
    private val TAG = "NickNameSetting"
    lateinit var binding: ActivityNicknameSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivityNicknameSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            saveNickName()
            finish()
        }
        binding.btnClearNickname.setOnClickListener {
            binding.etNickname.setText("")
        }
        var fcrNickName = PreferenceManager.get(AppConstants.KEY_SP_NICKNAME, "")
        if (TextUtils.isEmpty(fcrNickName)) {
            fcrNickName = AppUserInfoUtils.getUserInfo()?.userName ?: ""
            PreferenceManager.put(AppConstants.KEY_SP_NICKNAME, fcrNickName)
        }
        binding.etNickname.setText(fcrNickName)
    }

    private fun saveNickName() {
        PreferenceManager.put(AppConstants.KEY_SP_NICKNAME, binding.etNickname.text.toString())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveNickName()
    }
}