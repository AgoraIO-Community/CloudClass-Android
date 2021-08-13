package io.agora.extension

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import io.agora.educontext.EduContextPool

class AgoraExtAppEngine(
        private var context: Context,
        private var container: RelativeLayout,
        private var eduContext: EduContextPool,
        internal var aPaaSEntry: IAgoraExtAppAPaaSEntry) {

    private val tag = "AgoraExtAppEngine"
    private val launchedExtAppList = mutableListOf<AgoraExtAppItem>()
    private val launchedExtAppMap = mutableMapOf<String, AgoraExtAppItem>()
    private val launchedExtAppMapTransformed = mutableMapOf<String, AgoraExtAppItem>()

    companion object {
        private val registeredAppList = ArrayList<AgoraExtAppItem>()
        private val registeredAppMap = mutableMapOf<String, AgoraExtAppItem>()
        private val registeredAppMapTransformed = mutableMapOf<String, AgoraExtAppItem>()

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
                        config.param,
                        config.extAppClass,
                        config.language,
                        config.imageResource)
                registeredAppList.add(item)
                registeredAppMap[item.appIdentifier] = item
                registeredAppMapTransformed[item.formatIdentifier] = item
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

    /**
     * Launch an extension app denoted by the identifier.
     * Usually the formatted identifiers are maintained inside extension
     * app engine and aPaaS sdk. That means, identifiers should be formatted
     * if an extension app is launched from, e.g., callbacks of aPaaS,
     * where only formatted identifiers are maintained.
     * The identifiers would probably be not formatted if this method
     * is called from user code, where developers only know the original
     * identifiers of their extension apps.
     * @param identifier id of the extension app
     * @param formatted whether the identifier is a formatted one.
     */
    @Synchronized
    fun launchExtApp(identifier: String, formatted: Boolean = false, currentTime: Long): Int {
        if (formatted && launchedExtAppMapTransformed.containsKey(identifier)) {
            Log.w(tag, "launch ext app: app $identifier has been launched")
            return AgoraExtAppErrorCode.ExtAppIdDuplicated
        } else if (!formatted && launchedExtAppMap.containsKey(identifier)) {
            Log.w(tag, "launch ext app: app $identifier has been launched")
            return AgoraExtAppErrorCode.ExtAppIdDuplicated
        }

        val item = if (formatted)
            registeredAppMapTransformed[identifier]
        else registeredAppMap[identifier]

        if (item == null) {
            Log.w(tag, "launch ext app: app $identifier not found, have you registered this app?")
            return AgoraExtAppErrorCode.ExtAppIdNonExist
        }

        item.instance?.let {
            Log.w(tag, "launch ext app: app $identifier has legacy instance, ignore")
            item.instance = null
        }

        //sync the server ts
        TimeUtil.calibrateTimestamp(currentTime)

        item.instance = item.extAppClass.newInstance()
        item.instance?.init(item.appIdentifier, this)
        Log.d(tag, "launch ext app, extension app initialized, app id ${item.appIdentifier}")

        launchedExtAppList.add(item)
        launchedExtAppMap[item.appIdentifier] = item
        launchedExtAppMapTransformed[item.formatIdentifier] = item

        ContextCompat.getMainExecutor(context).execute {
            item.contentView = item.instance?.onCreateView(context)
            if (item.contentView != null) {
                container.addView(item.contentView, RelativeLayout.LayoutParams(
                        item.param.width, item.param.height))
                item.instance?.onExtAppLoaded(this.context, container, item.contentView!!, eduContext)
            } else {
                Log.w(tag, "launch ext app: cannot find container or content view, app $identifier")
            }
        }

        return AgoraExtAppErrorCode.ExtAppNoError
    }

    @Synchronized
    fun stopExtApp(identifier: String, formatted: Boolean = false): Int {
        if (formatted && !registeredAppMapTransformed.containsKey(identifier)) {
            Log.w(tag, "stop ext app: app $identifier is not registered")
            return AgoraExtAppErrorCode.ExtAppIdNonExist
        } else if (!formatted && !registeredAppMap.contains(identifier)) {
            Log.w(tag, "stop ext app: app $identifier is not registered")
            return AgoraExtAppErrorCode.ExtAppIdNonExist
        }

        val app: AgoraExtAppItem? = if (formatted) {
            launchedExtAppMapTransformed[identifier]
        } else {
            launchedExtAppMap[identifier]
        }

        app?.let { item ->
            ContextCompat.getMainExecutor(context).execute {
                (container as? ViewGroup)?.removeView(item.contentView)
            }

            item.instance?.onExtAppUnloaded()
            item.instance = null
            launchedExtAppMapTransformed.remove(item.formatIdentifier)
            launchedExtAppMap.remove(item.appIdentifier)
            launchedExtAppList.remove(item)
        }

        if (app == null) {
            Log.w(tag, "stop ext app: app $identifier has not been initialized or launched")
            return AgoraExtAppErrorCode.ExtAppInstanceNonExist
        }

        return AgoraExtAppErrorCode.ExtAppNoError
    }

    fun getExtAppProperties(identifier: String): MutableMap<String, Any?>? {
        return launchedExtAppMap[identifier]?.instance?.getProperties()
    }

    fun updateExtAppProperties(identifier: String,
                               properties: MutableMap<String, Any?>?,
                               cause: MutableMap<String, Any?>?,
                               common: MutableMap<String, Any?>?,
                               callback: AgoraExtAppCallback<String>? = null) {
        getRegisteredExtApp(identifier)?.let { item ->
            aPaaSEntry.updateProperties(item.formatIdentifier, properties, cause, common, callback)
        }
    }

    fun deleteExtAppProperties(identifier: String, propertyKeys: MutableList<String>,
                               cause: MutableMap<String, Any?>?, callback: AgoraExtAppCallback<String>? = null) {
        aPaaSEntry.deleteProperties(identifier, propertyKeys, cause, callback)
    }

    @Synchronized
    fun onExtAppPropertyUpdated(identifier: String,
                                properties: MutableMap<String, Any?>?,
                                cause: MutableMap<String, Any?>?,
                                state: MutableMap<String, Any?>?, currentTime: Long): Int {

        val shouldLaunch = parseExtAppLaunched(state)
        var launched = launchedExtAppMapTransformed[identifier] != null
        var ret = AgoraExtAppErrorCode.ExtAppNoError

        Log.d(tag, "ext app property updated, $identifier, will be " +
                "launched: $shouldLaunch, currently launched: $launched")

        if (shouldLaunch && !launched) {
            ret = launchExtApp(identifier, true,currentTime)
        } else if (!shouldLaunch && launched) {
            ret = stopExtApp(identifier, true)
        } else {
            Log.d(tag, "Extension app $identifier has already been ${ if (launched) "launched" else "stopped" }")
        }

        launched = launchedExtAppMapTransformed[identifier] != null
        if (launched) {
            Log.d(tag, "extension app $identifier update properties")
            launchedExtAppMapTransformed[identifier]?.instance?.onPropertyUpdated(properties, cause)
        }

        return ret
    }

    private fun parseExtAppLaunched(state: MutableMap<String, Any?>?) : Boolean {
        // Currently the state map contains the current extension app's launch state.
        // state: 0 = not launched; 1 = launched.
        // If state map is null, this app has not launched by default.
        var launched = false
        state?.get("state")?.let { value ->
            if (value is Double) {
                launched = value.toInt() == 1
            }
        }
        return launched
    }

    fun onRoomInfoChanged(roomInfo: AgoraExtAppRoomInfo) {
        launchedExtAppMap.forEach { item ->
            item.value.instance?.onRoomInfoUpdate(roomInfo)
        }
    }

    fun onLocalUserChanged(userInfo: AgoraExtAppUserInfo) {
        launchedExtAppMap.forEach { item ->
            item.value.instance?.onLocalUserInfoUpdate(userInfo)
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
        val param: AgoraExtAppLayoutParam,
        val extAppClass: Class<out AgoraExtAppBase>,
        val language: String,
        val imageResource: Int? = null
)

/**
 * How to determine the position of extension app on screen.
 * By default the app is located at the center of screen
 */
class AgoraExtAppLayoutParam(
        val width: Int,
        val height: Int) {

    companion object {
        /**
         * the inner layout determines the size of extension app
         */
        const val wrap = RelativeLayout.LayoutParams.WRAP_CONTENT
    }
}

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
        val param: AgoraExtAppLayoutParam,
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