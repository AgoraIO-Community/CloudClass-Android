package io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated

import io.agora.edu.core.internal.edu.common.bean.ResponseBody
import io.agora.edu.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.edu.core.internal.server.struct.request.RoomPreCheckReq
import io.agora.edu.core.internal.server.struct.response.EduRemoteConfigRes
import io.agora.edu.core.internal.server.struct.response.RoomPreCheckRes
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface RoomPreService {
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}")
    fun preCheckClassroom(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("userUuid") userUuid: String?,
            @Body req: RoomPreCheckReq?
    ): Call<ResponseBody<RoomPreCheckRes>>

    @GET("edu/apps/{appId}/v2/client/configs")
    fun pullRemoteConfig(
            @Path("appId") appId: String?
    ): Call<ResponseBody<EduRemoteConfigRes>>

    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}/device")
    fun updateDeviceState(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("userUuid") userUuid: String?,
            @Body req: DeviceStateUpdateReq?
    ): Call<ResponseBody<String>>
}