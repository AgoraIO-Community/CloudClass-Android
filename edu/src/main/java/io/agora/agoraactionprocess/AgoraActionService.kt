package io.agora.agoraactionprocess

import io.agora.base.network.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface AgoraActionService {
    @POST("/invitation/apps/{appId}/v1/rooms/{roomUuid}/process/{processUuid}")
    fun setupAgoraAction(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("processUuid") processUuid: String,
            @Body actionOptions: AgoraActionOptions
    ): Call<ResponseBody<String>>

    @DELETE("/invitation/apps/{appId}/v1/rooms/{roomUuid}/process/{processUuid}")
    fun deleteAgoraAction(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("processUuid") processUuid: String
    ): Call<ResponseBody<String>>

    @POST("/invitation/apps/{appId}/v2/rooms/{roomUuid}/users/{toUserUuid}/process/{processUuid}")
    fun startAgoraAction(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Path("processUuid") processUuid: String,
            @Body body: AgoraStartActionMsgReq
    ): Call<ResponseBody<String>>


    @HTTP(method = "DELETE", path = "/invitation/apps/{appId}/v2/rooms/{roomUuid}/users/{toUserUuid}/process/{processUuid}", hasBody = true)
    fun stopAgoraAction(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Path("processUuid") processUuid: String,
            @Body body: AgoraStopActionMsgReq
    ): Call<ResponseBody<String>>
}