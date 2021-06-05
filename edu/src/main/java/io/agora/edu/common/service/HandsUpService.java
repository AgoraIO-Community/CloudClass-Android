package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.handsup.HandsUpReq;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface HandsUpService {
    @POST("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    Call<ResponseBody<Integer>> applyHandsUp(
            @Path("appId") String appId,
            @Path("roomUUid") String roomUuid
    );

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress")
    Call<ResponseBody<Integer>> cancelApplyHandsUp(
            @Path("appId") String appId,
            @Path("roomUUid") String roomUuid
    );

    @DELETE("edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/acceptance")
    Call<ResponseBody<Integer>> exitHandsUp(
            @Path("appId") String appId,
            @Path("roomUUid") String roomUuid
    );
}
