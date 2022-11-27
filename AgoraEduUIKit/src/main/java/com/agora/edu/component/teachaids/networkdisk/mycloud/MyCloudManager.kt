package com.agora.edu.component.teachaids.networkdisk.mycloud

import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudDelFileReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudPresignedUrlsReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudUserAndResourceReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.res.MyCloudCoursewareRes
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors.DefaultError
import io.agora.agoraeducore.core.context.EduContextErrors.ResponseIsEmpty
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.BusinessException
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK.baseUrl
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : cjw
 * date : 2022/3/18
 * description :
 */
internal class MyCloudManager(val appId: String, val userUuid: String) {
    private val TAG = "MyCloudManager"

    private val rootUuid = "root"
    private val defaultPageSize = 10
    private val service = RetrofitManager.instance().getService(baseUrl(), MyCloudService::class.java)

    fun fetchCoursewareWithPage(
        resourceName: String? = null,
        page: Int,
        pageSize: Int = defaultPageSize,
        callback: EduContextCallback<MyCloudCoursewareRes>
    ) {
        service.fetchCoursewares(
            appId = appId, userUuid = userUuid, parentResourceUuid = rootUuid, resourceName = resourceName,
            pageNo = page, pageSize = pageSize
        )
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<MyCloudCoursewareRes>> {
                override fun onSuccess(res: ResponseBody<MyCloudCoursewareRes>?) {
                    if (res != null) {
                        callback.onSuccess(res.data)
                    } else {
                        callback.onFailure(ResponseIsEmpty)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    LogX.e(TAG,"fetchCoursewareWithPage-failed:${throwable?.let { GsonUtil.toJson(throwable) }}")
                    if (throwable is BusinessException) {
                        callback.onFailure(EduContextError(throwable.code, throwable.message!!))
                    } else {
                        callback.onFailure(DefaultError)
                    }
                }
            }))
    }

    fun presignedUrls(
        params: MutableList<MyCloudPresignedUrlsReq>,
        callback: EduContextCallback<List<MyCloudPresignedUrlsRes>>
    ) {
        service.presignedUrls(
            appId = appId, userUuid = userUuid, params = params
        )
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<List<MyCloudPresignedUrlsRes>>> {
                override fun onSuccess(res: ResponseBody<List<MyCloudPresignedUrlsRes>>?) {
                    if (res != null) {
                        callback.onSuccess(res.data)
                    } else {
                        callback.onFailure(ResponseIsEmpty)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    LogX.e(TAG,"fetchCoursewareWithPage-failed:${throwable?.let { GsonUtil.toJson(throwable) }}")
                    if (throwable is BusinessException) {
                        callback.onFailure(EduContextError(throwable.code, throwable.message!!))
                    } else {
                        callback.onFailure(DefaultError)
                    }
                }
            }))
    }

    fun buildUserAndResource(
        resourceUuid: String,
        params: MyCloudUserAndResourceReq,
        callback: EduContextCallback<Any>
    ) {
        service.buildUserAndResource(
            appId = appId, userUuid = userUuid, resourceUuid = resourceUuid,params = params
        )
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Any>> {
                override fun onSuccess(res: ResponseBody<Any>?) {
                    if (res != null) {
                        callback.onSuccess(res.data)
                    } else {
                        callback.onFailure(ResponseIsEmpty)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    LogX.e(TAG,"fetchCoursewareWithPage-failed:${throwable?.let { GsonUtil.toJson(throwable) }}")
                    if (throwable is BusinessException) {
                        callback.onFailure(EduContextError(throwable.code, throwable.message!!))
                    } else {
                        callback.onFailure(DefaultError)
                    }
                }
            }))
    }

    fun delFileRequest(
        params: List<MyCloudDelFileReq>,
        callback: EduContextCallback<Any>
    ) {
        service.deleteFileRequest(
            appId = appId, userUuid = userUuid, params = params
        )
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Any>> {
                override fun onSuccess(res: ResponseBody<Any>?) {
                    if (res != null) {
                        callback.onSuccess(res.data)
                    } else {
                        callback.onFailure(ResponseIsEmpty)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    LogX.e(TAG,"fetchCoursewareWithPage-failed:${throwable?.let { GsonUtil.toJson(throwable) }}")
                    if (throwable is BusinessException) {
                        callback.onFailure(EduContextError(throwable.code, throwable.message!!))
                    } else {
                        callback.onFailure(DefaultError)
                    }
                }
            }))
    }
}