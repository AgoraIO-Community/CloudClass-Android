package com.agora.edu.component.teachaids.networkdisk.mycloud

import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudDelFileReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudPresignedUrlsReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudUserAndResourceReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.res.MyCloudCoursewareRes
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

/**
 * author : cjw
 * date : 2022/3/18
 * description :
 */
internal interface MyCloudService {
    @GET("edu/apps/{appId}/v3/users/{userUuid}/resources/page")
    fun fetchCoursewares(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Query("parentResourceUuid") parentResourceUuid: String,
        @Query("resourceName") resourceName: String?,
        @Query("pageSize") pageSize: Int,
        @Query("pageNo") pageNo: Int
    ): Call<HttpBaseRes<MyCloudCoursewareRes>>

    @POST("edu/apps/{appId}/v3/users/{userUuid}/presignedUrls")
    fun presignedUrls(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Body params: MutableList<MyCloudPresignedUrlsReq>
    ): Call<HttpBaseRes<List<MyCloudPresignedUrlsRes>>>

    @PUT
    fun uploadFile(
        @Header("Authorization") auth: String = "",
        @Header("x-agora-token") token: String = "",
        @Header("x-agora-uid") uid: String = "",
        @Url url: String,
        @Body body: RequestBody
    ): Call<HttpBaseRes<String>>

    @POST("edu/apps/{appId}/v4/users/{userUuid}/resources/{resourceUuid}")
    fun buildUserAndResource(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Path("resourceUuid") resourceUuid: String,
        @Body params: MyCloudUserAndResourceReq
    ): Call<HttpBaseRes<Any>>

    @HTTP(method = "DELETE", path = "edu/apps/{appId}/v3/users/{userUuid}/resources", hasBody = true)
    fun deleteFileRequest(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Body params: List<MyCloudDelFileReq>
    ): Call<HttpBaseRes<Any>>
}