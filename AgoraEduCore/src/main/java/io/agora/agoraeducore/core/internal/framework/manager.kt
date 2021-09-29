package io.agora.agoraeducore.core.internal.framework

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.core.internal.education.api.logger.DebugItem
import io.agora.agoraeducore.core.internal.education.api.logger.LogLevel
import io.agora.agoraeducore.core.internal.education.api.media.EduMediaControl
import io.agora.agoraeducore.core.internal.framework.impl.EduManagerImpl
import io.agora.agoraeducore.core.internal.framework.data.*

data class EduManagerOptions(
        var context: Context,
        val appId: String,
        val rtmToken: String,
        val userUuid: String,
        val userName: String,
        val rtcRegion: String?,
        val rtmRegion: String?) {
    var logLevel: LogLevel = LogLevel.NONE
    var logFileDir: String? = null
}

/**
 * The listener callbacks rtm peer messages which are irrelevant
 * to edu rooms, so the callback listener should be set outside
 *  a room, current in edu manager
 */
interface EduManagerEventListener {
    fun onUserMessageReceived(message: EduMessage)

    fun onUserChatMessageReceived(chatMsg: EduPeerChatMessage)

    fun onUserActionMessageReceived(actionMessage: EduActionMessage)
}

/**
 * return code descriptions:
 * 0 normal
 * 1-10 local error code
 * 101 rtm messaging errors
 * 201 rtc media errors
 * 301 http networking errors
 */
abstract class EduManager(
        val options: EduManagerOptions) {
    companion object {
        val TAG = EduManager::class.java.simpleName

        /** Initialize edu manager instance and login rtm server
         * return code descriptions:
         * 1: illegal arguments
         * 2: internal error, could subscribe what error to receive
         * 101: rtm error
         * 301: networking error
         * */
        @JvmStatic
        fun init(options: EduManagerOptions, callback: EduCallback<EduManager>) {
            (options.context as? Activity)?.let {
                if (it.isFinishing || it.isDestroyed) {
                    callback.onFailure(EduError.parameterError("context"))
                    return
                }
            }

            if (TextUtils.isEmpty(options.appId)) {
                callback.onFailure(EduError.parameterError("appId"))
                return
            }

            if (TextUtils.isEmpty(options.rtmToken)) {
                callback.onFailure(EduError.parameterError("rtmToken"))
                return
            }

            if (TextUtils.isEmpty(options.userUuid)) {
                callback.onFailure(EduError.parameterError("userUuid"))
                return
            }

            if (TextUtils.isEmpty(options.userName)) {
                callback.onFailure(EduError.parameterError("userName"))
                return
            }

            val instance = EduManagerImpl(options)
            instance.login(options.userUuid, options.rtmToken, object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                    callback.onSuccess(instance)
                }

                override fun onFailure(error: EduError) {
                    callback.onFailure(error)
                }
            })
        }

        fun version(): String {
            return BuildConfig.SDK_VERSION
        }
    }

    var eduManagerEventListener: EduManagerEventListener? = null

    abstract fun createEduRoom(config: RoomCreateOptions): EduRoom?

    abstract fun release()

    /**
     * @param appScenario:
     * 0: 1v1, 1: Small Class, 2: Big Class，
     * 3: Super Small Class, 4: Interaction Small Class（aPaaS）
     * @param serviceType: 0: aPaaS，1: PaaS
     * @param appVersion: aPaaS version
     */
    abstract fun reportAppScenario(appScenario: Int, serviceType: Int, appVersion: String)

    /**
     * return code description
     * 1: illegal argument
     * 2: internal error
     */
    abstract fun logMessage(message: String, level: LogLevel): EduError

    /**
     * serialNumber：log serial number, used to search
     * return codes:
     * 1: illegal argument
     * 2: internal error
     * 301: networking error
     */
    abstract fun uploadDebugItem(item: DebugItem, payload: Any?, callback: EduCallback<String>): EduError

    abstract fun getEduMediaControl(): EduMediaControl
}