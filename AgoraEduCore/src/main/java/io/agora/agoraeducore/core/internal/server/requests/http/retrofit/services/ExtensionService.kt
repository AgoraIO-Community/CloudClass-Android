package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services

import io.agora.agoraeducore.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.agoraeducore.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.agoraeducore.core.internal.server.struct.response.BaseResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface ExtensionService {
    @PUT("edu/apps/{appId}/v2/rooms/{roomUUid}/properties")
    fun updateFlexRoomProps(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String,
            @Body req: RoomFlexPropsReq): Call<BaseResponseBody>

    @PUT("edu/apps/{appId}/v2/rooms/{roomUUid}/users/{userUuid}/properties")
    fun updateFlexUserProps(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body req: UserFlexPropsReq): Call<BaseResponseBody>
}