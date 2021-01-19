package io.agora.education.impl.user.network

import io.agora.education.impl.user.data.request.*
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.response.EduEntryRes
import retrofit2.Call
import retrofit2.http.*

internal interface UserService {

    /**加入房间*/
    @POST("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/entry")
    fun joinClassroom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<ResponseBody<EduEntryRes>>

    /**更新某一个用户的禁止聊天状态
     * @param mute 可否聊天 1可 0否*/
    @PUT("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}")
    fun updateUserMuteState(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduUserStatusReq: EduUserStatusReq
    ): Call<io.agora.base.network.ResponseBody<String>>


    /**调用此接口需要添加header->userToken
     * 此处的返回值没有写错，确实只返回code 和 msg*/
    @POST("/scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/exit")
    fun leaveClassroom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**开启 邀请/申请流程*/
    @POST("/invitation/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/process/{processUuid}")
    fun doAction(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Path("processUuid") processUuid: String,
            @Body startActionReq: EduActionReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**开启 邀请/申请流程*/
    @POST("/scene/apps/{appId}/v1/process/{processUuid}/stop")
    fun stopAction(
            @Path("appId") appId: String,
            @Path("processUuid") processUuid: String,
            @Body stopActionReq: EduStopActionReq
    ): Call<io.agora.base.network.ResponseBody<String>>


}