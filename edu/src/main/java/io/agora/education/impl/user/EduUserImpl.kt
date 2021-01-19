package io.agora.education.impl.user

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.Gson
import io.agora.education.impl.Constants.Companion.APPID
import io.agora.education.impl.Constants.Companion.AgoraLog
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.edu.BuildConfig.API_BASE_URL
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.customMsgError
import io.agora.education.api.base.EduError.Companion.httpError
import io.agora.education.api.base.EduError.Companion.mediaError
import io.agora.education.api.base.EduError.Companion.parameterError
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduChatMsgType
import io.agora.education.api.message.EduMsg
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduActionConfig
import io.agora.education.api.user.data.EduLocalUserInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.network.RetrofitManager
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.request.EduRemoveRoomPropertyReq
import io.agora.education.impl.room.data.request.EduUpsertRoomPropertyReq
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.*
import io.agora.education.impl.user.network.UserService
import io.agora.education.impl.util.Convert
import io.agora.education.impl.util.Convert.convertFromUserInfo
import io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
import io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rte.RteEngineImpl
import io.agora.rte.RteEngineImpl.OK
import io.agora.rte.RteEngineImpl.getError

internal open class EduUserImpl(
        override var userInfo: EduLocalUserInfo
) : EduUser {
    val TAG = EduUserImpl::class.java.simpleName

    override var videoEncoderConfig = VideoEncoderConfig()

    override var eventListener: EduUserEventListener? = null

    lateinit var eduRoom: EduRoomImpl

    private val surfaceViewList = mutableListOf<SurfaceView>()

    override fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        if (TextUtils.isEmpty(options.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        AgoraLog.i("$TAG->Start initOrUpdateLocalStream:${Gson().toJson(options)}")
        val a = RteEngineImpl.setVideoEncoderConfiguration(
                Convert.convertVideoEncoderConfig(videoEncoderConfig))
        if (a != OK()) {
            callback.onFailure(mediaError(a, getError(a)))
            return
        }
        val b = RteEngineImpl.enableVideo()
        if (b != OK()) {
            callback.onFailure(mediaError(b, getError(b)))
            return
        }
        /**enableCamera和enableMicrophone控制是否打开摄像头和麦克风*/
        val c = RteEngineImpl.enableLocalMedia(options.enableMicrophone, options.enableCamera)
        if (c != OK()) {
            callback.onFailure(mediaError(c, getError(c)))
            return
        }

        /**根据当前配置生成一个流信息*/
        val streamInfo = EduStreamInfoImpl(options.streamUuid, options.streamName, VideoSourceType.CAMERA,
                options.enableCamera, options.enableMicrophone, this.userInfo, System.currentTimeMillis())
        callback.onSuccess(streamInfo)
    }

    override fun switchCamera(): EduError? {
        val code = RteEngineImpl.switchCamera()
        AgoraLog.i("$TAG->switchCamera:$code")
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
        Log.e(TAG, "")
        val code = RteEngineImpl.muteRemoteStream(eduRoom.getCurRoomUuid(), uid, !options.subscribeAudio,
                !options.subscribeVideo)
        AgoraLog.i("$TAG->subscribeStream: streamUuid:${stream.streamUuid},audio:${options.subscribeAudio}," +
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
        AgoraLog.i("$TAG->unSubscribeStream: streamUuid: ${stream.streamUuid},audio:${options.subscribeAudio}," +
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
        AgoraLog.logMsg("$TAG->Create new Stream: ${Gson().toJson(stream)}", LogLevel.INFO.value)
        RetrofitManager.instance()!!.getService(API_BASE_URL, StreamService::class.java)
                .createStream(APPID, eduRoom.getCurRoomUuid(), userInfo.userUuid,
                        stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        AgoraLog.logMsg("$TAG->publishStream state: streamUuid: ${stream.streamUuid}," +
                                "${stream.hasAudio},${stream.hasVideo}",
                                LogLevel.INFO.value)
                        val a = RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(), CLIENT_ROLE_BROADCASTER)
                        if (a != OK()) {
                            callback.onFailure(mediaError(a, getError(a)))
                            return
                        }
                        val b = RteEngineImpl.muteLocalStream(!stream.hasAudio, !stream.hasVideo)
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

    override fun muteStream(stream: EduStreamInfo, callback: EduCallback<Boolean>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        val index = Convert.streamExistsInList(stream, eduRoom.getCurStreamList())
        if (index > -1) {
            val oldStream = eduRoom.getCurStreamList()[index]
            if (oldStream == stream) {
                AgoraLog.e("$TAG->Wanted state of the stream to be updated is same with the current state，return")
                callback.onSuccess(true)
            } else {
                AgoraLog.i("$TAG->Start updating locally existing stream info, streamUuid: + " +
                        "${stream.streamUuid}, Wanted state is:${stream.hasAudio},${stream.hasVideo}")
                /**设置角色*/
                val a = RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(), CLIENT_ROLE_BROADCASTER)
                if (a != OK()) {
                    callback.onFailure(mediaError(a, getError(a)))
                    return
                }
                val b = RteEngineImpl.muteLocalStream(!stream.hasAudio, !stream.hasVideo)
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
                RetrofitManager.instance()!!.getService(API_BASE_URL, StreamService::class.java)
                        .updateStreamInfo(APPID, eduRoom.getCurRoomUuid(), userInfo.userUuid,
                                stream.streamUuid, eduStreamStatusReq)
                        .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                            override fun onSuccess(res: ResponseBody<String>?) {
//                                (streamInfo as EduStreamInfoImpl).updateTime = res?.timeStamp
                                AgoraLog.i("$TAG->Update stream info success, streamUuid: + ${stream.streamUuid}")
                                callback.onSuccess(true)
                            }

                            override fun onFailure(throwable: Throwable?) {
                                AgoraLog.e("$TAG->Update stream info failed,streamUuid: + ${stream.streamUuid}")
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
            AgoraLog.e("$TAG->${error.msg}")
            callback.onFailure(error)
        }
    }

    override fun unPublishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>) {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            callback.onFailure(parameterError("streamUuid"))
            return
        }
        AgoraLog.i("$TAG->Del stream:${stream.streamUuid}")
        RetrofitManager.instance()!!.getService(API_BASE_URL, StreamService::class.java)
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
                        val b = RteEngineImpl.muteLocalStream(muteAudio = true, muteVideo = true)
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

    override fun sendRoomMessage(message: String, callback: EduCallback<EduMsg>) {
        AgoraLog.i("$TAG->sendRoomMessage:$message")
        val roomMsgReq = EduRoomMsgReq(message)
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .sendChannelCustomMessage(APPID, eduRoom.getCurRoomUuid(), roomMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(convertFromUserInfo(userInfo), message,
                                System.currentTimeMillis())
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

    override fun sendUserMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduMsg>) {
        AgoraLog.i("$TAG->sendUserMessage:$message,remoteUserUuid:${remoteUser.userUuid}")
        val userMsgReq = EduUserMsgReq(message)
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .sendPeerCustomMessage(APPID, eduRoom.getCurRoomUuid(), remoteUser.userUuid, userMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(convertFromUserInfo(userInfo), message,
                                System.currentTimeMillis())
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

    override fun sendRoomChatMessage(message: String, callback: EduCallback<EduChatMsg>) {
        AgoraLog.i("$TAG->sendRoomChatMessage:$message")
        val roomChatMsgReq = EduRoomChatMsgReq(message, EduChatMsgType.Text.value)
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .sendRoomChatMsg(eduRoom.getCurLocalUser().userInfo.userToken!!, APPID,
                        eduRoom.getCurRoomUuid(), roomChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduChatMsg(convertFromUserInfo(userInfo), message,
                                System.currentTimeMillis(), EduChatMsgType.Text.value)
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

    override fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduChatMsg>) {
        AgoraLog.i("$TAG->sendUserChatMessage:$message,remoteUserUuid:${remoteUser.userUuid}")
        val userChatMsgReq = EduUserChatMsgReq(message, EduChatMsgType.Text.value)
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .sendPeerChatMsg(APPID, eduRoom.getCurRoomUuid(), remoteUser.userUuid, userChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduChatMsg(convertFromUserInfo(userInfo), message,
                                System.currentTimeMillis(), EduChatMsgType.Text.value)
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
        AgoraLog.i("$TAG->startActionWithConfig:${Gson().toJson(config)}")
        val actionReq = EduActionReq(userInfo.userUuid, ReqPayload(config.action.value,
                ReqUser(userInfo.userUuid, userInfo.userName, userInfo.role.name),
                ReqRoom(eduRoom.getCurRoomInfo().roomName, eduRoom.getCurRoomUuid())))
        RetrofitManager.instance()!!.getService(API_BASE_URL, UserService::class.java)
                .doAction(APPID, eduRoom.getCurRoomUuid(), config.toUserUuid, config.processUuid, actionReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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

    override fun stopActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(config.processUuid)) {
            callback.onFailure(parameterError("processUuid"))
            return
        }
        AgoraLog.i("$TAG->stopActionWithConfig:${Gson().toJson(config)}")
        val actionReq = EduActionReq(userInfo.userUuid, ReqPayload(config.action.value,
                ReqUser(userInfo.userUuid, userInfo.userName, userInfo.role.name),
                ReqRoom(eduRoom.getCurRoomInfo().roomName, eduRoom.getCurRoomUuid())))
        RetrofitManager.instance()!!.getService(API_BASE_URL, UserService::class.java)
                .doAction(APPID, eduRoom.getCurRoomUuid(), config.toUserUuid, eduRoom.getCurRoomUuid(), actionReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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

    /**
     * @param viewGroup 视频画面的父布局(在UI布局上最好保持独立)*/
    override fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?,
                               config: VideoRenderConfig): EduError {
        if (TextUtils.isEmpty(stream.streamUuid)) {
            return parameterError("streamUuid")
        }
        if (TextUtils.isEmpty(stream.publisher.userUuid)) {
            return parameterError("publisher'userUuid")
        }
        if (TextUtils.isEmpty(channelId)) {
            return parameterError("channelId")
        }
        val videoCanvas: VideoCanvas
        if (viewGroup == null) {
            /**remove掉当前流对应的surfaceView*/
            val uid: Int = (stream.streamUuid.toLong() and 0xffffffffL).toInt()
            videoCanvas = VideoCanvas(null, config.renderMode.value, uid)
            val iterable = surfaceViewList.iterator()
            while (iterable.hasNext()) {
                val surfaceView = iterable.next()
                if (stream.streamUuid == surfaceView.tag && surfaceView.parent != null) {
                    (surfaceView.parent as ViewGroup).removeView(surfaceView)
                }
                iterable.remove()
            }
        } else {
            checkAndRemoveSurfaceView(stream.streamUuid)?.let {
                viewGroup.removeView(it)
            }
            val appContext = viewGroup.context.applicationContext
            val surfaceView = RtcEngine.CreateRendererView(appContext)
            surfaceView.tag = stream.streamUuid
            surfaceView.setZOrderMediaOverlay(true)
            val layoutParams = ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            surfaceView.layoutParams = layoutParams
            var uid: Int = ((stream.streamUuid.toLong()) and 0xffffffffL).toInt()
            if (stream.publisher.userUuid == this.userInfo.userUuid) {
                uid = 0
            }
            videoCanvas = VideoCanvas(surfaceView, config.renderMode.value, channelId, uid)
            viewGroup.addView(surfaceView)
            surfaceViewList.add(surfaceView)
        }
        var code: Int
        if (stream.publisher.userUuid == this.userInfo.userUuid) {
            code = RteEngineImpl.setupLocalVideo(videoCanvas)
            if (code == 0) {
                AgoraLog.e("$TAG->setupLocalVideo success")
            }
        } else {
            code = RteEngineImpl.setupRemoteVideo(videoCanvas)
            if (code == 0) {
                AgoraLog.e("$TAG->setupRemoteVideo success")
            }
        }
        return EduError(code, getError(code))
    }

    override fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?): EduError {
        /*屏幕分享使用fit模式，尽可能的保持画面完整*/
        val config = if (stream.videoSourceType == VideoSourceType.SCREEN) RenderMode.FIT else
            RenderMode.HIDDEN
        return setStreamView(stream, channelId, viewGroup, VideoRenderConfig(config))
    }

    internal fun removeAllSurfaceView() {
        AgoraLog.w("$TAG->Clear all SurfaceView")
        if (surfaceViewList.size > 0) {
            surfaceViewList.forEach {
                val parent = it.parent
                if (parent != null && parent is ViewGroup) {
                    val context = parent.context
                    if (context != null && context is Activity && !context.isFinishing &&
                            !context.isDestroyed) {
                        context.runOnUiThread(object : Runnable {
                            override fun run() {
                                parent.removeView(it)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun checkAndRemoveSurfaceView(tag: String): SurfaceView? {
        for (surfaceView in surfaceViewList) {
            if (surfaceView.tag == tag) {
                surfaceViewList.remove(surfaceView)
                return surfaceView
            }
        }
        return null
    }

    override fun setRoomProperties(properties: MutableMap<String, Any>,
                                   cause: MutableMap<String, String>, callback: EduCallback<Unit>) {
        val req = EduUpsertRoomPropertyReq(properties, cause)
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .setRoomProperties(APPID, eduRoom.getCurRoomUuid(), req)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .removeRoomProperties(APPID, eduRoom.getCurRoomUuid(), req)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
}
