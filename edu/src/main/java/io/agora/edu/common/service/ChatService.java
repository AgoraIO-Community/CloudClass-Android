package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.education.impl.user.data.request.EduRoomChatMsgReq;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChatService {
    @POST("/edu/apps/{appId}/v2/rooms/{roomUuid}/from/{userUuid}/chat")
    Call<ResponseBody<String>> roomChat(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String fromUuid,
            @Body EduRoomChatMsgReq req);
}
