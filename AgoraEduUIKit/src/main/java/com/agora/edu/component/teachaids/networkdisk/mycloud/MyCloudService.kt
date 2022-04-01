package com.agora.edu.component.teachaids.networkdisk.mycloud

import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * author : cjw
 * date : 2022/3/18
 * description :
 */
internal interface MyCloudService {
    @GET("/edu/apps/{appId}/v2/users/{userUuid}/resources/page")
    fun fetchCoursewares(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Query("parentResourceUuid") parentResourceUuid: String,
        @Query("resourceName") resourceName: String?,
        @Query("pageSize") pageSize: Int,
        @Query("pageNo") pageNo: Int
    ): Call<ResponseBody<MyCloudCoursewareRes>>
}