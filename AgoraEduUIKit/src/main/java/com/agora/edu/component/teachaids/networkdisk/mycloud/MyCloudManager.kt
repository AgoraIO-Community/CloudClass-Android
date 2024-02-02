package com.agora.edu.component.teachaids.networkdisk.mycloud

import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudDelFileReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudPresignedUrlsReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudUserAndResourceReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.res.MyCloudCoursewareRes
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors.ResponseIsEmpty
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK.baseUrl

/**
 * author : cjw
 * date : 2022/3/18
 * description :
 */
internal class MyCloudManager(val appId: String, val userUuid: String) {
    private val TAG = "MyCloudManager"

    private val rootUuid = "root"
    private val defaultPageSize = 10

    fun getCloudService(): MyCloudService {
        return AppRetrofitManager.instance().getService(baseUrl(), MyCloudService::class.java)
    }

    fun fetchCoursewareWithPage(
        resourceName: String? = null,
        page: Int,
        pageSize: Int = defaultPageSize,
        callback: EduContextCallback<MyCloudCoursewareRes>
    ) {
        val call = getCloudService().fetchCoursewares(
            appId = appId, userUuid = userUuid, parentResourceUuid = rootUuid, resourceName = resourceName,
            pageNo = page, pageSize = pageSize
        )
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<MyCloudCoursewareRes>>() {
            override fun onSuccess(res: HttpBaseRes<MyCloudCoursewareRes>?) {
                if (res != null) {
                    callback.onSuccess(res.data)
                } else {
                    callback.onFailure(ResponseIsEmpty)
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                callback.onFailure(EduContextError(code, message ?: ""))
            }
        })
    }

    fun presignedUrls(
        params: MutableList<MyCloudPresignedUrlsReq>,
        callback: EduContextCallback<List<MyCloudPresignedUrlsRes>>
    ) {
        val call = getCloudService().presignedUrls(
            appId = appId, userUuid = userUuid, params = params
        )
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<List<MyCloudPresignedUrlsRes>>>() {
            override fun onSuccess(res: HttpBaseRes<List<MyCloudPresignedUrlsRes>>?) {
                if (res != null) {
                    callback.onSuccess(res.data)
                } else {
                    callback.onFailure(ResponseIsEmpty)
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                callback.onFailure(EduContextError(code, message ?: ""))
            }
        })
    }

    fun buildUserAndResource(
        resourceUuid: String,
        params: MyCloudUserAndResourceReq,
        callback: EduContextCallback<Any>
    ) {
        val call = getCloudService().buildUserAndResource(
            appId = appId, userUuid = userUuid, resourceUuid = resourceUuid, params = params
        )
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(res: HttpBaseRes<Any>?) {
                if (res != null) {
                    callback.onSuccess(res.data)
                } else {
                    callback.onFailure(ResponseIsEmpty)
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                callback.onFailure(EduContextError(code, message ?: ""))
            }
        })
    }

    fun delFileRequest(
        params: List<MyCloudDelFileReq>,
        callback: EduContextCallback<Any>
    ) {
        val call = getCloudService().deleteFileRequest(
            appId = appId, userUuid = userUuid, params = params
        )
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(res: HttpBaseRes<Any>?) {
                if (res != null) {
                    callback.onSuccess(res.data)
                } else {
                    callback.onFailure(ResponseIsEmpty)
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                callback.onFailure(EduContextError(code, message ?: ""))
            }
        })
    }
}