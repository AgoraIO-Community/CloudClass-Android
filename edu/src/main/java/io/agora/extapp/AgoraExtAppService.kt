package io.agora.extapp

import io.agora.edu.common.bean.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PUT
import retrofit2.http.Path

interface AgoraExtAppService {

    @PUT("/edu/apps/{appId}/v2/rooms/{roomUUid}/extApps/{extAppUuid}/properties")
    fun setProperties(
        @Path("appId") appId:String,
        @Path("roomUUid") roomUuid:String,
        @Path("extAppUuid") extAppUuid:String,
        @Body body: AgoraExtAppUpdateRequest
    ) : Call<ResponseBody<String>>

    @HTTP(method = "DELETE", path = "/edu/apps/{appId}/v2/rooms/{roomUUid}/extApps/{extAppUuid}/properties", hasBody = true)
    fun deleteProperties(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String,
            @Path("extAppUuid") extAppUuid: String,
            @Body body: AgoraExtAppDeleteRequest
    ): Call<ResponseBody<String>>

}

data class AgoraExtAppUpdateRequest(
        val properties: MutableMap<String, Any?>?,
        val cause: MutableMap<String, Any?>?,
        val common: MutableMap<String, Any?>?
)

data class AgoraExtAppDeleteRequest(
        val properties: MutableList<String>?,
        val cause: MutableMap<String, Any?>?
)
