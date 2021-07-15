package io.agora.edu.common.impl

import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager
import io.agora.edu.common.api.Base
import io.agora.edu.common.api.FlexProps
import io.agora.edu.common.bean.flexpropes.RoomFlexPropsReq
import io.agora.edu.common.bean.flexpropes.UserFlexPropsReq
import io.agora.edu.common.service.FlexPropsService
import io.agora.edu.launch.AgoraEduSDK
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.customMsgError

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