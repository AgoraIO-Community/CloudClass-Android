package io.agora.edu.common.api;

import io.agora.edu.common.bean.request.DeviceStateUpdateReq;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import io.agora.education.api.EduCallback;

public interface RoomPre {
    void preCheckClassRoom(String userUuid, RoomPreCheckReq req, EduCallback<RoomPreCheckRes> callback);

    void pullRemoteConfig(EduCallback<EduRemoteConfigRes> callback);

    void updateDeviceState(String userUuid, DeviceStateUpdateReq req);
}
