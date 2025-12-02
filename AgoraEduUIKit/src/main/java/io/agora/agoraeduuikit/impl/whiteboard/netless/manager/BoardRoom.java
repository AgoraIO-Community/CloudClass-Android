package io.agora.agoraeduuikit.impl.whiteboard.netless.manager;

import com.herewhite.sdk.Room;
import com.herewhite.sdk.RoomParams;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.domain.AnimationMode;
import com.herewhite.sdk.domain.CameraConfig;
import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.ImageInformation;
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
    Room getRoom();

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

    @Nullable
    MemberState getMemberState();

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

    /**
     * 1，scenes不能为null，可以是Scene
     * 2，第一个参数为目录地址，形如"/dirxxx"
     * 3, 第三个参数应该是当前index的下一个
     * 4，添加后一般会需要切换页面操作
     *
     * @param dir
     * @param scenes
     * @param index
     */
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

    void getWindowManagerAttributes(Promise<String> promise);

    void setWindowManagerAttributes(String attr);

    void cleanScene(boolean retainPpt);

    void changeMixingState(long state, long errorCode);

    void disconnect(Promise<Object> promise);

    void setSceneIndex(Integer index, @androidx.annotation.Nullable final Promise<Boolean> promise);

    void redo();

    void undo();

    void addPage();

    void removePage();

    void prevPage();

    void nextPage();

    void disableSerialization(boolean disable);

    void setContainerSizeRatio(float ratio);

    boolean getWritable();

    void insertImage(String imageUrl, ImageInformation imageInformation, final Promise<String> callback);
}
