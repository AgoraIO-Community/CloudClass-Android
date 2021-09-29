package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.deprecated

import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface HandsUpService {
    @POST("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    fun applyHandsUp(
            @Path("appId") appId: String?,
            @Path("roomUUid") roomUuid: String?
    ): Call<ResponseBody<Int?>?>?

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    fun cancelApplyHandsUp(
            @Path("appId") appId: String?,
            @Path("roomUUid") roomUuid: String?
    ): Call<ResponseBody<Int?>?>?

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/acceptance")
    fun exitHandsUp(
            @Path("appId") appId: String?,
            @Path("roomUUid") roomUuid: String?
    ): Call<ResponseBody<Int?>?>?
}