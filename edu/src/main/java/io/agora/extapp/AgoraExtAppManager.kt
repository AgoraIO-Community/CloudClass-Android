package io.agora.extapp

import android.content.Context
import android.util.Log
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import io.agora.base.network.RetrofitManager
import io.agora.edu.classroom.bean.PropertyData
import io.agora.edu.common.bean.ResponseBody
import io.agora.edu.launch.AgoraEduSDK
import io.agora.edu.util.TimeUtil
import io.agora.educontext.EduContextPool
import io.agora.extension.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class AgoraExtAppManager(
        private val appId: String,
        private val context: Context,
        private val container: RelativeLayout,
        private val roomUuid: String,
        eduContext: EduContextPool) : IAgoraExtAppAPaaSEntry {

    private val tag = "AgoraExtAppManager"
    private val keyAppProperties = "extApps"
    private val keyAppCommon = "extAppsCommon"

    private val extAppEngine = AgoraExtAppEngine(context, container, eduContext, this)

    fun launchExtApp(identifier: String, currentTime: Long) : Int {
        return extAppEngine.launchExtApp(identifier,false,currentTime)
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

    fun handleExtAppPropertyInitialized(roomProperties: MutableMap<String, Any>?) {
        (roomProperties?.get(keyAppProperties) as? Map<String, Any?>)?.let { appMap ->
            appMap.forEach { (s, any) ->
                run {
                    Log.d(tag, "ext app initialize id $s, ${any.toString()}")
                    val state = (roomProperties?.get(keyAppCommon) as Map<String, Any>)?.get(s)
                    val currentTime = TimeUtil.currentTimeMillis()
                    updateExtAppProperties(s, any as? MutableMap<String, Any?>,
                            null, state as? MutableMap<String, Any?>,currentTime)
                }
            }

        }
    }

    fun handleRoomPropertiesChange(roomProperties: MutableMap<String, Any>?, cause: MutableMap<String, Any>?) {
        roomProperties ?: return

        // Server will compose a "cause" from which extension
        // app has caused this change event.
        cause?.get(PropertyData.CMD)?.let {
            val cmd = it.toString().toFloat().toInt()
            if (cmd != PropertyData.EXTAPP_CHANGED) {
                return
            }

            val dataMap = cause[PropertyData.DATA] as? MutableMap<*, *>
            dataMap?.let { data ->
                // This event contains the changed properties (in extAppCause)
                // of an extension app (denoted by extAppId)
                val extAppId = data["extAppUuid"] as? String
                val extAppCause = data["extAppCause"] as? MutableMap<String, Any?>
                val currentTime = TimeUtil.currentTimeMillis()
                // One property change event contains only the change info of one
                // single extension app, we must find the common info and properties
                // of this extension app and callback to it
                val stateMap = ((roomProperties[keyAppCommon] as? MutableMap<*, *>)
                        ?.get(extAppId)) as? MutableMap<String, Any?>
                val extAppProperties = ((roomProperties[keyAppProperties] as? MutableMap<*, *>)
                        ?.get(extAppId)) as? MutableMap<String, Any?>

                Log.d(tag, "handle extension app changed: $extAppId, properties: " +
                        "$extAppProperties, state map: $stateMap")

                extAppId?.let { id ->
                    updateExtAppProperties(id, extAppProperties, extAppCause, stateMap,currentTime)
                }
            }
        }
    }

    private fun updateExtAppProperties(appIdentifier: String,
                                       properties: MutableMap<String, Any?>?,
                                       cause: MutableMap<String, Any?>?,
                                       state: MutableMap<String, Any?>?, currentTime: Long) {
        extAppEngine.onExtAppPropertyUpdated(appIdentifier, properties, cause, state, currentTime)
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
    override fun updateProperties(identifier: String,
                                  properties: MutableMap<String, Any?>?,
                                  cause: MutableMap<String, Any?>?,
                                  common: MutableMap<String, Any?>?,
                                  callback: AgoraExtAppCallback<String>?) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(),
                AgoraExtAppService::class.java)
                .setProperties(appId, roomUuid, identifier,
                        AgoraExtAppUpdateRequest(properties, cause, common))
                .enqueue(object : Callback<ResponseBody<String>> {
                    override fun onResponse(call: Call<ResponseBody<String>>,
                                            response: Response<ResponseBody<String>>) {
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