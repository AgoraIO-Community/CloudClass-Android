package io.agora.agoraeducore.core.internal.education.impl.user

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.APPID
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.BusinessException
import io.agora.agoraeducore.core.internal.base.network.ResponseBody
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.customMsgError
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.httpError
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.mediaError
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.parameterError
import io.agora.agoraeducore.core.internal.education.api.logger.LogLevel
import io.agora.agoraeducore.core.internal.education.api.statistics.AgoraError
import io.agora.agoraeducore.core.internal.education.api.stream.data.*
import io.agora.agoraeducore.core.internal.framework.EduLocalUser
import io.agora.agoraeducore.core.internal.framework.EduActionConfig
import io.agora.agoraeducore.core.internal.framework.EduLocalUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserEventListener
import io.agora.agoraeducore.core.internal.education.impl.network.RetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.room.EduRoomImpl
import io.agora.agoraeducore.core.internal.server.struct.request.EduRemoveRoomPropertyReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUpsertRoomPropertyReq
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.RoomService
import io.agora.agoraeducore.core.internal.education.impl.stream.EduStreamInfoImpl
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.deprecated.StreamService
import io.agora.agoraeducore.core.internal.education.impl.user.data.request.*
import io.agora.agoraeducore.core.internal.education.impl.util.Convert
import io.agora.agoraeducore.core.internal.education.impl.util.Convert.convertFromUserInfo
import io.agora.agoraeducore.core.internal.education.impl.util.Convert.convertVideoEncoderConfig
import io.agora.agoraeducore.core.internal.framework.data.*
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.noError
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
import io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.agoraeducore.core.internal.rte.RteEngineImpl
import io.agora.agoraeducore.core.internal.rte.RteEngineImpl.OK
import io.agora.agoraeducore.core.internal.rte.RteEngineImpl.getError
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomChatMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUserChatMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUserMsgReq
import io.agora.agoraeducore.core.internal.server.struct.response.DataResponseBody
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal open class EduUserImpl(
        override var userInfo: EduLocalUserInfo
) : EduLocalUser {
    val tag = "EduUserImpl"

    override var videoEncoderConfig = EduVideoEncoderConfig()

    override var eventListener: EduUserEventListener? = null

    lateinit var eduRoom: EduRoomImpl

    @Volatile
    var lastMicState: Boolean? = null

    @Volatile
    var lastCameraState: Boolean? = null

    private val textureViewList = Collections.synchronizedList(mutableListOf<TextureView>())

    private val renderConfigMap = Collections.synchronizedMap(mutableMapOf<TextureView, EduRenderConfig>())

    private val textureViewMap = Collections.synchronizedMap(mutableMapOf<TextureView, ViewGroup>())

    final override var cachedRemoteVideoStates: MutableMap<String, Int> = ConcurrentHashMap()

    final override var cachedRemoteAudioStates: MutableMap<String, Int> = ConcurrentHashMap()

    final override var cacheRemoteOnlineUserIds: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        if (TextUtils.isEmpty(options.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        AgoraLog.i("$tag->Start initOrUpdateLocalStream:${Gson().toJson(options)}")
//        val a = RteEngineImpl.setVideoEncoderConfiguration(
//                Convert.convertVideoEncoderConfig(eduVideoEncoderConfig))
//        if (a != OK()) {
//            callback.onFailure(mediaError(a, getError(a)))
//            return
//        }
        /**enableCamera和enableMicrophone控制是否打开摄像头和麦克风的采集*/
        if (options.enableMicrophone != lastMicState || lastMicState == null) {
            val c1 = RteEngineImpl.enableLocalAudio(options.enableMicrophone)
            RteEngineImpl.muteLocalAudioStream(eduRoom.getCurRoomUuid(), !options.hasAudio)

            if (c1 != OK()) {
                callback.onFailure(mediaError(c1, getError(c1)))
                return
            }
        }

        lastMicState = options.enableMicrophone

        if (options.enableCamera != lastCameraState || lastCameraState == null) {
            val c2 = RteEngineImpl.enableLocalVideo(options.enableCamera)
            RteEngineImpl.muteLocalVideoStream(eduRoom.getCurRoomUuid(), !options.hasVideo)
            if (c2 != OK()) {
                callback.onFailure(mediaError(c2, getError(c2)))
                return
            }
        }
        lastCameraState = options.enableCamera

        /**根据当前配置生成一个流信息*/
        val streamInfo = EduStreamInfoImpl(options.streamUuid, options.streamName, VideoSourceType.CAMERA,
                options.hasVideo, options.hasAudio, this.userInfo, System.currentTimeMillis())
        callback.onSuccess(streamInfo)
    }

    override fun switchCamera(): EduError? {
        val code = RteEngineImpl.switchCamera()
        AgoraLog.i("$tag->switchCamera:$code")
        return if (code == OK()) null else mediaError(code, getError(code))
    }

    override fun subscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions,
                                 callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        /**订阅远端流*/
        val uid: Int = (stream.streamUuid.toLong() and 0xffffffffL).toInt()
        Log.e(tag, "")
        val code = RteEngineImpl.muteRemoteStream(eduRoom.getCurRoomUuid(), uid, !options.subscribeAudio,
                !options.subscribeVideo)
        AgoraLog.i("$tag->subscribeStream: streamUuid:${stream.streamUuid},audio:${options.subscribeAudio}," +
                "video:${options.subscribeVideo}, code: $code")
        if (code == OK()) {
            callback.onSuccess(Unit)
        } else {
            callback.onFailure(mediaError(code, getError(code)))
        }
    }

    override fun unSubscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions,
                                   callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        val uid: Int = (stream.streamUuid.toLong() and 0xffffffffL).toInt()
        val code = RteEngineImpl.muteRemoteStream(eduRoom.getCurRoomUuid(), uid, !options.subscribeAudio,
                !options.subscribeVideo)
        AgoraLog.i("$tag->unSubscribeStream: streamUuid: ${stream.streamUuid},audio:${options.subscribeAudio}," +
                "video:${options.subscribeVideo},code: $code")
        if (code == OK()) {
            callback.onSuccess(Unit)
        } else {
            callback.onFailure(mediaError(code, getError(code)))
        }
    }

    override fun publishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        /**改变流状态的参数*/
        val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0,
                if (stream.hasAudio) 1 else 0)
        AgoraLog.logMsg("$tag->Create new Stream: ${Gson().toJson(stream)}", LogLevel.INFO.value)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
                .createStream(APPID, eduRoom.getCurRoomUuid(), userInfo.userUuid,
                        stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        AgoraLog.logMsg("$tag->publishStream state: streamUuid: ${stream.streamUuid}," +
                                "${stream.hasAudio},${stream.hasVideo}",
                                LogLevel.INFO.value)
                        val a = RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(), CLIENT_ROLE_BROADCASTER)
                        if (a != OK()) {
                            callback.onFailure(mediaError(a, getError(a)))
                            return
                        }
                        val b = RteEngineImpl.muteLocalStream(eduRoom.getCurRoomUuid(),
                                !stream.hasAudio, !stream.hasVideo)
                        if (b != OK()) {
                            callback.onFailure(mediaError(b, getError(b)))
                            return
                        }
                        val c = RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                        if (c != OK()) {
                            callback.onFailure(mediaError(c, getError(c)))
                            return
                        }
                        callback.onSuccess(true)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    private fun muteLocal(oldStream: EduStreamInfo, stream: EduStreamInfo): Int {

        if (oldStream.hasAudio != stream.hasAudio && oldStream.hasVideo != stream.hasVideo) {
            val code0 = RteEngineImpl.muteLocalAudioStream(eduRoom.getCurRoomUuid(), !stream.hasAudio)
            val code1 = RteEngineImpl.muteLocalVideoStream(eduRoom.getCurRoomUuid(), !stream.hasVideo)
            return if (code0 == Constants.ERR_OK && code1 == Constants.ERR_OK) Constants.ERR_OK else -1
        } else if (oldStream.hasAudio != stream.hasAudio && oldStream.hasVideo == stream.hasVideo) {
            val code0 = RteEngineImpl.muteLocalAudioStream(eduRoom.getCurRoomUuid(), !stream.hasAudio)
            return if (code0 == Constants.ERR_OK) Constants.ERR_OK else -1
        } else {
            val code1 = RteEngineImpl.muteLocalVideoStream(eduRoom.getCurRoomUuid(), !stream.hasVideo)
            return if (code1 == Constants.ERR_OK) Constants.ERR_OK else -1
        }
    }

    override fun muteStream(stream: EduStreamInfo, callback: EduCallback<Boolean>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        val index = Convert.streamExistsInList(stream, eduRoom.getCurStreamList())
        if (index > -1) {
            val oldStream = eduRoom.getCurStreamList()[index]
            if (oldStream == stream) {
                AgoraLog.e("$tag->Wanted state of the stream to be updated is same with the current state，return")
                callback.onSuccess(true)
            } else {
                AgoraLog.i("$tag->Start updating locally existing stream info, streamUuid: + " +
                        "${stream.streamUuid}, Wanted state is:${stream.hasAudio},${stream.hasVideo}")
                /**设置角色*/
                val a = RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(), CLIENT_ROLE_BROADCASTER)
                if (a != OK()) {
                    callback.onFailure(mediaError(a, getError(a)))
                    return
                }
                val b = muteLocal(oldStream, stream)
                if (b != OK()) {
                    callback.onFailure(mediaError(b, getError(b)))
                    return
                }
                val c = RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                if (c != OK()) {
                    callback.onFailure(mediaError(c, getError(c)))
                    return
                }
                /**改变流状态的参数*/
                val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                        AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0,
                        if (stream.hasAudio) 1 else 0, 0)
                RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
                        .updateStreamInfo(APPID, eduRoom.getCurRoomUuid(), userInfo.userUuid,
                                stream.streamUuid, eduStreamStatusReq)
                        .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                            override fun onSuccess(res: ResponseBody<String>?) {
//                                (streamInfo as EduStreamInfoImpl).updateTime = res?.timeStamp
                                AgoraLog.i("$tag->Update stream info success, streamUuid: + ${stream.streamUuid}")
                                callback.onSuccess(true)
                            }

                            override fun onFailure(throwable: Throwable?) {
                                AgoraLog.e("$tag->Update stream info failed,streamUuid: + ${stream.streamUuid}")
                                var error = throwable as? BusinessException
                                callback.onFailure(httpError(error?.code
                                        ?: AgoraError.INTERNAL_ERROR.value,
                                        error?.message ?: throwable?.message))
                            }
                        }))
            }
        } else {
            val error = customMsgError("The stream you want to update does not exist locally, " +
                    "streamUuid: + ${stream.streamUuid}")
            AgoraLog.e("$tag->${error.msg}")
            callback.onFailure(error)
        }
    }

    override fun unPublishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        AgoraLog.i("$tag->Del stream:${stream.streamUuid}")
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
                .deleteStream(APPID, eduRoom.getCurRoomUuid(), userInfo.userUuid,
                        stream.streamUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        /**设置角色*/
                        val a = RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(), CLIENT_ROLE_AUDIENCE)
                        if (a != OK()) {
                            callback.onFailure(mediaError(a, getError(a)))
                            return
                        }
                        val b = RteEngineImpl.muteLocalStream(eduRoom.getCurRoomUuid(),
                                muteAudio = true, muteVideo = true)
                        if (b != OK()) {
                            callback.onFailure(mediaError(b, getError(b)))
                            return
                        }
                        val c = RteEngineImpl.unpublish(eduRoom.getCurRoomUuid())
                        if (c != OK()) {
                            callback.onFailure(mediaError(c, getError(c)))
                            return
                        }
                        callback.onSuccess(true)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun sendRoomMessage(message: String, callback: EduCallback<EduMessage>) {
        AgoraLog.i("$tag->sendRoomMessage:$message")
        val roomMsgReq = EduRoomMsgReq(message)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .sendChannelCustomMessage(APPID, eduRoom.getCurRoomUuid(), roomMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        val textMessage = EduMessage(convertFromUserInfo(userInfo), message, listOf(),
                                System.currentTimeMillis(), 0)
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun sendUserMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduMessage>) {
        AgoraLog.i("$tag->sendUserMessage:$message,remoteUserUuid:${remoteUser.userUuid}")
        val userMsgReq = EduUserMsgReq(message)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .sendPeerCustomMessage(APPID, eduRoom.getCurRoomUuid(), remoteUser.userUuid, userMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        val textMessage = EduMessage(convertFromUserInfo(userInfo), message, listOf(),
                                System.currentTimeMillis(), 0)
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun sendRoomChatMessage(message: String, callback: EduCallback<EduChatMessage>) {
        AgoraLog.i("$tag->sendRoomChatMessage:$message")
        val roomChatMsgReq = EduRoomChatMsgReq(message, EduChatMessageType.Text.value)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .sendRoomChatMsg(eduRoom.getCurLocalUser().userInfo.userToken!!, APPID,
                        eduRoom.getCurRoomUuid(), roomChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        val textMessage = EduChatMessage(convertFromUserInfo(userInfo), message, listOf(),
                                System.currentTimeMillis(), 0, EduChatMessageType.Text.value)
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduChatMessage>) {
        AgoraLog.i("$tag->sendUserChatMessage:$message,remoteUserUuid:${remoteUser.userUuid}")
        val userChatMsgReq = EduUserChatMsgReq(message, EduChatMessageType.Text.value)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .sendPeerChatMsg(APPID, eduRoom.getCurRoomUuid(), remoteUser.userUuid, userChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        val textMessage = EduChatMessage(convertFromUserInfo(userInfo), message, listOf(),
                                System.currentTimeMillis(), 0, EduChatMessageType.Text.value)
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun startActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(config.processUuid)) {
            callback.onFailure(parameterError("processUuid"))
            return
        }
        if (TextUtils.isEmpty(config.toUserUuid)) {
            callback.onFailure(parameterError("toUser'userUuid"))
            return
        }
        if (config.timeout <= 0) {
            callback.onFailure(parameterError("timeout"))
            return
        }
        AgoraLog.i("$tag->startActionWithConfig:${Gson().toJson(config)}")
//        val actionReq = EduActionReq(userInfo.userUuid, ReqPayload(config.action.value,
//                ReqUser(userInfo.userUuid, userInfo.userName, userInfo.role.name),
//                ReqRoom(eduRoom.getCurRoomInfo().roomName, eduRoom.getCurRoomUuid())))
//        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), UserService::class.java)
//                .doAction(APPID, eduRoom.getCurRoomUuid(), config.toUserUuid, config.processUuid, actionReq)
//                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.edu.core.internal.education.impl.network.ResponseBody<String>> {
//                    override fun onSuccess(res: io.agora.edu.core.internal.education.impl.network.ResponseBody<String>?) {
//                        callback.onSuccess(Unit)
//                    }
//
//                    override fun onFailure(throwable: Throwable?) {
//                        var error = throwable as? BusinessException
//                        callback.onFailure(httpError(error?.code
//                                ?: AgoraError.INTERNAL_ERROR.value,
//                                error?.message ?: throwable?.message))
//                    }
//                }))
    }

    override fun stopActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(config.processUuid)) {
            callback.onFailure(parameterError("processUuid"))
            return
        }
        AgoraLog.i("$tag->stopActionWithConfig:${Gson().toJson(config)}")
//        val actionReq = EduActionReq(userInfo.userUuid, ReqPayload(config.action.value,
//                ReqUser(userInfo.userUuid, userInfo.userName, userInfo.role.name),
//                ReqRoom(eduRoom.getCurRoomInfo().roomName, eduRoom.getCurRoomUuid())))
//        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), UserService::class.java)
//                .doAction(APPID, eduRoom.getCurRoomUuid(), config.toUserUuid, eduRoom.getCurRoomUuid(), actionReq)
//                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.edu.core.internal.education.impl.network.ResponseBody<String>> {
//                    override fun onSuccess(res: io.agora.edu.core.internal.education.impl.network.ResponseBody<String>?) {
//                        callback.onSuccess(Unit)
//                    }
//
//                    override fun onFailure(throwable: Throwable?) {
//                        var error = throwable as? BusinessException
//                        callback.onFailure(httpError(error?.code
//                                ?: AgoraError.INTERNAL_ERROR.value,
//                                error?.message ?: throwable?.message))
//                    }
//                }))
    }

    /**
     * @param viewGroup parent view of video surface, and it's better to be
     * independent of other UI elements to avoid layout issues. If null,
     * it means we want to remove the corresponding video surface of
     * current video stream
     */
    override fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?,
                               config: EduRenderConfig, top: Boolean): EduError {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            return parameterError("streamUuid")
        }

        if (TextUtils.isEmpty(stream.publisher.userUuid)) {
            return parameterError("publisher'userUuid")
        }

        if (TextUtils.isEmpty(channelId)) {
            return parameterError("channelId")
        }

        val list = textureViewList.filter { it.tag == stream.streamUuid }
        if (list.size > 1) {
            Log.w(tag, "setStreamView, more than one surface view is " +
                    "created for a single stream ${stream.streamUuid}, check " +
                    " the code with cautions")
            return customMsgError("Duplicated surface views " +
                    "created for stream ${stream.streamUuid}")
        }

        var uid: Int = (stream.streamUuid.toLong() and 0xffffffffL).toInt()
        val videoCanvas: VideoCanvas
        if (viewGroup == null) {
            // View group is null means we want to remove the video
            // surface of the corresponding stream uuid.
            val textureView = if (list.isNotEmpty()) list[0] else null
            if (textureView != null) {
                Log.d(tag, "Remove surface $textureView for stream " +
                        "${stream.streamUuid} from parent ${textureView.parent}")
                removeTextureView(textureView)
            } else {
                Log.d(tag, "Remove surface for parent $viewGroup, but " +
                        "the surface does not exist, operation ignored.")
            }

            videoCanvas = VideoCanvas(null, config.eduRenderMode.value, uid)
        } else {
            var oldTextureView: TextureView? = null
            (if (list.isNotEmpty()) list[0] else null)?.let { textureView ->
                oldTextureView = textureView
                textureViewMap[textureView]?.let { parent ->
                    if (parent == viewGroup) {
                        // If the view group has already set a video surface,
                        // nothing will be done to this stream and view group.
                        Log.d(tag, "The same view group is repeatedly passed " +
                                "to stream ${stream.streamUuid}, maintain the current " +
                                "surface view and only refresh renderConfig.")
                        return refreshRenderConfig(stream, channelId, textureView, config)
                    }
                }
            }

            // Remove and create a new video surface for this stream if:
            // 1. no surface has been created;
            // 2. a new view group has been passed
            oldTextureView?.let {
                Log.d(tag, "remove existing surface view for stream " +
                        "${stream.streamUuid} from parent parent ${it.parent}")
                removeTextureView(it)
            }

            val textureView = RtcEngine.CreateTextureView(
                    viewGroup.context.applicationContext)
            textureView.tag = stream.streamUuid
            textureView.layoutParams = ViewGroup.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            Log.d(tag, "add a new surface view $textureView for stream " +
                    "${stream.streamUuid} to parent $viewGroup")
            addTextureView(viewGroup, textureView, config)

            if (stream.publisher.userUuid == userInfo.userUuid) {
                // Note, rtc video canvas requires that the rtc
                // media uid should be 0 for local user.
                uid = 0
            }
            videoCanvas = VideoCanvas(textureView, config.eduRenderMode.value, channelId, uid,
                    config.eduMirrorMode.value)
        }

        val code = setupVideo(videoCanvas)

        return EduError(code, getError(code))
    }

    private fun setupVideo(videoCanvas: VideoCanvas): Int {
        return if (videoCanvas.uid == 0) {
            val result = RteEngineImpl.setupLocalVideo(videoCanvas)
            if (result == 0) {
                AgoraLog.e("$tag->setupLocalVideo success")
            }
            result
        } else {
            val result = RteEngineImpl.setupRemoteVideo(videoCanvas)
            if (result == 0) {
                AgoraLog.e("$tag->setupRemoteVideo success")
            }
            result
        }
    }

    private fun refreshRenderConfig(stream: EduStreamInfo, channelId: String, view: TextureView,
                                    config: EduRenderConfig): EduError {
        renderConfigMap[view]?.let {
            if (it != config) {
                var uid: Int = (stream.streamUuid.toLong() and 0xffffffffL).toInt()
                if (stream.publisher.userUuid == userInfo.userUuid) {
                    // Note, rtc video canvas requires that the rtc
                    // media uid should be 0 for local user.
                    uid = 0
                }
                // update renderConfig cache
                renderConfigMap[view] = config
                val code = if (uid == 0) {
                    RteEngineImpl.setLocalRenderMode(config.eduRenderMode.value,
                            config.eduMirrorMode.value)
                } else {
                    RteEngineImpl.setRemoteRenderMode(channelId, uid, config.eduRenderMode.value,
                            config.eduMirrorMode.value)
                }
                return EduError(code, getError(code))
            }
        }
        return customMsgError("renderConfig cache not contains this textureView:$view")
    }

    private fun addTextureView(parent: ViewGroup, textureView: TextureView, renderConfig: EduRenderConfig) {
        parent.addView(textureView)
        if (!textureViewList.contains(textureView)) {
            textureViewList.add(textureView)
        }

        if (!textureViewMap.containsKey(textureView)) {
            textureViewMap[textureView] = parent
        }

        if (!renderConfigMap.containsKey(textureView)) {
            renderConfigMap[textureView] = renderConfig
        }
    }

    private fun removeTextureView(textureView: TextureView) {
        (textureView.parent as? ViewGroup)?.removeView(textureView)
        textureViewList.remove(textureView)
        textureViewMap.remove(textureView)
        renderConfigMap.remove(textureView)
    }

    override fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?, top: Boolean): EduError {
        /*屏幕分享使用fit模式，尽可能的保持画面完整*/
        val config = if (stream.videoSourceType == VideoSourceType.SCREEN) EduRenderMode.FIT else
            EduRenderMode.HIDDEN
        return setStreamView(stream, channelId, viewGroup, EduRenderConfig(config), top)
    }

    internal fun removeAllTextureView() {
        AgoraLog.w("$tag->Clear all SurfaceView")
        if (textureViewList.size > 0) {
            textureViewList.forEach {
                val parent = it.parent
                if (parent != null && parent is ViewGroup) {
                    val context = parent.context
                    if (context != null && context is Activity && !context.isFinishing &&
                            !context.isDestroyed) {
                        context.runOnUiThread(Runnable { parent.removeView(it) })
                    }
                }
            }

            textureViewList.clear()
            textureViewMap.clear()
            renderConfigMap.clear()
        }
    }

    override fun setRoomProperties(properties: MutableMap<String, Any>,
                                   cause: MutableMap<String, String>, callback: EduCallback<Unit>) {
        val req = EduUpsertRoomPropertyReq(properties, cause)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .setRoomProperties(APPID, eduRoom.getCurRoomUuid(), req)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun removeRoomProperties(properties: MutableList<String>, cause: MutableMap<String, String>,
                                      callback: EduCallback<Unit>) {
        val req = EduRemoveRoomPropertyReq(properties, cause)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .removeRoomProperties(APPID, eduRoom.getCurRoomUuid(), req)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun resetVideoEncoderConfig(videoEncoderConfig: EduVideoEncoderConfig): EduError {
        val code = RteEngineImpl.setVideoEncoderConfiguration(convertVideoEncoderConfig(videoEncoderConfig))
        return if (code == OK()) {
            noError()
        } else {
            httpError(code, getError(code))
        }
    }

    override fun startAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
        RteEngineImpl.startAudioMixing(filepath, loopback, replace, cycle)
    }

    override fun stopAudioMixing() {
        RteEngineImpl.stopAudioMixing()
    }

    override fun setAudioMixingPosition(position: Int) {
        RteEngineImpl.setAudioMixingPosition(position)
    }
}
