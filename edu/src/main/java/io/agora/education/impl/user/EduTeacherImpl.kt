package io.agora.education.impl.user

import io.agora.education.impl.Constants.Companion.APPID
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.edu.BuildConfig.API_BASE_URL
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError.Companion.httpError
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.ScreenStreamInitOptions
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.util.Convert
import io.agora.education.api.room.data.EduMuteState
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.user.data.EduLocalUserInfo
import io.agora.education.api.user.listener.EduTeacherEventListener
import io.agora.education.impl.network.RetrofitManager
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.data.request.EduDelStreamsBody
import io.agora.education.impl.stream.data.request.EduDelStreamsReq
import io.agora.education.impl.stream.data.request.EduUpsertStreamsBody
import io.agora.education.impl.stream.data.request.EduUpsertStreamsReq
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.EduRoomMuteStateReq
import io.agora.education.impl.user.data.request.EduUserStatusReq
import io.agora.education.impl.user.data.request.RoleMuteConfig
import io.agora.education.impl.user.network.UserService

internal class EduTeacherImpl(
        userInfo: EduLocalUserInfo
) : EduUserImpl(userInfo), EduTeacher {
    override fun setEventListener(eventListener: EduTeacherEventListener?) {
        this.eventListener = eventListener
    }

    override fun updateCourseState(roomState: EduRoomState, callback: EduCallback<Unit>) {
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomState(APPID, eduRoom.getCurRoomUuid(), roomState.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
        RetrofitManager.instance()!!.getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomMuteState(APPID, eduRoom.getCurRoomUuid(), eduRoomStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
        val eduUserStatusReq = EduUserStatusReq(remoteStudent.userName, chatState.value, role)
        RetrofitManager.instance()!!.getService(API_BASE_URL, UserService::class.java)
                .updateUserMuteState(APPID, eduRoom.getCurRoomUuid(), remoteStudent.userUuid, eduUserStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
        RetrofitManager.instance()!!.getService(API_BASE_URL, StreamService::class.java)
                .upsertStreams(APPID, eduRoom.getCurRoomUuid(), body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
        RetrofitManager.instance()!!.getService(API_BASE_URL, StreamService::class.java)
                .delStreams(APPID, eduRoom.getCurRoomUuid(), body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
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
