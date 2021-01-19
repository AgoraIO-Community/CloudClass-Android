package io.agora.education.impl.user

import android.text.TextUtils
import io.agora.education.impl.Constants.Companion.APPID
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.edu.BuildConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.stream.data.AudioSourceType
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduAssistant
import io.agora.education.api.user.data.EduLocalUserInfo
import io.agora.education.api.user.listener.EduAssistantEventListener
import io.agora.education.impl.network.RetrofitManager
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.EduStreamStatusReq

internal class EduAssistantImpl(
        userInfo: EduLocalUserInfo
) : EduUserImpl(userInfo), EduAssistant {
    override fun setEventListener(eventListener: EduAssistantEventListener?) {
        this.eventListener = eventListener
    }

    override fun createOrUpdateTeacherStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(streamInfo.streamUuid)) {
            callback.onFailure(EduError.parameterError("streamUuid"))
            return
        }
        upsertStream(streamInfo, callback)
    }

    override fun createOrUpdateStudentStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>) {
        if (TextUtils.isEmpty(streamInfo.streamUuid)) {
            callback.onFailure(EduError.parameterError("streamUuid"))
            return
        }
        upsertStream(streamInfo, callback)
    }

    private fun upsertStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>) {
        val userUuid = streamInfo.publisher.userUuid
        val req = EduStreamStatusReq(streamInfo.streamName, streamInfo.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (streamInfo.hasVideo) 1 else 0,
                if (streamInfo.hasAudio) 1 else 0)
        RetrofitManager.instance()!!.getService(BuildConfig.API_BASE_URL, StreamService::class.java)
                .upsertStream(APPID, eduRoom.getCurRoomUuid(), userUuid, streamInfo.streamUuid, req)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(EduError.httpError(error?.code
                                ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message))
                    }
                }))
    }
}