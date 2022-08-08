package io.agora.education.config


import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpErrorRes
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.education.BuildConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

object ConfigUtil {
    /**
     * 预检
     */
    private const val regionUrlFormat = "https://api-solutions.%s.agoralab.co/"
    private const val regionPreUrlFormat = "https://api-solutions-pre.%s.agoralab.co/"
    private const val regionDevUrlFormat = "https://api-solutions-dev.%s.agoralab.co/"

    fun getConfig(url: String, roomUuid: String, role: Int, userId: String, callback: EduCallback<ConfigData>) {
        RetrofitManager.instance().getService(url, ConfigService::class.java)
            .getConfig(roomUuid, role, userId).enqueue(object : Callback<ConfigResponse> {
                override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                    if (response.body() == null) {
                        val error = response.errorBody()?.string()
                        if (error != null) {
                            val info = GsonUtil.parseToObject(error, HttpErrorRes::class.java)
                            callback.onFailure(EduError(info?.status ?: -1, info?.error ?: "response body is null"))
                        } else {
                            callback.onFailure(EduError(-1, "response body is null"))
                        }
                    } else {
                        if (response.isSuccessful) {
                            callback.onSuccess(response.body()?.data)
                        } else {
                            callback.onFailure(EduError(response.body()!!.code, response.body()!!.msg))
                        }
                    }
                }

                override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                    callback.onFailure(EduError(-1, t.message ?: ""))
                }
            })
    }

    fun config(url: String, userId: String, callback: EduCallback<ConfigData>) {
        RetrofitManager.instance().getService(url, ConfigService::class.java)
                .config(userId).enqueue(object : Callback<ConfigResponse> {
                    override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                        response.body()?.let {
                            if (it.code == 0) {
                                callback.onSuccess(it.data)
                            } else {
                                callback.onFailure(EduError(it.code, it.msg))
                            }
                        }
                    }

                    override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                        callback.onFailure(EduError(-1, t.message ?: ""))
                    }
            })
    }

    fun getRegionUrl(region: String?): String {
        val server = when (region?.toLowerCase(Locale.ROOT)) {
            "cn" -> "bj2"
            "ap" -> "sg3sbm"
            "na" -> "sv3sbm"
            "eu" -> "fr3sbm"
            else -> "bj2"
        }

        if (AgoraEduSDK.agoraEduEnv == AgoraEduEnv.ENV) {
            return String.format(regionDevUrlFormat, server)
        } else if (AgoraEduSDK.agoraEduEnv == AgoraEduEnv.PRE) {
            return String.format(regionPreUrlFormat, server)
        }
        return String.format(regionUrlFormat, server)
    }
}