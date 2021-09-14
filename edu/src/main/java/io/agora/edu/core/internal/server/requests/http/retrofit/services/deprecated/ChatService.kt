package io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated

import io.agora.edu.core.internal.edu.common.bean.ResponseBody
import io.agora.edu.core.internal.server.struct.request.*
import io.agora.edu.core.internal.server.struct.response.*
import retrofit2.Call
import retrofit2.http.*

interface ChatService {
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/from/{userUuid}/chat")
    fun roomChat(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("userUuid") fromUuid: String?,
            @Body req: EduRoomChatMsgReq?): Call<SendChatRes>?

    @POST("edu/acadsoc/apps/{appId}/v1/translation")
    fun translate(
            @Path("appId") appId: String?,
            @Body req: ChatTranslateReq?): Call<DataResponseBody<ChatTranslateRes?>?>?

    @GET("edu/apps/{appId}/v2/rooms/{roomUUid}/chat/messages")
    fun pullChatRecords(
            @Path("appId") appId: String?,
            @Path("roomUUid") roomUUid: String?,
            @Query("count") count: Int,
            @Query("nextId") nextId: String?,
            @Query("sort") sort: Int): Call<DataResponseBody<ChatRecordRes>>?

    /**
     * Conversation is defined as only used in student Q&A session
     */
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/conversation/students/{studentUuid}/messages")
    fun conversation(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("studentUuid") userUuid: String?,
            @Body req: EduRoomChatMsgReq?): Call<ConversationRes?>?

    @GET("edu/apps/{appId}/v2/rooms/{roomUUid}/conversation/students/{studentsUuid}/messages")
    fun pullConversationRecords(
            @Path("appId") appId: String?,
            @Path("roomUUid") roomUUid: String?,
            @Path("studentsUuid") userUuid: String?,
            @Query("nextId") nextId: String?,
            @Query("sort") sort: Int): Call<DataResponseBody<ConversationRecordRes>>?







    /**发送自定义的频道消息*/
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/message/channel")
    fun sendChannelCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMsgReq: EduRoomMsgReq
    ): Call<ResponseBody<String>>

    /**发送自定义的点对点消息*/
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/messages/peer")
    fun sendPeerCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserMsgReq: EduUserMsgReq
    ): Call<ResponseBody<String>>



    /**发送用户间的私聊消息*/
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/chat/peer")
    fun sendPeerChatMsg(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserChatMsgReq: EduUserChatMsgReq
    ): Call<ResponseBody<String>>
}