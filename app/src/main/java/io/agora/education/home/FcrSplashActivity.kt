package io.agora.education.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.base.http.TokenUtils
import io.agora.agoraeducore.core.internal.base.network.FcrDomainManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.education.R
import io.agora.education.base.BaseActivity
import io.agora.education.config.AppConstants
import io.agora.education.databinding.ActivitySplashBinding
import io.agora.education.join.FcrQuickStartActivity
import io.agora.education.login.FcrLoginManager
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrIPRes
import io.agora.education.request.bean.FcrUserInfoRes
import io.agora.education.setting.FcrSettingTestActivity
import io.agora.education.utils.AppUtil
import io.agora.education.utils.FcrPrivateProtocolUtils


/**
 * 启动页
 */
class FcrSplashActivity : BaseActivity() {
    val TAG = "FcrSplashActivity"
    lateinit var binding: ActivitySplashBinding
    lateinit var loginManager: FcrLoginManager

    var region: String = ""
    var animator: ObjectAnimator? = null
    var handler = Handler(Looper.getMainLooper())
    var isAgreePrivateTerms = false
    var privacyTermsBottomDialog: FcrPrivacyTermsBottomDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        AppUtil.hideStatusBar(window, false)
        setContentView(binding.root)

        if (PreferenceManager.get(Constants.KEY_SP_IS_NOT_NEED_LOGIN, false)) {  // 不需要登录
            startActivity(Intent(this, FcrQuickStartActivity::class.java))
            finish()
            return
        }

        initViewData()
        lunchPageV2()
        setQATestPage()
        initPrivateTerms()
    }

    fun initPrivateTerms() {
        isAgreePrivateTerms = PreferenceManager.get(Constants.KEY_SP_PRIVACY_POLICY, false)
        binding.fcrImgCheck.isSelected = isAgreePrivateTerms

        binding.fcrTvPrivate.movementMethod = LinkMovementMethod.getInstance()
        binding.fcrTvPrivate.text = FcrPrivateProtocolUtils.getPrivateProtocol(this) {
            binding.fcrLayoutPrivate.performClick()
        }
        binding.fcrLayoutPrivate.setOnClickListener {
            isAgreePrivateTerms = !binding.fcrImgCheck.isSelected
            binding.fcrImgCheck.isSelected = isAgreePrivateTerms
            binding.fcrTips.visibility = View.GONE
        }
    }

    fun setPrivateTerms() {
        val isFirst = PreferenceManager.get(Constants.KEY_SP_PRIVACY_POLICY_FIRST, false)
        privacyTermsBottomDialog?.dismiss()
        if (!isFirst) { // 首次显示
            privacyTermsBottomDialog = FcrPrivacyTermsBottomDialog.newInstance(this)
            privacyTermsBottomDialog?.onAgreeListener = {
                if (it) {
                    isAgreePrivateTerms = true
                    binding.fcrTips.visibility = View.GONE
                    binding.fcrImgCheck.isSelected = true
                    PreferenceManager.put(Constants.KEY_SP_PRIVACY_POLICY_FIRST, true)
                } else {
                    // 再次弹窗
                    FcrPrivateProtocolUtils.showAgreeDialog(this) { isAgree ->
                        if (isAgree) {
                            binding.fcrImgCheck.isSelected = true
                            binding.fcrTips.visibility = View.GONE
                            isAgreePrivateTerms = true
                            PreferenceManager.put(Constants.KEY_SP_PRIVACY_POLICY_FIRST, true)
                        } else {
                            finish()
                        }
                    }
                    // finish()
                }
            }
            privacyTermsBottomDialog?.show()
        }

        if (!isAgreePrivateTerms) {
            handler.removeCallbacksAndMessages(null)
            binding.fcrTips.visibility = View.VISIBLE
            handler.postDelayed({
                binding.fcrTips.visibility = View.GONE
            }, 2000)
        }
    }

    fun initViewData() {
        loginManager = FcrLoginManager(loadingView, this)
        binding.btnRegLogin.setOnClickListener {
            if (!isAgreePrivateTerms) { // show tips
                val shake: Animation = AnimationUtils.loadAnimation(this, R.anim.fcr_input_shake)
                binding.fcrLayoutPrivate.startAnimation(shake)
                handler.removeCallbacksAndMessages(null)
                binding.fcrTips.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // 是否登录
            if (isLogin()) {
                checkTokenAndGetUserInfo {
                    startActivity(Intent(this, FcrHomeActivity::class.java))
                    finish()
                }
            } else {
                loginManager.startLogin()
            }
        }
    }


    fun isLogin(): Boolean {
        return AppUserInfoUtils.isVerLogin()
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
                        Log.e(TAG, "$message($code)")
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val width: Int = binding.root.measuredWidth
            val height: Int = binding.root.measuredHeight

            val imgW = binding.ivLight.measuredWidth
            val imgH = binding.ivLight.measuredHeight

            //Log.e("splash", "width=$width height=$height")

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

    /**
     * 1、请求是否需要登录
     * 2、获取是否设置过区域
     * 3、获取是否同意隐私协议
     * 4、获取是否登录
     */
    fun lunchPageV2() {
        region = PreferenceManager.get(AppConstants.KEY_SP_REGION, "")
        if (TextUtils.isEmpty(region)) {
            FcrDomainManager.initDomain(region)
        } else {
            FcrDomainManager.initDomain(AgoraEduRegion.cn)
        }

        loadingView.show()
        val call = AppRetrofitManager.instance().getService(AppService::class.java).requestIP()
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrIPRes>>() {
            override fun onSuccess(result: HttpBaseRes<FcrIPRes>?) {
                //result?.data?.loginType = 0  // TEST

                if (result?.data?.loginType == 1) { // 国内，登录
                    AppUtil.isChina = true
                    if (TextUtils.isEmpty(region)) {
                        region = AgoraEduRegion.cn
                        PreferenceManager.put(AppConstants.KEY_SP_REGION, region)
                        FcrDomainManager.initDomain(region)
                    }
                    lunchLogin()
                } else { // 国外，免登录
                    AppUtil.isChina = false
                    if (TextUtils.isEmpty(region)) {
                        region = AgoraEduRegion.na
                        PreferenceManager.put(AppConstants.KEY_SP_REGION, region)
                        FcrDomainManager.initDomain(region)
                    }
                    lunchQuickStart()
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                AppUtil.setDefaultRegin(this@FcrSplashActivity)
                lunchLogin()
            }

            override fun onComplete() {
                super.onComplete()
                loadingView.dismiss()
            }
        })
    }

    fun lunchLogin() {
        val isAgreePolicy = PreferenceManager.get(Constants.KEY_SP_PRIVACY_POLICY_FIRST, false)
        if (isAgreePolicy && isLogin()) { // 同意隐私政策
            startActivity(Intent(this@FcrSplashActivity, FcrHomeActivity::class.java))
            finish()
        } else {  // 未同意隐私政策
            setPrivateTerms()
        }
    }

    fun lunchQuickStart() {
        if (isLogin()) { // 课前
            startActivity(Intent(this@FcrSplashActivity, FcrHomeActivity::class.java))
        } else {        // 免登录页面
            startActivity(Intent(this@FcrSplashActivity, FcrQuickStartActivity::class.java))
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingView.dismiss()
        animator?.cancel()
        handler.removeCallbacksAndMessages(null)
        privacyTermsBottomDialog?.dismiss()
    }
}