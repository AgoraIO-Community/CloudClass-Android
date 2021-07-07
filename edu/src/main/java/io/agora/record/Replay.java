package io.agora.record;

import java.util.List;

import io.agora.education.api.EduCallback;

public interface Replay {
    void replayList(String appId, String roomId, int nextId,
                    EduCallback<ReplayRes> callback);


    void allReplayList(String appId, String roomId, int nextId,
                       EduCallback<List<ReplayRes.RecordDetail>> callback);
}
