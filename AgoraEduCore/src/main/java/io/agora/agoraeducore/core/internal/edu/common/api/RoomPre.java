package io.agora.agoraeducore.core.internal.edu.common.api;

import io.agora.agoraeducore.core.internal.server.struct.request.DeviceStateUpdateReq;
import io.agora.agoraeducore.core.internal.server.struct.request.RoomPreCheckReq;
import io.agora.agoraeducore.core.internal.framework.data.EduCallback;
import io.agora.agoraeducore.core.internal.server.struct.response.EduRemoteConfigRes;
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes;

public interface RoomPre {
    void preCheckClassRoom(String userUuid, RoomPreCheckReq req, EduCallback<RoomPreCheckRes> callback);

    void pullRemoteConfig(EduCallback<EduRemoteConfigRes> callback);

    void updateDeviceState(String userUuid, DeviceStateUpdateReq req);
}
