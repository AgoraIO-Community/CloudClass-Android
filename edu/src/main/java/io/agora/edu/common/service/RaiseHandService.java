package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.RaiseHandReq;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RaiseHandService {
    @POST("/edu/apps/{appId}/v2/rooms/{roomUuid}/handup/{toUserUuid}")
    Call<ResponseBody<Integer>> raiseHand(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("toUserUuid") String toUserUuid,
            @Body RaiseHandReq req
    );
}
