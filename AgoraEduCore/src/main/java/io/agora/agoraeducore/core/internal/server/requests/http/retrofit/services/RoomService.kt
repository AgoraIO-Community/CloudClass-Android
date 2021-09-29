package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services

import io.agora.agoraeducore.core.internal.server.struct.request.*
import io.agora.agoraeducore.core.internal.server.struct.response.*
import retrofit2.Call
import retrofit2.http.*

internal interface RoomService {
    @GET("edu/apps/{appId}/v2/configs")
    fun pullRemoteConfig(
            @Path("appId") appId: String?
    ): Call<DataResponseBody<EduRemoteConfigRes>>

    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}")
    fun preCheckClassroom(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("userUuid") userUuid: String?,
            @Body req: RoomPreCheckReq
    ): Call<DataResponseBody<RoomPreCheckRes>>

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/entry")
    fun joinRoom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<DataResponseBody<EduEntryRes>>

    @GET("scene/apps/{appId}/v1/rooms/{roomUuid}/snapshot")
    fun fetchSnapshot(
            @Header("token") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String
    ): Call<DataResponseBody<EduSequenceSnapshotRes>>

    @GET("scene/apps/{appId}/v1/rooms/{roomUuid}/sequences")
    fun fetchLostSequences(
            @Header("token") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Query("nextId") nextId: Int,
            @Query("count") count: Int?
    ): Call<DataResponseBody<EduSequenceListRes<Any>>>

    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/properties")
    fun setRoomProperties(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body req: EduUpsertRoomPropertyReq
    ): Call<DataResponseBody<String>>

    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/properties")
    fun removeRoomProperties(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body req: EduRemoveRoomPropertyReq
    ): Call<DataResponseBody<String>>

    /**
     * Set a mute state for all the users of some role type
     */
    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/roles/mute")
    fun updateRoomMuteStateForRole(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMuteStateReq: EduRoomMuteStateReq
    ): Call<DataResponseBody<String>>

    /**
     * Set the class state, particularly start and finish
     * states of the class.
     */
    @PUT("scene/apps/{appId}/v1/rooms/{roomUUid}/states/{state}")
    fun updateClassState(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUUid: String,
            @Path("state") state: Int
    ): Call<DataResponseBody<String>>

    ////////////////////////////////////////////////////////////////////
    // Has been removed to media service
    ////////////////////////////////////////////////////////////////////
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}/device")
    fun updateDeviceState(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body req: DeviceStateUpdateReq
    ): Call<DataResponseBody<String>>?

    ////////////////////////////////////////////////////////////////////
    // The following have been moved to message service, and leave
    // them here to keep old code compatible
    ////////////////////////////////////////////////////////////////////

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/message/channel")
    fun sendChannelCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMsgReq: EduRoomMsgReq
    ): Call<DataResponseBody<String>>

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/messages/peer")
    fun sendPeerCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserMsgReq: EduUserMsgReq
    ): Call<DataResponseBody<String>>

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/chat/channel")
    fun sendRoomChatMsg(
            @Header("token") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomChatMsgReq: EduRoomChatMsgReq
    ): Call<DataResponseBody<String>>

    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/chat/peer")
    fun sendPeerChatMsg(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserChatMsgReq: EduUserChatMsgReq
    ): Call<DataResponseBody<String>>
}