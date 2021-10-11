package io.agora.edu.core.internal.privatechat

import retrofit2.Call
import retrofit2.http.*

interface PrivateChatService {
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{toUserUuid}/privateSpeech")
    fun startPrivateChat(@Path("appId") appId: String,
                         @Path("roomUuid") roomUuid: String,
                         @Path("toUserUuid") toUserUuid: String): Call<ResponseBody>

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{toUserUuid}/privateSpeech")
    fun finishPrivateChat(@Path("appId") appId: String,
                          @Path("roomUuid") roomUuid: String,
                          @Path("toUserUuid") toUserUuid: String): Call<ResponseBody>
}
