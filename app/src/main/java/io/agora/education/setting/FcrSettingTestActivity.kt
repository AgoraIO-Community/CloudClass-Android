package io.agora.education.setting

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.util.UnCatchExceptionHandler.Companion.getExceptionHandler
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.utils.SkinUtils
import io.agora.education.BuildConfig
import io.agora.education.R
import io.agora.education.databinding.ActivityQaTestBinding
import io.agora.education.join.FcrQuickStartActivity

/**
 * author : felix
 * date : 2022/3/3
 * description : QA 测试页面
 */
class FcrSettingTestActivity : AppCompatActivity() {
    lateinit var binding: ActivityQaTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQaTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setVersion()
        setEnv()
    }

    fun setVersion() {
        val isDebug = if (BuildConfig.DEBUG) "Debug" else "Release"
        binding.agoraTestVersion.text = "SDK 版本号：${AgoraEduSDK.version()} ( $isDebug )"
        binding.agoraTestBack.setOnClickListener { finish() }
    }

    fun setEnv() {
        val env = PreferenceManager.getObject<AgoraEduEnv>(Constants.KEY_SP_ENV, AgoraEduEnv::class.java)
        binding.agoraTestMode.isChecked = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false)
        binding.agoraNightMode.isChecked = PreferenceManager.get(Constants.KEY_SP_NIGHT, false)
        binding.agoraEnterRoom.isChecked = PreferenceManager.get(Constants.KEY_SP_IS_NOT_NEED_LOGIN, false)

        binding.agoraEnterRoom.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.put(Constants.KEY_SP_IS_NOT_NEED_LOGIN, isChecked)
        }

        binding.agoraTestMode.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.put(Constants.KEY_SP_USE_OPEN_TEST_MODE, isChecked)

            if (isChecked) {
                getExceptionHandler().init(this.applicationContext, "io.agora")
            }
        }

        binding.agoraNightMode.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.put(Constants.KEY_SP_NIGHT, isChecked)
            SkinUtils.setNightMode(isChecked)
        }

        binding.agoraFastRoom.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.put(Constants.KEY_SP_FAST, isChecked)
        }

        binding.agoraTestRoom.setOnClickListener {
            startActivity(Intent(this, FcrMainActivity::class.java))
        }

        binding.agoraStart.setOnClickListener {
            startActivity(Intent(this, FcrQuickStartActivity::class.java))
        }

        when (env) {
            AgoraEduEnv.PROD -> {
                PreferenceManager.put(Constants.KEY_SP_ENV, AgoraEduEnv.PROD)
                binding.agoraTestEnvProd.isChecked = true
            }

            AgoraEduEnv.PRE -> {
                PreferenceManager.put(Constants.KEY_SP_ENV, AgoraEduEnv.PRE)
                binding.agoraTestEnvPre.isChecked = true
            }

            AgoraEduEnv.ENV -> {
                PreferenceManager.put(Constants.KEY_SP_ENV, AgoraEduEnv.ENV)
                binding.agoraTestEnvDev.isChecked = true
            }
        }

        binding.agoraTestEnv.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.agora_test_env_dev -> {
                    PreferenceManager.putObject(Constants.KEY_SP_ENV, AgoraEduEnv.ENV)
                }

                R.id.agora_test_env_prod -> {
                    PreferenceManager.putObject(Constants.KEY_SP_ENV, AgoraEduEnv.PROD)
                }

                R.id.agora_test_env_pre -> {
                    PreferenceManager.putObject(Constants.KEY_SP_ENV, AgoraEduEnv.PRE)
                }
            }

            val env = PreferenceManager.getObject<AgoraEduEnv>(Constants.KEY_SP_ENV, AgoraEduEnv::class.java)
            if (env != null) {
                AgoraEduSDK.agoraEduEnv = env
            }
        }
    }
}