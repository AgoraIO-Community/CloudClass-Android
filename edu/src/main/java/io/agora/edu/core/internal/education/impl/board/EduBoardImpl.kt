package io.agora.edu.core.internal.education.impl.board

import io.agora.edu.core.internal.education.impl.Constants
import io.agora.edu.core.internal.base.callback.ThrowableCallback
import io.agora.edu.core.internal.base.network.RetrofitManager
import io.agora.edu.core.internal.launch.AgoraEduSDK
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.education.api.board.EduBoard
import io.agora.edu.core.internal.education.api.board.data.EduBoardInfo
import io.agora.edu.core.internal.framework.EduUserInfo
import io.agora.edu.core.internal.education.impl.network.ResponseBody
import io.agora.edu.core.internal.education.impl.board.data.request.BoardRoomStateReq
import io.agora.edu.core.internal.education.impl.board.data.request.BoardUserStateReq
import io.agora.edu.core.internal.education.impl.board.data.response.BoardRoomRes
import io.agora.edu.core.internal.education.impl.board.network.BoardService

class EduBoardImpl : EduBoard() {
    override fun followMode(enable: Boolean, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), BoardService::class.java)
                .updateBoardRoomState("", Constants.APPID, "", BoardRoomStateReq(if (enable) 1 else 0))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Nothing>> {
                    override fun onSuccess(res: ResponseBody<Nothing>?) {

                    }

                    override fun onFailure(throwable: Throwable?) {

                    }
                }))
    }

    override fun grantPermission(user: EduUserInfo, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), BoardService::class.java)
                .updateBoardUserState("", Constants.APPID, "", user.userUuid, BoardUserStateReq(1))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Nothing>> {
                    override fun onSuccess(res: ResponseBody<Nothing>?) {

                    }

                    override fun onFailure(throwable: Throwable?) {

                    }
                }))
    }

    override fun revokePermission(user: EduUserInfo, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), BoardService::class.java)
                .updateBoardUserState("", Constants.APPID, "", user.userUuid, BoardUserStateReq(0))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Nothing>> {
                    override fun onSuccess(res: ResponseBody<Nothing>?) {

                    }

                    override fun onFailure(throwable: Throwable?) {

                    }
                }))
    }

    override fun getBoardInfo(callback: EduCallback<EduBoardInfo>) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), BoardService::class.java)
                .getBoardRoom("", Constants.APPID, "")
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<BoardRoomRes>> {
                    override fun onSuccess(res: ResponseBody<BoardRoomRes>?) {

                    }

                    override fun onFailure(throwable: Throwable?) {

                    }
                }))
    }
}
