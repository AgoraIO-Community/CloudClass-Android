package io.agora.online.impl.whiteboard.netless.listener;

import com.herewhite.sdk.Room;
import com.herewhite.sdk.domain.CameraState;
import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.PageState;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.RoomState;
import com.herewhite.sdk.domain.SDKError;
import com.herewhite.sdk.domain.SceneState;

import org.jetbrains.annotations.NotNull;

public interface BoardEventListener {
    void onJoinSuccess(GlobalState state);

    void onJoinFail(SDKError error);

    void onRoomPhaseChanged(@NotNull RoomPhase phase);

    void onGlobalStateChanged(@NotNull GlobalState state);

    void onSceneStateChanged(@NotNull SceneState state);

    void onPageStateChanged(@NotNull PageState state);

    void onMemberStateChanged(@NotNull MemberState state);

    void onCameraStateChanged(@NotNull CameraState state);

    void onDisconnectWithError(Exception e);

    void onRoomStateChanged(RoomState modifyState);


    /**
     * 可撤销次数发生变化回调。
     * <p>
     * 当本地用户调用 {@link Room#undo undo} 撤销上一步操作时，会触发该回调，报告剩余的可撤销次数。
     *
     * @param canUndoSteps 剩余的可撤销次数。
     */
    void onCanUndoStepsUpdate(long canUndoSteps);

    /**
     * 可重做次数发生变化回调。
     * <p>
     * 当本地用户调用 {@link Room#redo redo} 重做上一步操作时，会触发该回调，报告剩余的可重做次数。
     *
     * @param canRedoSteps 剩余的可重做次数。
     */
    void onCanRedoStepsUpdate(long canRedoSteps);
}
