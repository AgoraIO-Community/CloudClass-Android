package io.agora.agoraeduuikit.impl.whiteboard.netless.manager;

import com.herewhite.sdk.RoomParams;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.domain.AnimationMode;
import com.herewhite.sdk.domain.CameraConfig;
import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.RoomState;
import com.herewhite.sdk.domain.Scene;
import com.herewhite.sdk.domain.SceneState;
import com.herewhite.sdk.domain.WindowAppParam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.agora.agoraeduuikit.impl.whiteboard.netless.annotation.Appliance;
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.BoardEventListener;

public interface BoardRoom {
    void setListener(BoardEventListener listener);

    void init(WhiteSdk sdk, RoomParams params);

    void setAppliance(@Appliance String appliance);

    String getAppliance();

    void setStrokeColor(int[] color);

    int[] getStrokeColor();

    void setStrokeWidth(double width);

    Double getStrokeWidth();

    void setTextSize(double size);

    Double getTextSize();

    void setMemState(@NotNull MemberState state);

    @Nullable MemberState getMemberState();

    void setSceneIndex(int index);

    RoomState getBoardState();

    int getSceneCount();

    void zoom(double scale);

    void moveCamera(CameraConfig cameraConfig);

    void scalePptToFit();

    void follow(boolean follow);

    void scalePptToFit(AnimationMode mode);

    void hasBroadcaster(Promise<Boolean> promise);

    boolean hasBroadcaster();

    double getZoomScale();

    void pptPreviousStep();

    void pptNextStep();

    void getRoomPhase(Promise<RoomPhase> promise);

    void refreshViewSize();

    void removeScenes(String dirOrPath);

    void putScenes(String dir, Scene[] scenes, int index);

    void setScenePath(String path, final Promise<Boolean> promise);

    void setScenePath(String path);

    void getSceneState(Promise<SceneState> promise);

    void disableDeviceInputs(boolean disabled);

    void disableDeviceInputsTemporary(boolean disabled);

    boolean isDisableDeviceInputs();

    void disableCameraTransform(boolean disabled);

    boolean isDisableCameraTransform();

    void setWritable(boolean writable);

    void setWritable(boolean writable, Promise<Boolean> promise);

    void disconnect();

    void setGlobalState(GlobalState state);

    void setWindowApp(@NotNull WindowAppParam param, @Nullable Promise<String> promise);

    void changeMixingState(int state, int errorCode);

    void disconnect(Promise<Object> promise);
}
