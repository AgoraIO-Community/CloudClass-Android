package io.agora.edu.common.service;

import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.AllocateGroupReq;
import io.agora.edu.common.bean.request.RoomCreateOptionsReq;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.EduRoomInfoRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * 课堂预处理服务
 */
public interface RoomPreService {
    /**
     * 分配小组:请求服务端分配一个小教室
     */
    @POST("/grouping/apps/{appId}/v1/rooms/{roomUuid}/groups")
    Call<ResponseBody<EduRoomInfoRes>> allocateGroup(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Body AllocateGroupReq allocateGroupReq);

    /**
     * 创建房间
     *
     * @return 房间id(roomId)
     */
    @POST("/scene/apps/{appId}/v1/rooms/{roomUuid}/config")
    Call<ResponseBody<String>> createClassroom(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Body RoomCreateOptionsReq roomCreateOptionsReq
    );

    /**
     * 创建/返回房间预检状态
     */
    @PUT("/edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}")
    Call<ResponseBody<RoomPreCheckRes>> preCheckClassroom(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String userUuid,
            @Body RoomPreCheckReq req
    );

    /**
     * 拉取远端配置项
     */
    @GET("/edu/apps/{appId}/v2/configs")
    Call<ResponseBody<EduRemoteConfigRes>> pullRemoteConfig(
            @Path("appId") String appId
    );
}
