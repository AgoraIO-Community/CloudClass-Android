package io.agora.extension

import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.UiThread

abstract class AgoraExtAppBase : IAgoraExtApp {
    private val tag = "AgoraExtAppBase"
    protected var engine: AgoraExtAppEngine? = null
    protected var extAppContext: AgoraExtAppContext? = null

    internal fun init(identifier: String, engine: AgoraExtAppEngine) {
        this.engine = engine

        engine.getRegisteredExtApp(identifier)?.let { item ->
            AgoraExtAppContext(
                    engine.aPaaSEntry.getRoomInfo(),
                    engine.aPaaSEntry.getLocalUserInfo(),
                    mutableMapOf(), identifier, item.language).let { context ->
                        extAppContext = context
                        engine.aPaaSEntry.getProperties(identifier)?.let { properties ->
                            context.properties.putAll(properties)
                        }
            }
        }
    }

    /**
     * set properties to remote service, and every extApp impl will
     * receive the properties in onPropertiesUpdated callback.
     *
     * @param properties the properties map.The key separating by '.' will split to
     *      multi key of the map when received in nPropertiesUpdated callback.
     * @param cause the reason why properties changed.
     */
    fun updateProperties(properties: MutableMap<String, Any?>,
                         cause: MutableMap<String, Any?>,
                         callback: AgoraExtAppCallback<String>? = null) {
        extAppContext?.let { context ->
            engine?.updateExtAppProperties(context.appIdentifier, properties, cause, callback)
            return@updateProperties
        }

        Log.w(tag, "agora extension app engine does not initialize")
    }

    /**
     * delete properties which set by updateProperties method. The properties after deleting will be
     * received by every extApp impl in onPropertiesUpdated callback.
     *
     * @param propertyKeys the keys of properties.
     * @param cause the reason why properties deleted.
     */
    fun deleteProperties(propertyKeys: MutableList<String>,
                         cause: MutableMap<String, Any?>,
                         callback: AgoraExtAppCallback<String>?) {
        extAppContext?.let { context ->
            engine?.deleteExtAppProperties(context.appIdentifier, propertyKeys, cause, callback)
            return@deleteProperties
        }

        Log.w(tag, "agora extension app engine does not initialize")
    }

    /**
     * finish the extApp and then close it
     */
    @UiThread
    fun unload() {
        extAppContext?.let { context ->
            engine?.stopExtApp(context.appIdentifier)
            return@unload
        }

        Log.w(tag, "agora extension app engine does not initialize the context")
    }

    fun getLocalUserInfo(): AgoraExtAppUserInfo? {
        return extAppContext?.localUserInfo
    }

    fun getRoomInfo(): AgoraExtAppRoomInfo? {
        return extAppContext?.roomInfo
    }

    fun getProperties(): MutableMap<String, Any?>? {
        return extAppContext?.properties
    }

    // extAppContext info updated callback
    open fun onRoomInfoUpdate(roomInfo: AgoraExtAppRoomInfo) {

    }

    open fun onLocalUserInfoUpdate(userInfo: AgoraExtAppUserInfo) {

    }

    open fun onPropertiesUpdate(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?) {

    }

    // update extAppContext info
    internal fun updateLocalUserInfo(userInfo: AgoraExtAppUserInfo) {
//        extAppContext.localUserInfo = AgoraExtAppUserInfo(
//                userInfo.userUuid, userInfo.userName, userInfo.userRole)
//        onLocalUserInfoUpdate(userInfo)
    }

    internal fun updateProperties(properties: MutableMap<String, Any>?,
                                  cause: MutableMap<String, Any>?) {
//        extAppContext.properties.clear()
//        properties?.let {
//            extAppContext.properties.putAll(properties)
//        }
//        onPropertiesUpdate(extAppContext.properties, cause)
    }

    internal fun updateRoomInfo(roomInfo: AgoraExtAppRoomInfo) {
//        extAppContext.roomInfo = AgoraExtAppRoomInfo(roomInfo.roomUuid, roomInfo.roomName, roomInfo.roomType)
//        onRoomInfoUpdate(roomInfo)
    }
}

data class AgoraExtAppContext(
        var roomInfo: AgoraExtAppRoomInfo,
        var localUserInfo: AgoraExtAppUserInfo,
        val properties: MutableMap<String, Any?>,
        val appIdentifier: String,
        val language: String)