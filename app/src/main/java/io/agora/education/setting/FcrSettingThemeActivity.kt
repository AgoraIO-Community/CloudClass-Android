package io.agora.education.setting

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.utils.SkinUtils
import io.agora.education.utils.AppUtil
import io.agora.education.R
import io.agora.education.databinding.ActivityThemeSwitchBinding

/**
 * author : felix
 * date : 2022/7/20
 * description : 设置主题
 */
class FcrSettingThemeActivity : AppCompatActivity() {
    private val TAG = "ThemeSwitchActivity"
    lateinit var binding: ActivityThemeSwitchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivityThemeSwitchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            finish()
        }

        setTheme()
    }

    private fun setTheme() {
        val fcrTheme = PreferenceManager.get(Constants.KEY_SP_NIGHT, false)
        when (fcrTheme) {
            false -> {//明亮模式
                PreferenceManager.put(Constants.KEY_SP_NIGHT, false)
                binding.rbThemeLight.isChecked = true

            }
            true -> {//暗黑模式
                PreferenceManager.put(Constants.KEY_SP_NIGHT, true)
                binding.rbThemeNight.isChecked = true
            }
        }


        binding.fcrRgThemeSwitch.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_theme_light -> {
                    PreferenceManager.put(Constants.KEY_SP_NIGHT, false)
                    SkinUtils.setNightMode(false)
                }

                R.id.rb_theme_night -> {
                    PreferenceManager.put(Constants.KEY_SP_NIGHT, true)
                    SkinUtils.setNightMode(true)
                }
            }
            val themeMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false)
            if (themeMode != null) {
                AgoraEduSDK.isNightTheme = themeMode
            }
        }
    }
}