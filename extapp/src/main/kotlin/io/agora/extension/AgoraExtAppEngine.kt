package io.agora.extension

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup

class AgoraExtAppEngine(
        private var context: Context,
        private var container: View,
        internal var aPaaSEntry: IAgoraExtAppAPaaSEntry) {

    private val tag = "AgoraExtAppEngine"
    private val launchedExtAppList = mutableListOf<AgoraExtAppItem>()
    private val launchedExtAppMap = mutableMapOf<String, AgoraExtAppItem>()
    private val launchedExtAppMapTransformed = mutableMapOf<String, AgoraExtAppItem>()

    companion object {
        private val registeredAppList = ArrayList<AgoraExtAppItem>()
        private val registeredAppMap = mutableMapOf<String, AgoraExtAppItem>()

        fun registerExtAppList(apps: MutableList<AgoraExtAppConfiguration>) {
            apps.forEach { config ->
                registerExtApp(config)
            }
        }

        fun getRegisteredExtApps(): List<AgoraExtAppInfo> {
            val list = mutableListOf<AgoraExtAppInfo>()
            registeredAppList.forEach { item ->
                list.add(AgoraExtAppInfo(
                        item.appIdentifier,
                        item.language,
                        item.imageResource))
            }

            return list
        }

        fun registerExtApp(config: AgoraExtAppConfiguration) {
            if (!registeredAppMap.containsKey(config.appIdentifier)) {
                val item = AgoraExtAppItem(
                        config.appIdentifier,
                        formatExtAppIdentifier(config.appIdentifier),
                        config.layout,
                        config.extAppClass,
                        config.language,
                        config.imageResource)
                registeredAppList.add(item)
                registeredAppMap[item.appIdentifier] = item
            }
        }

        /**
         * Replace invalid characters in app identifiers
         */
        private fun formatExtAppIdentifier(id: String): String {
            return id.replace(".", "_")
        }
    }

    fun getRegisteredExtAppInfoList(): List<AgoraExtAppInfo> {
        val list = mutableListOf<AgoraExtAppInfo>()
        registeredAppList.forEach { config ->
            list.add(AgoraExtAppInfo(
                    config.appIdentifier,
                    config.language,
                    config.imageResource))
        }
        return list
    }

    internal fun getRegisteredExtApp(identifier: String): AgoraExtAppItem? {
        return registeredAppMap[identifier]
    }

    fun getLaunchedExtAppInfoList(): List<AgoraExtAppInfo> {
        val list = mutableListOf<AgoraExtAppInfo>()
        launchedExtAppList.forEach { item ->
            list.add(AgoraExtAppInfo(
                    item.appIdentifier,
                    item.language,
                    item.imageResource))
        }
        return list
    }

    @Synchronized fun launchExtApp(identifier: String): Int {
        if (launchedExtAppMap.containsKey(identifier)) {
            Log.w(tag, "launch ext app: app $identifier has been launched")
            return AgoraExtAppErrorCode.ExtAppIdDuplicated
        }

        val item = registeredAppMap[identifier]
        if (item == null) {
            Log.w(tag, "launch ext app: app $identifier not found, have you registered this app?")
            return AgoraExtAppErrorCode.ExtAppIdNonExist
        }

        item.instance?.let {
            Log.w(tag, "launch ext app: app $identifier has legacy instance, ignore")
            item.instance = null
        }

        item.instance = item.extAppClass.newInstance()
        item.instance?.init(identifier, this)
        launchedExtAppList.add(item)
        launchedExtAppMap[item.appIdentifier] = item
        launchedExtAppMapTransformed[item.formatIdentifier] = item
        item.instance?.onExtAppLoaded(this.context)

        item.contentView = item.instance?.onCreateView(context)
        if (item.contentView != null) {
            (container as? ViewGroup)?.addView(item.contentView, item.layoutParams)
        } else {
            Log.w(tag, "launch ext app: cannot find container or content view, app $identifier")
        }

        return AgoraExtAppErrorCode.ExtAppNoError
    }

    @Synchronized fun stopExtApp(identifier: String): Int {
        if (!registeredAppMap.containsKey(identifier)) {
            Log.w(tag, "stop ext app: app $identifier is not registered")
            return AgoraExtAppErrorCode.ExtAppIdNonExist
        }

        if (!launchedExtAppMap.containsKey(identifier) ||
                launchedExtAppMap[identifier] == null) {
            Log.w(tag, "stop ext app: app $identifier has not been initialized or launched")
            return AgoraExtAppErrorCode.ExtAppInstanceNonExist
        }

        launchedExtAppMap[identifier]?.let { item ->
            item.instance?.onExtAppUnloaded()
            (container as? ViewGroup)?.removeView(item.contentView)
            item.instance = null
            launchedExtAppMapTransformed.remove(item.formatIdentifier)
            launchedExtAppMap.remove(item.appIdentifier)
            launchedExtAppList.remove(item)
        }

        return AgoraExtAppErrorCode.ExtAppNoError
    }

    fun getExtAppProperties(identifier: String): MutableMap<String, Any?>? {
        return launchedExtAppMap[identifier]?.instance?.getProperties()
    }

    fun updateExtAppProperties(identifier: String, properties: MutableMap<String, Any?>?,
                               cause: MutableMap<String, Any?>?, callback: AgoraExtAppCallback<String>? = null) {
        getRegisteredExtApp(identifier)?.let { item ->
            aPaaSEntry.updateProperties(item.formatIdentifier, properties, cause, callback)
        }
    }

    fun deleteExtAppProperties(identifier: String, propertyKeys: MutableList<String>,
                               cause: MutableMap<String, Any?>?, callback: AgoraExtAppCallback<String>? = null) {
        aPaaSEntry.deleteProperties(identifier, propertyKeys, cause, callback)
    }

    fun onExtAppPropertyUpdated(identifier: String, properties: MutableMap<String, Any>?,
                                cause: MutableMap<String, Any?>?): Int {
        if (!launchedExtAppMapTransformed.containsKey(identifier) ||
                launchedExtAppMapTransformed[identifier] == null) {
            Log.w(tag, "update ext app properties: app has not been initialized or launched")
            return AgoraExtAppErrorCode.ExtAppInstanceNonExist
        }

        launchedExtAppMapTransformed[identifier]?.instance?.onPropertyUpdated(properties, cause)
        return AgoraExtAppErrorCode.ExtAppNoError
    }

    fun onRoomInfoChanged(roomInfo: AgoraExtAppRoomInfo) {
        launchedExtAppMap.forEach { item ->
            item.value.instance?.updateRoomInfo(roomInfo)
        }
    }

    fun onLocalUserChanged(userInfo: AgoraExtAppUserInfo) {
        launchedExtAppMap.forEach { item ->
            item.value.instance?.updateLocalUserInfo(userInfo)
        }
    }

    fun dispose() {
        launchedExtAppMap.forEach { item ->
            stopExtApp(item.value.appIdentifier)
        }
    }
}

data class AgoraExtAppConfiguration(
        val appIdentifier: String,
        val layout: ViewGroup.LayoutParams,
        val extAppClass: Class<out AgoraExtAppBase>,
        val language: String,
        val imageResource: Int? = null
)

data class AgoraExtAppInfo(
        val appIdentifier: String,
        val language: String,
        val imageResource: Int? = null
)

/**
 * Internally saved app extension app item
 * @param formatIdentifier a transformed app id, used internally
 *  and will not be visible outside, mainly be used to replace
 *  invalid "." inside the original app identifiers which will
 *  be confusing when handling batch room properties
 */
internal data class AgoraExtAppItem(
        val appIdentifier: String,
        var formatIdentifier: String,
        val layoutParams: ViewGroup.LayoutParams,
        val extAppClass: Class<out AgoraExtAppBase>,
        val language: String,
        val imageResource: Int? = null,
        var instance: AgoraExtAppBase? = null,
        var contentView: View? = null
)

data class AgoraExtAppUserInfo(
        val userUuid: String,
        val userName: String,
        val userRole: AgoraExtAppUserRole)

enum class AgoraExtAppUserRole(var value: Int) {
    Invalid(0),
    TEACHER(1),
    STUDENT(2),
    ASSISTANT(3);

    companion object {
        fun toType(value: Int): AgoraExtAppUserRole {
            return when (value) {
                TEACHER.value -> TEACHER
                STUDENT.value -> STUDENT
                ASSISTANT.value -> ASSISTANT
                else -> Invalid
            }
        }
    }
}

data class AgoraExtAppRoomInfo(
        val roomUuid: String,
        val roomName: String,
        val roomType: Int)

object AgoraExtAppErrorCode {
    const val ExtAppEngineError = -1
    const val ExtAppNoError = 0
    const val ExtAppIdDuplicated = 101
    const val ExtAppIdNonExist = 102
    const val ExtAppInstanceNonExist = 103
}