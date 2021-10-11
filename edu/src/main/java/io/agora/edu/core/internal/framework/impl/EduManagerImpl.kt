package io.agora.edu.core.internal.framework.impl

import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.core.internal.base.callback.ThrowableCallback
import io.agora.edu.core.internal.base.network.BusinessException
import io.agora.edu.BuildConfig
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.data.EduError.Companion.communicationError
import io.agora.edu.core.internal.framework.data.EduError.Companion.httpError
import io.agora.edu.core.internal.education.api.logger.DebugItem
import io.agora.edu.core.internal.education.api.logger.LogLevel
import io.agora.edu.core.internal.education.api.media.EduMediaControl
import io.agora.edu.core.internal.education.api.room.data.*
import io.agora.edu.core.internal.education.api.statistics.AgoraError
import io.agora.edu.core.internal.education.impl.Constants.Companion.APPID
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.education.impl.media.EduMediaControlImpl
import io.agora.edu.core.internal.education.impl.network.RetrofitManager
import io.agora.edu.core.internal.education.impl.room.EduRoomImpl
import io.agora.edu.core.internal.education.impl.room.data.EduRoomInfoImpl
import io.agora.edu.core.internal.education.impl.room.data.RtmConnectState
import io.agora.edu.core.internal.education.impl.util.Convert
import io.agora.edu.core.internal.education.impl.util.UnCatchExceptionHandler
import io.agora.edu.core.internal.education.impl.util.UnCatchExceptionHandler.Companion.hasException
import io.agora.edu.core.internal.framework.*
import io.agora.edu.core.internal.launch.AgoraEduSDK
import io.agora.edu.core.internal.log.UploadManager
import io.agora.edu.core.internal.log.UploadManager.Params.AndroidLog
import io.agora.edu.core.internal.log.UploadManager.Params.ZIP
import io.agora.edu.core.internal.rte.RteCallback
import io.agora.edu.core.internal.rte.RteEngineImpl
import io.agora.edu.core.internal.rte.data.RtcAppScenario
import io.agora.edu.core.internal.rte.data.RteError
import io.agora.edu.core.internal.rte.listener.RteEngineEventListener
import io.agora.edu.core.internal.server.requests.AgoraRequestClient
import io.agora.rtc.RtcEngine
import io.agora.rtm.RtmMessage
import io.agora.rtm.RtmStatusCode
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File

internal class EduManagerImpl(options: EduManagerOptions) : EduManager(options), RteEngineEventListener {
    companion object {
        private const val TAG = "EduManagerImpl"

        private val eduRooms = mutableListOf<EduRoom>()

        fun addRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.add(eduRoom)
        }

        fun removeRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.remove(eduRoom)
        }
    }

    private val rtmConnectState = RtmConnectState()
    private val eduMediaControl = EduMediaControlImpl()

    init {
        logMessage("$TAG: Init EduManagerImpl", LogLevel.INFO)
        logMessage("$TAG: Init RteEngineImpl", LogLevel.INFO)

        if (!TextUtils.isEmpty(options.logFileDir)) {
            RteEngineImpl.init(options.context, options.appId, options.logFileDir!!, options.rtcRegion, options.rtmRegion)
        } else {
            AgoraLog.e("$TAG->options.logFileDir is empty!")
        }
        RteEngineImpl.eventListener = this
        APPID = options.appId

        // Associate request client and rtm client
        AgoraRequestClient.setRtmMessageDelegate(RteEngineImpl)

        RetrofitManager.instance()!!.addHeader("x-agora-token", options.rtmToken)
        RetrofitManager.instance()!!.addHeader("x-agora-uid", options.userUuid)
        RetrofitManager.instance()!!.setLogger(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logMessage(message, LogLevel.INFO)
            }
        })

        logMessage("$TAG: Init of EduManagerImpl completed", LogLevel.INFO)
    }

    override fun createEduRoom(config: RoomCreateOptions): EduRoom? {
        if (TextUtils.isEmpty(config.roomUuid) || TextUtils.isEmpty(config.roomName)) {
            return null
        }

        if (!RoomType.roomTypeIsValid(config.roomType)) {
            return null
        }

        val eduRoomInfo = EduRoomInfoImpl(config.roomType, config.roomUuid, config.roomName)
        val status = EduRoomStatus(EduRoomState.INIT, 0, true, 0, 0)
        val room = EduRoomImpl(eduRoomInfo, status)
        room.defaultUserName = options.userName
        return room
    }

    fun login(userUuid: String, rtmToken: String, callback: EduCallback<Unit>) {
        logMessage("$TAG: Calling the login function to login RTM", LogLevel.INFO)
        RteEngineImpl.loginRtm(userUuid, rtmToken,
                object : RteCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                        logMessage("$TAG: Login to RTM successfully", LogLevel.INFO)
                        callback.onSuccess(res)
                    }

                    override fun onFailure(error: RteError) {
                        logMessage("$TAG: Login to RTM failed->code:${error.errorCode}," +
                                "reason:${error.errorDesc}", LogLevel.ERROR)
                        callback.onFailure(communicationError(error.errorCode,
                                error.errorDesc))
                    }
                })
    }

    override fun release() {
        logMessage("$TAG: Call release function to exit RTM and release data", LogLevel.INFO)
        if (hasException()) {
            UnCatchExceptionHandler.getExceptionHandler().uploadAndroidException()
        }

        RteEngineImpl.logoutRtm()
        RteEngineImpl.dispose()
        eduRooms.clear()
    }

    override fun reportAppScenario(appScenario: Int, serviceType: Int, appVersion: String) {
        val rtcAppScenario = RtcAppScenario(appScenario, serviceType, appVersion)
        val jsonObject = JSONObject()
        jsonObject.put("rtc.report_app_scenario", Gson().toJson(rtcAppScenario))
        RteEngineImpl.setRtcParameters(jsonObject.toString())
    }

    override fun logMessage(message: String, level: LogLevel): EduError {
        when (level) {
            LogLevel.NONE -> {
                AgoraLog.d(message)
            }
            LogLevel.INFO -> {
                AgoraLog.i(message)
            }
            LogLevel.WARN -> {
                AgoraLog.w(message)
            }
            LogLevel.ERROR -> {
                AgoraLog.e(message)
            }
        }
        return EduError(AgoraError.NONE.value, "")
    }

    override fun uploadDebugItem(item: DebugItem, payload: Any?, callback: EduCallback<String>): EduError {
        val uploadParam = UploadManager.UploadParam(BuildConfig.SDK_VERSION, Build.DEVICE,
                Build.VERSION.SDK, ZIP, "Android", payload)
        logMessage("$TAG: Call the uploadDebugItem function to upload logs，parameter->${Gson().toJson(uploadParam)}", LogLevel.INFO)
        val payloadJson = Gson().toJson(payload)
        val payloadMap: MutableMap<String, Any> = Gson().fromJson(payloadJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        if (!payloadMap.isNullOrEmpty()) {
            AndroidLog.forEach {
                payloadMap[it.key] = it.value
            }
            uploadParam.tag = payloadMap
        }
        UploadManager.upload(options.context!!, APPID, AgoraEduSDK.logHostUrl(), options.logFileDir!!, uploadParam,
                object : ThrowableCallback<String> {
                    override fun onSuccess(res: String?) {
                        res?.let {
                            logMessage("$TAG: Log uploaded successfully->$res", LogLevel.INFO)
                            callback.onSuccess(res)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error?.code?.let {
                            logMessage("$TAG: Log upload error->code:${error?.code}, reason:${
                                error?.message
                                        ?: throwable?.message
                            }", LogLevel.ERROR)
                            callback.onFailure(httpError(error?.code, error?.message
                                    ?: throwable?.message))
                        }
                    }
                })
        return EduError(-1, "")
    }

    override fun getEduMediaControl(): EduMediaControl {
        return eduMediaControl
    }

    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        logMessage("$TAG: The RTM connection state has changed->state:$p0,reason:$p1", LogLevel.INFO)

        eduRooms.forEach {
            if (rtmConnectState.isReconnecting() &&
                    p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
                logMessage("$TAG: RTM disconnection and reconnected，Request missing " +
                        "sequences in classroom ${(it as EduRoomImpl).getCurRoomUuid()}", LogLevel.INFO)

                it.syncSession.fetchLostSequence(object : EduCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                        it.eventListener?.onConnectionStateChanged(Convert.convertConnectionState(p0), it)
                        it.onRemoteInitialized()
                    }

                    override fun onFailure(error: EduError) {
                        it.syncSession.fetchLostSequence(this)
                    }
                })
            } else {
                it.eventListener?.onConnectionStateChanged(Convert.convertConnectionState(p0), it)
            }
        }
        rtmConnectState.lastConnectionState = p0
    }

    override fun onPeerMsgReceived(p0: RtmMessage?, p1: String?) {
        logMessage("$TAG: PeerMessage has received->${Gson().toJson(p0)}", LogLevel.INFO)
        if (p0 == null || p1 == null) {
            logMessage("$TAG: PeerMessage illegal value->${Gson().toJson(p0)}", LogLevel.WARN)
            return
        }

        // Check if the rtm client server consumes and parses this peer message,
        // because this rtm peer message suits the format of request server protocol.
        if (!AgoraRequestClient.handleRtmRequestResponses(p0.text, p1)) {
            eduRooms.forEach {
                (it as EduRoomImpl).cmdDispatch.dispatchPeerMsg(p0.text, eduManagerEventListener)
            }
        }
    }
}
