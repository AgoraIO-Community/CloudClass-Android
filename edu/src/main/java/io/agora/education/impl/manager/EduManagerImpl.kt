package io.agora.education.impl.manager

import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
import io.agora.education.impl.Constants.Companion.APPID
import io.agora.education.impl.Constants.Companion.AgoraLog
import io.agora.education.impl.Constants.Companion.LOGS_DIR_NAME
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.edu.BuildConfig
import io.agora.edu.BuildConfig.LOG_OSS_CALLBACK_HOST
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.communicationError
import io.agora.education.api.base.EduError.Companion.httpError
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.EduManagerOptions
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.statistics.AgoraError
import io.agora.education.impl.network.RetrofitManager
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.RtmConnectState
import io.agora.education.impl.util.Convert
import io.agora.education.impl.util.UnCatchExceptionHandler
import io.agora.log.LogManager
import io.agora.log.UploadManager
import io.agora.rte.RteCallback
import io.agora.rte.RteEngineImpl
import io.agora.rte.data.RteError
import io.agora.rte.listener.RteEngineEventListener
import io.agora.rtm.RtmMessage
import io.agora.rtm.RtmStatusCode
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options), RteEngineEventListener {

    companion object {
        private const val TAG = "EduManagerImpl"

        /**管理所有EduRoom示例的集合*/
        private val eduRooms = mutableListOf<EduRoom>()

        fun addRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.add(eduRoom)
        }

        fun removeRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.remove(eduRoom)
        }
    }

    /**全局的rtm连接状态*/
    private val rtmConnectState = RtmConnectState()

    init {
        /*注册UnCatchExceptionHandler*/
        UnCatchExceptionHandler.getExceptionHandler().init(options.context.applicationContext)
        /*初始化LogManager*/
        options.logFileDir?.let {
            options.logFileDir = options.context.cacheDir.toString().plus(File.separatorChar).plus(LOGS_DIR_NAME)
        }
        LogManager.init(options.logFileDir!!, "AgoraEducation")
        AgoraLog = LogManager("SDK")
        logMessage("${TAG}: Init LogManager,log path is ${options.logFileDir}", LogLevel.INFO)
        logMessage("${TAG}: Init EduManagerImpl", LogLevel.INFO)
        logMessage("${TAG}: Init RteEngineImpl", LogLevel.INFO)
        RteEngineImpl.init(options.context, options.appId, options.logFileDir!!)
        /*为RteEngine设置eventListener*/
        RteEngineImpl.eventListener = this
        APPID = options.appId
//        if (!TextUtils.isEmpty(options.customerId) && !TextUtils.isEmpty(options.customerCertificate)) {
//            val auth = Base64.encodeToString("${options.customerId}:${options.customerCertificate}"
//                    .toByteArray(Charsets.UTF_8), Base64.DEFAULT).replace("\n", "").trim()
//            RetrofitManager.instance()!!.addHeader("Authorization", CryptoUtil.getAuth(auth))
//        }
        RetrofitManager.instance()!!.addHeader("x-agora-token", options.rtmToken)
        RetrofitManager.instance()!!.addHeader("x-agora-uid", options.userUuid)
        RetrofitManager.instance()!!.setLogger(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                /**OKHttp的log写入SDK的log文件*/
                logMessage(message, LogLevel.INFO)
            }
        })
        logMessage("${TAG}: Init of EduManagerImpl completed", LogLevel.INFO)
    }

    override fun createClassroom(config: RoomCreateOptions): EduRoom? {
        if (TextUtils.isEmpty(config.roomUuid) || TextUtils.isEmpty(config.roomName)) {
            return null
        }
        if (!RoomType.roomTypeIsValid(config.roomType)) {
            return null
        }
        val eduRoomInfo = EduRoomInfoImpl(config.roomType, config.roomUuid, config.roomName)
        val status = EduRoomStatus(EduRoomState.INIT, 0, true, 0)
        val room = EduRoomImpl(eduRoomInfo, status)
        /**设置默认用户名*/
        room.defaultUserName = options.userName
        return room
    }

    fun login(userUuid: String, rtmToken: String, callback: EduCallback<Unit>) {
        logMessage("${TAG}: Calling the login function to login RTM", LogLevel.INFO)
        RteEngineImpl.loginRtm(userUuid, rtmToken,
                object : RteCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                        logMessage("${TAG}: Login to RTM successfully", LogLevel.INFO)
                        callback.onSuccess(res)
                    }

                    override fun onFailure(error: RteError) {
                        logMessage("${TAG}: Login to RTM failed->code:${error.errorCode}," +
                                "reason:${error.errorDesc}", LogLevel.ERROR)
                        callback.onFailure(communicationError(error.errorCode,
                                error.errorDesc))
                    }
                })
    }

    override fun release() {
        logMessage("${TAG}: Call release function to exit RTM and release data", LogLevel.INFO)
        RteEngineImpl.logoutRtm()
        eduRooms.clear()
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

    override fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>): EduError {
        val uploadParam = UploadManager.UploadParam(BuildConfig.VERSION_NAME, Build.DEVICE,
                Build.VERSION.SDK, "ZIP", "Android", null)
        logMessage("${TAG}: Call the uploadDebugItem function to upload logs，parameter->${Gson().toJson(uploadParam)}", LogLevel.INFO)
        UploadManager.upload(options.context, APPID, LOG_OSS_CALLBACK_HOST, options.logFileDir!!, uploadParam,
                object : ThrowableCallback<String> {
                    override fun onSuccess(res: String?) {
                        res?.let {
                            logMessage("${TAG}: Log uploaded successfully->$res", LogLevel.INFO)
                            callback.onSuccess(res)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error?.code?.let {
                            logMessage("${TAG}: Log upload error->code:${error?.code}, reason:${error?.message
                                    ?: throwable?.message}", LogLevel.ERROR)
                            callback.onFailure(httpError(error?.code, error?.message
                                    ?: throwable?.message))
                        }
                    }
                })
        return EduError(-1, "")
    }

    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        logMessage("${TAG}: The RTM connection state has changed->state:$p0,reason:$p1", LogLevel.INFO)
        /*断线重连之后，同步至每一个教室*/
        eduRooms?.forEach {
            if (rtmConnectState.isReconnecting() &&
                    p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
                logMessage("${TAG}: RTM disconnection and reconnected，Request missing sequences in classroom " +
                        "${(it as EduRoomImpl).getCurRoomUuid()}", LogLevel.INFO)
                it.syncSession.fetchLostSequence(object : EduCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                        /*断线重连之后，数据同步成功之后再把重连成功的事件回调出去*/
                        it.eventListener?.onConnectionStateChanged(Convert.convertConnectionState(p0), it)
                        /*断线重连之后，重新走initialized回调*/
                        it.onRemoteInitialized()
                    }

                    override fun onFailure(error: EduError) {
                        /*无限重试，保证数据同步成功*/
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
        logMessage("${TAG}: PeerMessage has received->${Gson().toJson(p0)}", LogLevel.INFO)
        /**RTM保证peerMsg能到达,不用走同步检查(seq衔接性检查)*/
        p0?.text?.let {
            eduRooms?.forEach {
                (it as EduRoomImpl).cmdDispatch.dispatchPeerMsg(p0.text, eduManagerEventListener)
            }
        }
    }
}
