package io.agora.edu.core.internal.education.impl.user

import android.text.TextUtils
import io.agora.edu.core.internal.education.impl.Constants.Companion.APPID
import io.agora.edu.core.internal.base.callback.ThrowableCallback
import io.agora.edu.core.internal.base.network.BusinessException
import io.agora.edu.core.internal.base.network.ResponseBody
import io.agora.edu.core.internal.launch.AgoraEduSDK
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.education.api.statistics.AgoraError
import io.agora.edu.core.internal.framework.data.AudioSourceType
import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.framework.EduLocalUserInfo
import io.agora.edu.core.internal.framework.EduAssistantEventListener
import io.agora.edu.core.internal.education.impl.network.RetrofitManager
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.StreamService
import io.agora.edu.core.internal.education.impl.user.data.request.EduStreamStatusReq
import io.agora.edu.core.internal.framework.EduAssistant

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
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), StreamService::class.java)
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