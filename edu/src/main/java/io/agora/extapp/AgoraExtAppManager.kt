package io.agora.extapp

import android.content.Context
import android.view.View
import androidx.annotation.UiThread
import io.agora.base.network.RetrofitManager
import io.agora.edu.classroom.bean.PropertyCauseType
import io.agora.edu.common.bean.ResponseBody
import io.agora.edu.launch.AgoraEduSDK
import io.agora.extension.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class AgoraExtAppManager(
        private val appId: String,
        private val context: Context,
        private val container: View,
        private val roomUuid: String) : IAgoraExtAppAPaaSEntry {

    private val extAppEngine = AgoraExtAppEngine(context, container, this)

    fun launchExtApp(identifier: String) : Int {
        return extAppEngine.launchExtApp(identifier)
    }

    fun getRegisteredApps() : List<AgoraExtAppInfo> {
        return extAppEngine.getRegisteredExtAppInfoList()
    }

    fun handleRoomInfoChange(roomInfo: AgoraExtAppRoomInfo) {
        extAppEngine.onRoomInfoChanged(roomInfo)
    }

    fun handleLocalUserChange(userInfo: AgoraExtAppUserInfo) {
        extAppEngine.onLocalUserChanged(userInfo)
    }

    fun handleRoomPropertiesChange(roomProperties: MutableMap<String, Any>?, cause: MutableMap<String, Any>?) {
        roomProperties ?: return
        val extAppCauseMap = mutableMapOf<String, MutableMap<String, Any?>?>()
        var extAppsInfo : MutableMap<String, MutableMap<String, Any>> ?=null
        try{
            extAppsInfo = roomProperties["extApps"] as? MutableMap<String, MutableMap<String, Any>>
            cause?.get(PropertyCauseType.CMD)?.let {
                if (it.toString().toFloat().toInt() == PropertyCauseType.EXTAPP_CHANGED) {
                    val extAppUuid = (cause.get(PropertyCauseType.DATA) as MutableMap<String, Any>)["extAppUuid"] as String
                    val extAppCause = (cause.get(PropertyCauseType.DATA) as MutableMap<String, Any>)["extAppCause"]
                    extAppCauseMap[extAppUuid] = extAppCause as? MutableMap<String, Any?>
                }
            }
        } catch (e:Exception) {
            // do nothing
        }

        extAppsInfo?.let {
            for (appId in it.keys) {
                updateExtAppProperties(appId, extAppsInfo[appId], extAppCauseMap[appId])
            }
        }
    }

    private fun updateExtAppProperties(appIdentifier: String,
                                       properties: MutableMap<String, Any>?,
                                       cause: MutableMap<String, Any?>?) {
        extAppEngine.onExtAppPropertyUpdated(appIdentifier, properties, cause)
    }

    @UiThread
    fun dispose() {
        extAppEngine.dispose()
    }

    override fun getProperties(identifier: String): MutableMap<String, Any?>? {
        return extAppEngine.getExtAppProperties(identifier)
    }

    /**
     * Usually triggered inside an extension app, change and sync current app
     * state to remote users
     * @param identifier app id, may be transformed if containing dots before calling
     */
    override fun updateProperties(identifier: String, properties: MutableMap<String, Any?>?,
                                  cause: MutableMap<String, Any?>?, callback: AgoraExtAppCallback<String>?) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(),
                AgoraExtAppService::class.java).setProperties(appId, roomUuid, identifier,
                AgoraExtAppUpdateRequest(properties, cause)).enqueue(object : Callback<ResponseBody<String>> {
            override fun onResponse(call: Call<ResponseBody<String>>, response: Response<ResponseBody<String>>) {
                response.body()?.data?.let {
                    callback?.onSuccess(it)
                }
            }

            override fun onFailure(call: Call<ResponseBody<String>>, t: Throwable) {
                callback?.onFail(t)
            }
        })
    }

    /**
     * Usually triggered inside an extension app, change and sync current app
     * state to remote users
     * @param identifier app id, may be transformed if containing dots before calling
     */
    override fun deleteProperties(identifier: String, propertyKeys: MutableList<String>,
                                  cause: MutableMap<String, Any?>?, callback: AgoraExtAppCallback<String>?) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(),
            AgoraExtAppService::class.java).deleteProperties(appId, roomUuid, identifier,
                AgoraExtAppDeleteRequest(propertyKeys, cause)).enqueue(object : Callback<ResponseBody<String>> {
            override fun onResponse(call: Call<ResponseBody<String>>, response: Response<ResponseBody<String>>) {
                response.body()?.data?.let {
                    callback?.onSuccess(it)
                }
            }

            override fun onFailure(call: Call<ResponseBody<String>>, t: Throwable) {
                callback?.onFail(t)
            }
        })
    }
}