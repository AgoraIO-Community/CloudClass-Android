package io.agora.education.request

import android.text.TextUtils
import android.util.Log
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.base.http.TokenUtils
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.education.config.AppConstants
import io.agora.education.request.bean.FcrUserInfoRes

/**
 * author : hefeng
 * date : 2022/9/13
 * description : user info
 */
object AppUserInfoUtils {

    fun logout() {//退出登录，清除本地token
        PreferenceManager.putObject(AppHostUtil.KEY_SP_USER_INFO, "")
        TokenUtils.setLoginToken("", "")
    }

    fun deleteAccount(listener: (Boolean) -> Unit) {//注销，让远端token失效
        val call = AppRetrofitManager.instance().getService(AppService::class.java).logOutAccount()
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<String>>() {
            override fun onSuccess(result: HttpBaseRes<String>?) {
                LogX.e("deleteAccount", result.toString())
                ToastManager.showShort("注销成功")
                logout()
                listener.invoke(true)
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                LogX.e("deleteAccount", "$message(+$code+)")
                listener.invoke(false)
            }
        })
    }

    fun requestUserInfo(callback: HttpCallback<HttpBaseRes<FcrUserInfoRes>>? = null) {
        val call = AppRetrofitManager.instance().getService(AppService::class.java).getUserInfo()
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrUserInfoRes>>() {
            override fun onSuccess(result: HttpBaseRes<FcrUserInfoRes>?) {
                PreferenceManager.putObject(AppHostUtil.KEY_SP_USER_INFO, result?.data ?: "")
                // 设置默认昵称
                var fcrNickName = PreferenceManager.get(AppConstants.KEY_SP_NICKNAME, "")
                if (TextUtils.isEmpty(fcrNickName)) {
                    fcrNickName = AppUserInfoUtils.getUserInfo()?.userName ?: ""
                    PreferenceManager.put(AppConstants.KEY_SP_NICKNAME, fcrNickName)
                }
                callback?.onSuccess(result)
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                callback?.onError(httpCode,code, message)
                Log.e("AppUserInfoUtils", "$message($code)")
            }

            override fun onComplete() {
                super.onComplete()
                callback?.onComplete()
            }
        })
    }

    fun getUserInfo(): FcrUserInfoRes? {
        return PreferenceManager.getObject<FcrUserInfoRes>(AppHostUtil.KEY_SP_USER_INFO, FcrUserInfoRes::class.java)
    }

    fun getCompanyId(): String {
        return getUserInfo()?.companyId ?: ""
    }
}