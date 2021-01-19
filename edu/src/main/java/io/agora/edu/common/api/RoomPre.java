package io.agora.edu.common.api;

import io.agora.edu.common.bean.request.AllocateGroupReq;
import io.agora.edu.common.bean.request.RoomCreateOptionsReq;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import io.agora.education.api.EduCallback;
import io.agora.education.api.room.data.EduRoomInfo;

public interface RoomPre {
    void allocateGroup(AllocateGroupReq allocateGroupReq, EduCallback<EduRoomInfo> callback);

    void createClassRoom(RoomCreateOptionsReq roomCreateOptionsReq, EduCallback<String> callback);

    void preCheckClassRoom(String userUuid, RoomPreCheckReq req, EduCallback<RoomPreCheckRes> callback);

    void pullRemoteConfig(EduCallback<EduRemoteConfigRes> callback);
}
