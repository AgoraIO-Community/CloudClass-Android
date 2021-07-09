package io.agora.edu.common.service

import io.agora.base.network.ResponseBody
import io.agora.edu.common.bean.flexpropes.RoomFlexPropsReq
import io.agora.edu.common.bean.flexpropes.UserFlexPropsReq
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface FlexPropsService {
    @PUT("/edu/apps/{appId}/v2/rooms/{roomUUid}/properties")
    fun updateFlexRoomProps(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String,
            @Body req: RoomFlexPropsReq?): Call<ResponseBody<String?>>

    @PUT("/edu/apps/{appId}/v2/rooms/{roomUUid}/users/{userUuid}/properties")
    fun updateFlexUserProps(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body req: UserFlexPropsReq?): Call<ResponseBody<String?>>
}