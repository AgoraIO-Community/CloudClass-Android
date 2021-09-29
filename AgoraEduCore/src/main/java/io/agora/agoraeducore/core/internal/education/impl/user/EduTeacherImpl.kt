package io.agora.agoraeducore.core.internal.education.impl.user

import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.APPID
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.BusinessException
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.httpError
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomState
import io.agora.agoraeducore.core.internal.framework.data.EduStreamInfo
import io.agora.agoraeducore.core.internal.education.api.stream.data.ScreenStreamInitOptions
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.education.impl.util.Convert
import io.agora.agoraeducore.core.internal.education.api.room.data.EduMuteState
import io.agora.agoraeducore.core.internal.education.api.statistics.AgoraError
import io.agora.agoraeducore.core.internal.framework.EduLocalUserInfo
import io.agora.agoraeducore.core.internal.framework.EduTeacherEventListener
import io.agora.agoraeducore.core.internal.education.impl.network.RetrofitManager
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.RoomService
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduDelStreamsBody
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduDelStreamsReq
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduUpsertStreamsBody
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduUpsertStreamsReq
import io.agora.agoraeducore.core.internal.framework.EduTeacher
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.deprecated.StreamService
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomMuteStateReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUserRoomChatMuteReq
import io.agora.agoraeducore.core.internal.server.struct.request.RoleMuteConfig
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.UserService
import io.agora.agoraeducore.core.internal.server.struct.response.DataResponseBody

internal class EduTeacherImpl(
        userInfo: EduLocalUserInfo
) : EduUserImpl(userInfo), EduTeacher {
    override fun setEventListener(eventListener: EduTeacherEventListener?) {
        this.eventListener = eventListener
    }

    override fun updateCourseState(roomState: EduRoomState, callback: EduCallback<Unit>) {
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .updateClassState(APPID, eduRoom.getCurRoomUuid(), roomState.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun allowAllStudentChat(isAllow: Boolean, callback: EduCallback<Unit>) {
        val chatState = if (isAllow) EduMuteState.Enable else EduMuteState.Disable
        val eduRoomStatusReq = EduRoomMuteStateReq(
                RoleMuteConfig(null, chatState.value.toString(), chatState.value.toString()),
                null, null)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), RoomService::class.java)
                .updateRoomMuteStateForRole(APPID, eduRoom.getCurRoomUuid(), eduRoomStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun allowStudentChat(isAllow: Boolean, remoteStudent: EduUserInfo, callback: EduCallback<Unit>) {
        /***/
        val role = Convert.convertUserRole(remoteStudent.role, eduRoom.getCurRoomType())
        val chatState = if (isAllow) EduMuteState.Enable else EduMuteState.Disable
        val eduUserStatusReq = EduUserRoomChatMuteReq(remoteStudent.userName, chatState.value, role)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), UserService::class.java)
                .updateUserMuteState(APPID, eduRoom.getCurRoomUuid(), remoteStudent.userUuid, eduUserStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<String>> {
                    override fun onSuccess(res: DataResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun startShareScreen(options: ScreenStreamInitOptions, callback: EduCallback<EduStreamInfo>) {

    }

    override fun stopShareScreen(callback: EduCallback<Unit>) {

    }

    override fun upsertStudentStreams(streamInfos: MutableList<EduStreamInfo>, callback: EduCallback<Unit>) {
        val streams = mutableListOf<EduUpsertStreamsReq>()
        streamInfos.forEach {
            val eduUpsertStreamReq = EduUpsertStreamsReq(it.publisher.userUuid, it.streamUuid,
                    it.streamName, it.videoSourceType.value, if (it.hasVideo) 1 else 0,
                    if (it.hasAudio) 1 else 0)
            streams.add(eduUpsertStreamReq)
        }
        val body = EduUpsertStreamsBody(streams)

        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
                .upsertStreams(APPID, eduRoom.getCurRoomUuid(), body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.agoraeducore.core.internal.base.network.ResponseBody<String>> {
                    override fun onSuccess(res: io.agora.agoraeducore.core.internal.base.network.ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }

    override fun deleteStudentStreams(streamInfos: MutableList<EduStreamInfo>, callback: EduCallback<Unit>) {
        val streams = mutableListOf<EduDelStreamsReq>()
        streamInfos.forEach {
            val eduDelStreamReq = EduDelStreamsReq(it.publisher.userUuid, it.streamUuid)
            streams.add(eduDelStreamReq)
        }
        val body = EduDelStreamsBody(streams)
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
                .delStreams(APPID, eduRoom.getCurRoomUuid(), body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.agoraeducore.core.internal.base.network.ResponseBody<String>> {
                    override fun onSuccess(res: io.agora.agoraeducore.core.internal.base.network.ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(httpError(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }
}
