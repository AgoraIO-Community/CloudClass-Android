package io.agora.education.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.base.http.TokenUtils
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.education.BuildConfig
import io.agora.education.base.BaseActivity
import io.agora.education.config.AppConstants
import io.agora.education.databinding.ActivitySplashBinding
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrRedirectUrlReq
import io.agora.education.request.bean.FcrUserInfoRes
import io.agora.education.setting.FcrMainActivity
import io.agora.education.setting.FcrSettingTestActivity
import io.agora.education.utils.AppUtil


/**
 * 启动页
 */
class FcrSplashActivity : BaseActivity() {
    lateinit var binding: ActivitySplashBinding
    var regionStr: String = ""
    var atyResultLauncher: ActivityResultLauncher<Intent>? = null
    var animator: ObjectAnimator? = null
    val TAG = "FcrSplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.IS_NOT_NEED_LOGIN || PreferenceManager.get(Constants.KEY_SP_IS_NOT_NEED_LOGIN, false)) {
            // 不需要登录
            startActivity(Intent(this, FcrMainActivity::class.java))
            finish()
            return
        }

        initViewData()
        setRegisterForActivityResult()
        showPrivacyTerms()
        setQATestPage()
        checkTokenAndGetUserInfo()
    }

    fun initViewData() {
        AppUtil.hideStatusBar(window, false)

        binding.btnJoin.setOnClickListener {
            if (BuildConfig.IS_NOT_NEED_LOGIN || PreferenceManager.get(Constants.KEY_SP_IS_NOT_NEED_LOGIN, false)) {
                // 不需要登录
                startActivity(Intent(this, FcrMainActivity::class.java))
                finish()
            } else {
                // 需要登录
                if (isExistTokenAndUserInfo()) {
                    checkTokenAndGetUserInfo {
                        startActivity(Intent(this, FcrHomeActivity::class.java))
                        finish()
                    }
                } else {//accessToken 过期
                    loadLoginPage()
                }
            }
        }
    }

    fun isExistTokenAndUserInfo(): Boolean {
        return TokenUtils.isExistLoginToken() && AppUserInfoUtils.getUserInfo() != null
    }

    fun checkTokenAndGetUserInfo(isNextListener: ((Boolean) -> Unit)? = null) {
        if (TokenUtils.isExistLoginToken()) {
            TokenUtils.setHttpRequestToken()

            if (AppUserInfoUtils.getUserInfo() == null) {
                loadingView.show()
                AppUserInfoUtils.requestUserInfo(object : HttpCallback<HttpBaseRes<FcrUserInfoRes>>() {
                    override fun onSuccess(result: HttpBaseRes<FcrUserInfoRes>?) {
                        isNextListener?.invoke(true)
                    }

                    override fun onError(httpCode: Int, code: Int, message: String?) {
                        if (httpCode != 401) {
                            ToastManager.showShort("$message($code)")
                        }
                        Log.e(TAG,"$message($code)")
                        isNextListener?.invoke(false)
                    }

                    override fun onComplete() {
                        super.onComplete()
                        loadingView.dismiss()
                    }
                })
            } else {
                AppUserInfoUtils.requestUserInfo()
                isNextListener?.invoke(true)
            }
        }
    }

    private fun setRegisterForActivityResult() {
        atyResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isLogin = result.data?.getBooleanExtra("isLogin", false)
                if (isLogin == true) {
                    loadingView.show()
                    TokenUtils.setHttpRequestToken()
                    AppUserInfoUtils.requestUserInfo(object : HttpCallback<HttpBaseRes<FcrUserInfoRes>>() {
                        override fun onSuccess(result: HttpBaseRes<FcrUserInfoRes>?) {
                            startActivity(Intent(this@FcrSplashActivity, FcrHomeActivity::class.java))
                            finish()
                        }
                        override fun onComplete() {
                            super.onComplete()
                            loadingView.dismiss()
                        }
                    })
                }
            }

        }
        regionStr = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
    }

    private fun setQATestPage() {
        binding.frame.setOnClickListener(object : View.OnClickListener {
            val COUNTS = 5 //点击次数
            val DURATION = (3 * 1000).toLong() //规定有效时间
            var mHits = LongArray(COUNTS)
            override fun onClick(v: View) {
                /**
                 * 实现双击方法
                 * src 拷贝的源数组
                 * srcPos 从源数组的那个位置开始拷贝.
                 * dst 目标数组
                 * dstPos 从目标数组的那个位子开始写数据
                 * length 拷贝的元素的个数
                 */
                System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
                //实现左移，然后最后一个位置更新距离开机的时间，如果最后一个时间和最开始时间小于DURATION，即连续5次点击
                mHits[mHits.size - 1] = SystemClock.uptimeMillis()
                if (mHits[0] >= SystemClock.uptimeMillis() - DURATION) {
                    startActivity(Intent(this@FcrSplashActivity, FcrSettingTestActivity::class.java))
                }
            }
        })
    }

    private fun showPrivacyTerms() {
        val key = "PrivacyTerms"
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!sharedPreferences.getBoolean(key, false)) {
            val dialog = FcrPrivacyTermsDialog(this)
            dialog.setPrivacyTermsDialogListener(object : FcrPrivacyTermsDialog.OnPrivacyTermsDialogListener {
                override fun onPositiveClick() {
                    sharedPreferences.edit().putBoolean(key, true).apply()
                    dialog.dismiss()
                    //loadLoginPage()
                }

                override fun onNegativeClick() {
                    dialog.dismiss()
                    finish()
                }
            })
            dialog.show()
        } else {
            //loadLoginPage()
        }
    }

    fun loadLoginPage() {
        loadingView.show()

        // 如果非中文，设置默认为na区域和英文
        val language = SpUtil.getString(applicationContext, AppConstants.LOCALE_LANGUAGE, "cn")
        var r = "en"
        if (language.equals("cn", true)) {
            r = "cn"
        }
        val fcrRedirectUrlReq = FcrRedirectUrlReq(r)
        val call = AppRetrofitManager.instance()
            .getService(AppHostUtil.getAppHostUrl(regionStr.lowercase()), AppService::class.java)
            .getAuthWebPage(fcrRedirectUrlReq)
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<String>>() {
            override fun onSuccess(result: HttpBaseRes<String>?) {
                result?.data?.let { webViewUrl ->
                    val intent = Intent(this@FcrSplashActivity, FcrLoginActivity::class.java)
                    intent.putExtra(LOGIN_URL_KEY, webViewUrl)
//                    startActivity(intent)
                    atyResultLauncher?.launch(intent)
                }
            }

            override fun onComplete() {
                super.onComplete()
                loadingView.dismiss()
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val width: Int = binding.root.measuredWidth
            val height: Int = binding.root.measuredHeight

            val imgW = binding.ivLight.measuredWidth
            val imgH = binding.ivLight.measuredHeight

            //Log.e("hefeng", "width=$width height=$height")

            val path = Path()
            path.moveTo(-imgW * 2 / 3f, -imgH * 2 / 3f)
            path.lineTo(width * 1f - imgW / 2f, height * 1 / 3f)
            path.lineTo(-imgW / 2f, height * 2 / 3f)
            path.lineTo(width * 1f - imgW / 3f, height * 1f - imgH / 3f)

            animator = ObjectAnimator.ofFloat(binding.ivLight, "translationX", "translationY", path)
            animator?.duration = 10000
            animator?.repeatCount = -1
            animator?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animator?.cancel()
    }
}