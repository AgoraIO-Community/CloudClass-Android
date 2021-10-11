package io.agora.edu.core.internal.server.requests.http.retrofit.services

import io.agora.edu.core.internal.server.struct.request.EduJoinClassroomReq
import io.agora.edu.core.internal.server.struct.request.EduUserRoomChatMuteReq
import io.agora.edu.core.internal.server.struct.response.DataResponseBody
import io.agora.edu.core.internal.server.struct.response.EduEntryRes
import retrofit2.Call
import retrofit2.http.*

internal interface UserService {
    @POST("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    fun applyHandsUp(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String
    ): Call<DataResponseBody<Int>>

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    fun cancelApplyHandsUp(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String
    ): Call<DataResponseBody<Int>>

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/acceptance")
    fun exitHandsUp(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUuid: String
    ): Call<DataResponseBody<Int>>





    ///////////////////////////////////////////////////////////////////////////
    // The following services are deprecated, and we leave them here for 
    // old version code compatible
    ///////////////////////////////////////////////////////////////////////////
    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}")
    fun updateUserMuteState(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduUserStatusReq: EduUserRoomChatMuteReq
    ): Call<DataResponseBody<String>>

    /**加入房间*/
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/entry")
    fun joinClassroom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<DataResponseBody<EduEntryRes>>
}