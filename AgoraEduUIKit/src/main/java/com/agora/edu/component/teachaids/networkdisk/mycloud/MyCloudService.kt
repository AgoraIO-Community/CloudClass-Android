package com.agora.edu.component.teachaids.networkdisk.mycloud

import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudDelFileReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudPresignedUrlsReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudUserAndResourceReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.res.MyCloudCoursewareRes
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import java.io.File

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
    ): Call<ResponseBody<MyCloudCoursewareRes>>

//    /edu/apps/{appId}/v3/users/{userUuid}/presignedUrls
//    MyCloudPresignedUrlsRes

    @POST("/edu/apps/{appId}/v3/users/{userUuid}/presignedUrls")
    fun presignedUrls(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Body params:MutableList<MyCloudPresignedUrlsReq>
    ): Call<ResponseBody<List<MyCloudPresignedUrlsRes>>>

    @PUT
    fun uploadFile(
        @Url url:String ,
        @Body body: RequestBody
    ): Call<ResponseBody<String>>

    @POST("/edu/apps/{appId}/v4/users/{userUuid}/resources/{resourceUuid}")
    fun buildUserAndResource(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Path("resourceUuid") resourceUuid: String,
        @Body params:MyCloudUserAndResourceReq
    ): Call<ResponseBody<Any>>

    @HTTP(method = "DELETE",path = "/cn/edu/apps/{appId}/v3/users/{userUuid}/resources",hasBody = true)
    fun deleteFileRequest(
        @Path("appId") appId: String,
        @Path("userUuid") userUuid: String,
        @Body params: List<MyCloudDelFileReq>
    ): Call<ResponseBody<Any>>


}