package io.agora.education.setting

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.education.utils.AppUtil
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.databinding.ActivityRegionSettingBinding


/**
 * author : wufang
 * date : 2022/8/16
 * description : 设置区域
 */
class FcrSettingRegionActivity : AppCompatActivity() {
    private val TAG = "RegionSettingActivity"
    lateinit var binding: ActivityRegionSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivityRegionSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            finish()
        }
        setRegion()
        //showTest()
    }

    fun showTest() {
        val isTest = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false)
        if (isTest) {
            binding.rbRegionAp.visibility = View.VISIBLE
            binding.rbRegionEu.visibility = View.VISIBLE
        }
    }

    private fun setRegion() {
        val fcrTheme = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        when (fcrTheme) {
            AgoraEduRegion.na -> {
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.na)
                binding.rbRegionNa.isChecked = true
            }
            AgoraEduRegion.ap -> {
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.ap)
                binding.rbRegionAp.isChecked = true
            }
            AgoraEduRegion.cn -> {
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
                binding.rbRegionCn.isChecked = true
            }
            AgoraEduRegion.eu -> {
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.eu)
                binding.rbRegionEu.isChecked = true
            }
        }


        binding.fcrRgRegionSetting.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_region_na -> {
                    PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.na)
                }

                R.id.rb_region_ap -> {
                    PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.ap)
                }
                R.id.rb_region_cn -> {
                    PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
                }
                R.id.rb_region_eu -> {
                    PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.eu)
                }
            }
//            val fcrLanguage = PreferenceManager.get(Constants.KEY_SP_REGION, AgoraEduRegion.cn)
//            if (fcrLanguage != null) {
//                AgoraEduSDK.region = fcrLanguage
//            }

        }
    }


}