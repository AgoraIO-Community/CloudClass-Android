package io.agora.agoraeduuikit.impl.whiteboard.netless.listener;

import com.herewhite.sdk.domain.CameraState;
import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.SDKError;
import com.herewhite.sdk.domain.SceneState;

import org.jetbrains.annotations.NotNull;

public interface BoardEventListener {
    void onJoinSuccess(@NotNull GlobalState state);

    void onJoinFail(SDKError error);

    void onRoomPhaseChanged(@NotNull RoomPhase phase);

    void onGlobalStateChanged(@NotNull GlobalState state);

    void onSceneStateChanged(@NotNull SceneState state);

    void onMemberStateChanged(@NotNull MemberState state);

    void onCameraStateChanged(@NotNull CameraState state);

    void onDisconnectWithError(Exception e);
}
