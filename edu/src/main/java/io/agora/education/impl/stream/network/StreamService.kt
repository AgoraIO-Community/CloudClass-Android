package io.agora.education.impl.stream.network

import io.agora.base.network.ResponseBody
import io.agora.education.impl.stream.data.request.EduDelStreamsBody
import io.agora.education.impl.stream.data.request.EduDelStreamsReq
import io.agora.education.impl.stream.data.request.EduUpsertStreamsBody
import io.agora.education.impl.stream.data.request.EduUpsertStreamsReq
import io.agora.education.impl.user.data.request.EduStreamStatusReq
import retrofit2.Call
import retrofit2.http.*

interface StreamService {

    /**创建流*/
    @POST("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun createStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<ResponseBody<String>>

    /**更新流状态*/
    @PUT("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun updateStreamInfo(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<ResponseBody<String>>

    /**删除流*/
    @DELETE("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun deleteStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String
    ): Call<ResponseBody<String>>

    /**新增/更新流*/
    @PUT("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun upsertStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<ResponseBody<String>>

    /**批量upsert流*/
    @PUT("/scene/apps/{appId}/v1/rooms/{roomUuid}/streams")
    fun upsertStreams(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body streams: EduUpsertStreamsBody
    ): Call<ResponseBody<String>>

    /**批量删除流*/
    @HTTP(method = "DELETE", path = "/scene/apps/{appId}/v1/rooms/{roomUuid}/streams", hasBody = true)
    fun delStreams(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body streams: EduDelStreamsBody
    ): Call<ResponseBody<String>>
}