package io.agora.whiteboard.netless.listener;

import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.SDKError;
import com.herewhite.sdk.domain.SceneState;

public interface BoardEventListener {
    void onJoinSuccess(GlobalState state);

    void onJoinFail(SDKError error);

    void onRoomPhaseChanged(RoomPhase phase);

    void onGlobalStateChanged(GlobalState state);

    void onSceneStateChanged(SceneState state);

    void onMemberStateChanged(MemberState state);

    void onDisconnectWithError(Exception e);
}
