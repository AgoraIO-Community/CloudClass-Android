package io.agora.agoraeduuikit.impl.whiteboard

import android.text.TextUtils
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * author : hefeng
 * date : 2022/6/8
 * description :
 */
class FcrWhiteboardPath {
    var coursewareAttributes: String? = null //把课件保存下来，带到分组的时候用到
    var tag = "FcrWhiteboard"

    /*获取当前多窗口状态*/
    interface FcrBoardService {
        @GET("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/netlessBoard/windowManager")
        fun getCoursewareAttribute(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
        ): Call<ResponseBody<Any>>
    }

    /**
     * 分组带入PPT, web端默认带入笔迹
     */
    fun getCoursewareAttribute(appId: String, listener: ((String) -> Unit)? = null) {
        FCRGroupClassUtils.mainLaunchConfig?.let {
            RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), FcrBoardService::class.java)
                .getCoursewareAttribute(appId, it.roomUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Any>> {
                    override fun onSuccess(res: ResponseBody<Any>?) {
                        coursewareAttributes = res?.data?.let { GsonUtil.toJson(it) }
                        Constants.AgoraLog?.e("$tag-> getCoursewareAttribute ${coursewareAttributes}")
                        coursewareAttributes?.let {
                            listener?.invoke(it)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        Constants.AgoraLog?.e("$tag->${throwable?.let { GsonUtil.toJson(it) }}")
                    }
                }))
        }
    }

    fun setCoursewareAttribute(mAppid: String, boardRoom: BoardRoom) {
        Constants.AgoraLog?.e("$tag-> setCoursewareAttribute ${coursewareAttributes}")
        if (!TextUtils.isEmpty(coursewareAttributes)) {
            try { // 带入笔迹
                boardRoom.setWindowManagerAttributes(coursewareAttributes)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else {
            getCoursewareAttribute(mAppid) {
                try { // 带入笔迹
                    boardRoom.setWindowManagerAttributes(it)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}