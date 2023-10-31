package io.agora.online.component.teachaids.networkdisk.mycloud

import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors.ResponseIsEmpty
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback

/**
 * author : cjw
 * date : 2022/3/18
 * description :
 */
internal class MyCloudManager(val appId: String, val userUuid: String) {
    private val TAG = "MyCloudManager"

    private val rootUuid = "root"
    private val defaultPageSize = 10
    private val service = AppRetrofitManager.getService(MyCloudService::class.java)

    fun fetchCoursewareWithPage(
        resourceName: String? = null,
        page: Int,
        pageSize: Int = defaultPageSize,
        callback: EduContextCallback<MyCloudCoursewareRes>
    ) {
        val call = service.fetchCoursewares(
            appId = appId, userUuid = userUuid, parentResourceUuid = rootUuid, resourceName = resourceName,
            pageNo = page, pageSize = pageSize
        )
        AppRetrofitManager.exc(call,object : HttpCallback<HttpBaseRes<MyCloudCoursewareRes>>(){
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
}