package io.agora.edu.core.internal.server.requests.http.retrofit.services

import io.agora.edu.core.internal.server.struct.request.*
import io.agora.edu.core.internal.server.struct.response.*
import retrofit2.Call
import retrofit2.http.*

interface MessageService {
    /**
     * Send a TEXT message to the current room, and all other room users
     * can receive and see this message.
     * Note the message data structure is defined internally in aPaaS
     * sdk and users cannot define their own message data structure
     */
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/from/{userUuid}/chat")
    fun sendRoomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") fromUuid: String,
            @Body req: EduRoomChatMsgReq): Call<SendChatRes>

    /**
     * Retrieve a number of past room TEXT messages.
     * @param appId
     * @param roomUUid
     * @param count how many messages is to be fetched
     * @param nextId the base message id that is search for, exclusively.
     * if null, search all messages from the first message
     * @param sort 0, retrieve messages from nextId for more recent messages;
     * 1, retrieve messages reversely from nextId for earlier messages
     */
    @GET("edu/apps/{appId}/v2/rooms/{roomUUid}/chat/messages")
    fun retrieveRoomMessages(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUUid: String,
            @Query("count") count: Int,
            @Query("nextId") nextId: String?,
            @Query("sort") sort: Int): Call<DataResponseBody<ChatRecordRes>>

    /**
     * Send custom message to the room. The message is not only text chat but
     * also other kinds of structures that is defined by user
     */
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/message/channel")
    fun sendRoomCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMsgReq: EduRoomMsgReq
    ): Call<DataResponseBody<String>>

    /**
     * Send a user to user TEXT chat message
     */
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/chat/peer")
    fun sendPeerMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserChatMsgReq: EduUserChatMsgReq
    ): Call<DataResponseBody<String>>

    /**
     * Send a user to user custom TEXT message, whose structure is
     * defined by user.
     */
    @POST("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/messages/peer")
    fun sendPeerCustomMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserMsgReq: EduUserMsgReq
    ): Call<DataResponseBody<String>>

    /**
     * Conversation is TEXT messages that only can be read by
     * only part of the room users
     */
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/conversation/students/{studentUuid}/messages")
    fun sendConversationMessage(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("studentUuid") userUuid: String,
            @Body req: EduRoomChatMsgReq): Call<ConversationRes>

    @GET("edu/apps/{appId}/v2/rooms/{roomUUid}/conversation/students/{studentsUuid}/messages")
    fun retrieveConversationMessages(
            @Path("appId") appId: String,
            @Path("roomUUid") roomUUid: String,
            @Path("studentsUuid") userUuid: String,
            @Query("nextId") nextId: String?,
            @Query("sort") sort: Int): Call<DataResponseBody<ConversationRecordRes>>

    /**
     * Translate a message to the target language.
     */
    @POST("edu/acadsoc/apps/{appId}/v1/translation")
    fun translate(
            @Path("appId") appId: String,
            @Body req: ChatTranslateReq): Call<DataResponseBody<ChatTranslateRes>>

    /**
     * Only can be call by a room manager or room host.
     * Allow or forbid a certain user in the room to send
     * room chat
     */
    @PUT("scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}")
    fun setUserChatMuteState(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body eduUserStatusReq: EduUserRoomChatMuteReq
    ): Call<DataResponseBody<String>>
}