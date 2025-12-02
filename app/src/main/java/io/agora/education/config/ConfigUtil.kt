package io.agora.education.config


import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError

object ConfigUtil {
    /**
     * 免登录
     */
    fun getV3Config(url: String, roomUuid: String, role: Int, userId: String, callback: EduCallback<ConfigData>) {
        val call = AppRetrofitManager.instance().getService(url, ConfigService::class.java).getConfigV3(roomUuid, role, userId)
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<ConfigData>>() {
            override fun onSuccess(result: HttpBaseRes<ConfigData>?) {
                callback.onSuccess(result?.data)
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                callback.onFailure(EduError(code, message ?: "error"))
            }
        })
    }

    /**
     * 需要登录
     */
    fun getV4Config(url: String, roomUuid: String, role: Int, userId: String, callback: EduCallback<ConfigData>) {
        val call = AppRetrofitManager.instance().getService(url, ConfigService::class.java).getConfigV4(roomUuid, role, userId)
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<ConfigData>>() {
            override fun onSuccess(result: HttpBaseRes<ConfigData>?) {
                callback.onSuccess(result?.data)
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                callback.onFailure(EduError(code, message ?: "error"))
            }
        })
    }
}