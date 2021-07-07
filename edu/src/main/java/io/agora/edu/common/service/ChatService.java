package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.ChatTranslateReq;
import io.agora.edu.common.bean.response.ChatRecordRes;
import io.agora.edu.common.bean.response.ChatTranslateRes;
import io.agora.edu.common.bean.response.SendChatRes;
import io.agora.education.impl.user.data.request.EduRoomChatMsgReq;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatService {
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/from/{userUuid}/chat")
    Call<SendChatRes> roomChat(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String fromUuid,
            @Body EduRoomChatMsgReq req);

    @POST("edu/acadsoc/apps/{appId}/v1/translation")
    Call<ResponseBody<ChatTranslateRes>> translate(
            @Path("appId") String appId,
            @Body ChatTranslateReq req);

    @GET("edu/apps/{appId}/v2/rooms/{roomUUid}/chat/messages")
    Call<ResponseBody<ChatRecordRes>> pullChatRecords(
            @Path("appId") String appId,
            @Path("roomUUid") String roomUUid,
            @Query("count") int count,
            @Query("nextId") String nextId,
            @Query("sort") int sort);
}
