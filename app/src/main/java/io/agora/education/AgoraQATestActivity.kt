package io.agora.education

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.education.databinding.ActivityQaTestBinding

/**
 * author : hefeng
 * date : 2022/3/3
 * description : QA 测试页面
 */
class AgoraQATestActivity : AppCompatActivity() {
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

        binding.agoraTestMode.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.put(Constants.KEY_SP_USE_OPEN_TEST_MODE, isChecked)
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
            EduApplication.setAppId(null)
        }
    }
}