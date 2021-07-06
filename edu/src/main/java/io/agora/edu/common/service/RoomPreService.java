package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.DeviceStateUpdateReq;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RoomPreService {

    /**
     * Create/return to room pre-check status
     */
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}")
    Call<ResponseBody<RoomPreCheckRes>> preCheckClassroom(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String userUuid,
            @Body RoomPreCheckReq req
    );

    /**
     * Pull remote configuration items
     */
    @GET("edu/apps/{appId}/v2/configs")
    Call<ResponseBody<EduRemoteConfigRes>> pullRemoteConfig(
            @Path("appId") String appId
    );

    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}/device")
    Call<io.agora.base.network.ResponseBody<String>> updateDeviceState(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String userUuid,
            @Body DeviceStateUpdateReq req
    );
}
