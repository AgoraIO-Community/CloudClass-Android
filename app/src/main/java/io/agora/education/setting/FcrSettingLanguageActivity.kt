package io.agora.education.setting

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.education.utils.AppUtil
import io.agora.education.EduApplication
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.agoraeduuikit.util.MultiLanguageUtil
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.education.databinding.ActivityLanguageSettingBinding
import io.agora.education.home.FcrSplashActivity
import java.util.*

/**
 * author : wufang
 * date : 2022/8/16
 * description : 设置语言
 */
class FcrSettingLanguageActivity : AppCompatActivity() {
    private val TAG = "LanguageSetting"
    lateinit var binding: ActivityLanguageSettingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        binding = ActivityLanguageSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        binding.ivBack.setOnClickListener {
            finish()
        }

        configLanguage()
    }

    private fun configLanguage() {
        val fcrTheme = SpUtil.getString(this,AppConstants.LOCALE_AREA)
        //设置radiobutton的选择状态
        when (fcrTheme) {
            "CN" -> {
                binding.rbLanguageZh.isChecked = true
            }
            "US" -> {
                binding.rbLanguageEn.isChecked = true
            }
//            "TW" -> {
//                binding.rbLanguageZhTw.isChecked = true
//            }
            else ->{
                binding.rbLanguageZh.isChecked = true
            }
        }

        binding.fcrRgLanguageSetting.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_language_zh -> {
                    changeLanguage("zh", "CN");
                }
                R.id.rb_language_en -> {
                    changeLanguage("en", "US");
                }
//                R.id.rb_language_zh_tw -> {
//                    changeLanguage("zh", "TW");
//                }
            }
        }
    }

    //修改应用内语言设置
    private fun changeLanguage(language: String, area: String) {
        if (TextUtils.isEmpty(language) && TextUtils.isEmpty(area)) {
            //如果语言和地区都是空，那么跟随系统
            SpUtil.saveString(this,AppConstants.LOCALE_LANGUAGE, "")
            SpUtil.saveString(this,AppConstants.LOCALE_AREA, "")
        } else {
            //不为空，那么修改app语言，并true是把语言信息保存到sp中，false是不保存到sp中
            val newLocale = Locale(language, area)
            MultiLanguageUtil.changeAppLanguage(this, newLocale, true)
            //重启app
            restartApp()
        }
    }

    private fun restartApp(){
        //重启app,这一步一定要加上，如果不重启app，可能打开新的页面显示的语言会不正确
        val intent = Intent(EduApplication.getContext(), FcrSplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        EduApplication.getContext().startActivity(intent)
        Process.killProcess(Process.myPid())
        System.exit(0)
    }
}