package io.agora.edu.core.internal.edu.common.impl

import io.agora.edu.core.internal.base.callback.ThrowableCallback
import io.agora.edu.core.internal.base.network.BusinessException
import io.agora.edu.core.internal.base.network.ResponseBody
import io.agora.edu.core.internal.base.network.RetrofitManager
import io.agora.edu.core.internal.edu.common.api.Base
import io.agora.edu.core.internal.edu.common.api.FlexProps
import io.agora.edu.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.edu.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.FlexPropsService
import io.agora.edu.core.internal.launch.AgoraEduSDK
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.data.EduError.Companion.customMsgError

class FlexPropsImpl(
        appId: String,
        roomUuid: String) : Base(appId, roomUuid), FlexProps {

    override fun updateFlexRoomProperties(reqFlex: RoomFlexPropsReq, callback: EduCallback<Boolean>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), FlexPropsService::class.java)
                .updateFlexRoomProps(appId, roomUuid, reqFlex)
                .enqueue(RetrofitManager.Callback<ResponseBody<String?>>(0, object : ThrowableCallback<ResponseBody<String?>?> {
                    override fun onSuccess(res: ResponseBody<String?>?) {
                        res?.let {
                            callback.onSuccess(true)
                            return
                        }
                        callback.onFailure(EduError.customMsgError("response is null"))
                    }

                    override fun onFailure(throwable: Throwable?) {
                        if (throwable is BusinessException) {
                            callback.onFailure(EduError(throwable.code, throwable.message!!))
                        } else {
                            callback.onFailure(customMsgError(throwable!!.message))
                        }
                    }
                }))
    }

    override fun updateFlexUserProperties(userUuid: String, reqFlex: UserFlexPropsReq, callback: EduCallback<Boolean>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), FlexPropsService::class.java)
                .updateFlexUserProps(appId, roomUuid, userUuid, reqFlex)
                .enqueue(RetrofitManager.Callback<ResponseBody<String?>>(0, object : ThrowableCallback<ResponseBody<String?>?> {
                    override fun onSuccess(res: ResponseBody<String?>?) {
                        res?.let {
                            callback.onSuccess(true)
                            return
                        }
                        callback.onFailure(EduError.customMsgError("response is null"))
                    }

                    override fun onFailure(throwable: Throwable?) {
                        if (throwable is BusinessException) {
                            callback.onFailure(EduError(throwable.code, throwable.message!!))
                        } else {
                            callback.onFailure(customMsgError(throwable!!.message))
                        }
                    }
                }))
    }
}