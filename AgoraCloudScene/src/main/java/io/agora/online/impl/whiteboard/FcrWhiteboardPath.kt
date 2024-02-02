package io.agora.online.impl.whiteboard

import android.text.TextUtils
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.online.impl.whiteboard.netless.manager.BoardRoom
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * author : felix
 * date : 2022/6/8
 * description :
 */
class FcrWhiteboardPath {
    var coursewareAttributes: String? = null //把课件保存下来，带到分组的时候用到
    var tag = "AgoraWhiteBoard"

    /*获取当前多窗口状态*/
    interface FcrBoardService {
        @GET("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/netlessBoard/windowManager")
        fun getCoursewareAttribute(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
        ): Call<HttpBaseRes<Any>>
    }

    /**
     * 分组带入PPT, web端默认带入笔迹
     */
    fun getCoursewareAttribute(appId: String, listener: ((String) -> Unit)? = null) {
        FCRGroupClassUtils.mainLaunchConfig?.let {
            val call = AppRetrofitManager.getService(FcrBoardService::class.java).getCoursewareAttribute(appId, it.roomUuid)
            AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
                override fun onSuccess(result: HttpBaseRes<Any>?) {
                    coursewareAttributes = result?.data?.let { GsonUtil.toJson(it) }
                    coursewareAttributes?.let {
                        listener?.invoke(it)
                    }
                    LogX.e(tag, "getCoursewareAttribute= ${coursewareAttributes}")
                }

                override fun onError(httpCode: Int, code: Int, message: String?) {
                    //super.onError(httpCode, code, message)
                }
            })
        }
    }

    fun setCoursewareAttribute(mAppid: String, boardRoom: BoardRoom) {
        LogX.e(tag,"setCoursewareAttribute ${coursewareAttributes}")
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