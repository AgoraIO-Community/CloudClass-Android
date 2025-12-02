package io.agora.education.login

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.base.http.TokenUtils
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.education.config.AppConstants
import io.agora.education.home.FcrHomeActivity
import io.agora.education.home.FcrLoginActivity
import io.agora.education.home.LOGIN_URL_KEY
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrRedirectUrlReq
import io.agora.education.request.bean.FcrUserInfoRes

/**
 * author : felix
 * date : 2023/8/8
 * description :
 */
class FcrLoginManager(var loadingView: AgoraLoadingDialog, var context: AppCompatActivity) {
    var atyResultLauncher =
        context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val isLogin = result.data?.getBooleanExtra("isLogin", false)
                if (isLogin == true) {
                    loadingView.show()
                    TokenUtils.setHttpRequestToken()
                    AppUserInfoUtils.requestUserInfo(object : HttpCallback<HttpBaseRes<FcrUserInfoRes>>() {
                        override fun onSuccess(result: HttpBaseRes<FcrUserInfoRes>?) {
                            PreferenceManager.put(Constants.KEY_SP_PRIVACY_POLICY, true)
                            context.startActivity(Intent(context, FcrHomeActivity::class.java))
                            context.finish()
                        }

                        override fun onComplete() {
                            super.onComplete()
                            loadingView.dismiss()
                        }
                    })
                }
            }
        }


    fun startLogin() {
         loadingView.show()
        val regionStr = PreferenceManager.get(AppConstants.KEY_SP_REGION, "")
        // 如果非中文，设置默认为na区域和英文
        val language = SpUtil.getString(context.applicationContext, AppConstants.LOCALE_LANGUAGE, "cn")
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
                    val intent = Intent(context, FcrLoginActivity::class.java)
                    intent.putExtra(LOGIN_URL_KEY, webViewUrl)
                    atyResultLauncher?.launch(intent)
                }
            }

            override fun onComplete() {
                super.onComplete()
                loadingView.dismiss()
            }
        })
    }
}