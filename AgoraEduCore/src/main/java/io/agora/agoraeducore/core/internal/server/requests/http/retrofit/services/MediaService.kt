package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services

import io.agora.agoraeducore.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduDelStreamsBody
import io.agora.agoraeducore.core.internal.education.impl.stream.data.request.EduUpsertStreamsBody
import io.agora.agoraeducore.core.internal.education.impl.user.data.request.EduStreamStatusReq
import io.agora.agoraeducore.core.internal.server.struct.response.DataResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Collects services of media devices, media business streams and so on
 */
interface MediaService {
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}/device")
    fun updateDeviceState(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body req: DeviceStateUpdateReq
    ): Call<DataResponseBody<String>>

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun createStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<DataResponseBody<String>>

    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun updateStreamInfo(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<DataResponseBody<String>>

    @DELETE("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun deleteStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String
    ): Call<DataResponseBody<String>>

    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun upsertStream(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<DataResponseBody<String>>

    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/streams")
    fun upsertStreams(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body streams: EduUpsertStreamsBody
    ): Call<DataResponseBody<String>>

    @HTTP(method = "DELETE", path = "scene/apps/{appId}/v1/rooms/{roomUuid}/streams", hasBody = true)
    fun delStreams(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body streams: EduDelStreamsBody
    ): Call<DataResponseBody<String>>
}