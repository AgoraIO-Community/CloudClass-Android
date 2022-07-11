package io.agora.agoraeduuikit.whiteboard;

import static android.text.TextUtils.isEmpty;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.herewhite.sdk.Room;
import com.herewhite.sdk.RoomListener;
import com.herewhite.sdk.RoomParams;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.domain.AnimationMode;
import com.herewhite.sdk.domain.BroadcastState;
import com.herewhite.sdk.domain.CameraConfig;
import com.herewhite.sdk.domain.CameraState;
import com.herewhite.sdk.domain.GlobalState;
import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.RoomState;
import com.herewhite.sdk.domain.SDKError;
import com.herewhite.sdk.domain.Scene;
import com.herewhite.sdk.domain.SceneState;
import com.herewhite.sdk.domain.ViewMode;
import com.herewhite.sdk.domain.WindowAppParam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeduuikit.impl.whiteboard.netless.annotation.Appliance;
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.BoardEventListener;
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom;
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.NetlessManager;

/**
 * author : hefeng
 * date : 2022/6/9
 * description :
 */
public class FcrBoardMainWindow extends NetlessManager<Room> implements BoardRoom, RoomListener {
    public static final String TAG = "FcrBoardMainWindow";

    private String appliance;
    private int[] strokeColor;
    private double strokeWidth = -100f, textSize = -100f;
    private MemberState memberState = new MemberState();
    private Boolean disableDeviceInputs;
    private Boolean disableCameraTransform;
    private Boolean writable;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BoardEventListener listener;
    private boolean joinSuccess = false;

    private int joinFailedRetry = 2;
    private int connectFailedRetry = 2;
    private WhiteSdk whiteSdk;
    private RoomParams roomParams;

    @Override
    public Room getRoom() {
        return t;
    }

    public void setListener(BoardEventListener listener) {
        this.listener = listener;
    }

    public void init(WhiteSdk sdk, RoomParams params) {
        this.whiteSdk = sdk;
        this.roomParams = params;
        sdk.joinRoom(params, FcrBoardMainWindow.this, promise);
    }

    public void setAppliance(@Appliance String appliance) {
        if (t != null) {
            MemberState state = new MemberState();
            state.setCurrentApplianceName(appliance);
            t.setMemberState(state);
        }
        this.appliance = appliance;
    }

    public String getAppliance() {
        if (t != null) {
            return t.getMemberState().getCurrentApplianceName();
        }
        return null;
    }

    public void setStrokeColor(int[] color) {
        if (t != null) {
            MemberState state = new MemberState();
            state.setStrokeColor(color);
            t.setMemberState(state);
        }
        this.strokeColor = color;
    }

    public int[] getStrokeColor() {
        if (t != null) {
            return t.getMemberState().getStrokeColor();
        }
        return null;
    }

    public void setStrokeWidth(double width) {
        if (t != null) {
            MemberState state = new MemberState();
            state.setStrokeWidth(width);
            t.setMemberState(state);
        }
        this.strokeWidth = width;
    }

    public Double getStrokeWidth() {
        if (t != null) {
            return t.getMemberState().getStrokeWidth();
        }
        return null;
    }

    public void setTextSize(double size) {
        if (t != null) {
            MemberState state = new MemberState();
            state.setTextSize(size);
            t.setMemberState(state);
        }
        this.textSize = size;
    }

    public Double getTextSize() {
        if (t != null) {
            return t.getMemberState().getTextSize();
        }
        return null;
    }

    @Override
    public void setMemState(@NotNull MemberState state) {
        if (!isEmpty(state.getCurrentApplianceName())) {
            this.appliance = state.getCurrentApplianceName();
            this.memberState.setShapeType(state.getShapeType());
            this.memberState.setCurrentApplianceName(state.getCurrentApplianceName(), state.getShapeType());
        }
        if (state.getStrokeColor() != null) {
            this.strokeColor = state.getStrokeColor();
            this.memberState.setStrokeColor(state.getStrokeColor());
        }
        if (state.getStrokeWidth() != 0.0) {
            this.strokeWidth = state.getStrokeWidth();
            this.memberState.setStrokeWidth(state.getStrokeWidth());
        }
        if (state.getTextSize() != 0.0) {
            this.textSize = state.getTextSize();
            this.memberState.setTextSize(state.getTextSize());
        }
        if (t != null) {
            t.setMemberState(memberState);
        }
    }

    @Override
    public @Nullable
    MemberState getMemberState() {
        if (t != null) {
            return t.getMemberState();
        }
        return null;
    }

    public void setSceneIndex(int index) {
        if (t != null && !isDisableDeviceInputs()) {
            t.setSceneIndex(index, new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                }

                @Override
                public void catchEx(SDKError t) {
                }
            });
        }
    }

    public RoomState getBoardState() {
        if (t != null) {
            return t.getRoomState();
        }
        return null;
    }

    public int getSceneCount() {
        if (t != null) {
            return t.getScenes().length;
        }
        return 0;
    }

    public void zoom(double scale) {
        if (t != null && !isDisableCameraTransform()) {
            CameraConfig cameraConfig = new CameraConfig();
            cameraConfig.setScale(scale);
            t.moveCamera(cameraConfig);
        }
    }

    public void moveCamera(CameraConfig cameraConfig) {
        if (t != null) {
            t.moveCamera(cameraConfig);
        }
    }

    public void scalePptToFit() {
        if (t != null) {
            t.scalePptToFit();
            t.scaleIframeToFit();
        }
    }

    public void follow(boolean follow) {
        Constants.INSTANCE.getAgoraLog().e(TAG + "->follow: " + follow);
        if (t != null) {
            Constants.INSTANCE.getAgoraLog().e(TAG + "->setViewMode: ViewMode.Broadcaster");
            //Constants.INSTANCE.getAgoraLog().e(TAG + "->setViewMode: " + (follow ? ViewMode.Follower : ViewMode.Broadcaster));
            // 多窗口，所有用户都可以作为 boardcaster 没有 follower 概念
            //t.setViewMode(follow ? ViewMode.Follower : ViewMode.Broadcaster);
            t.setViewMode(ViewMode.Broadcaster);
        }
    }

    public void scalePptToFit(AnimationMode mode) {
        if (t != null) {
            t.scalePptToFit(mode);
            t.scaleIframeToFit();
        }
    }

    public void cleanScene(boolean retainPpt) {
        if (t != null) {
            t.cleanScene(retainPpt);
        }
    }

    public void hasBroadcaster(Promise<Boolean> promise) {
        if (promise == null) {
            return;
        }
        if (t != null) {
            t.getRoomState(new Promise<RoomState>() {
                @Override
                public void then(RoomState roomState) {
                    BroadcastState state = roomState.getBroadcastState();
                    promise.then(state != null && state.getBroadcasterId() != null);
                }

                @Override
                public void catchEx(SDKError t) {
                    promise.catchEx(t);
                }
            });
        } else {
            promise.then(false);
        }
    }

    public boolean hasBroadcaster() {
        if (t != null && t.getRoomState() != null) {
            BroadcastState state = t.getRoomState().getBroadcastState();
            return state != null && state.getBroadcasterId() != null;
        }
        return false;
    }

    public double getZoomScale() {
        if (t != null) {
            return t.getZoomScale();
        }
        return 1.0;
    }

    public void pptPreviousStep() {
        if (t != null && !isDisableDeviceInputs()) {
            t.pptPreviousStep();
        }
    }

    public void pptNextStep() {
        if (t != null && !isDisableDeviceInputs()) {
            t.pptNextStep();
        }
    }

    public void getRoomPhase(Promise<RoomPhase> promise) {
        if (t != null) {
            t.getRoomPhase(promise);
        } else {
            if (promise != null) {
                promise.then(RoomPhase.disconnected);
            }
        }
    }

    public void refreshViewSize() {
        if (t != null) {
            t.refreshViewSize();
        }
    }

    public void removeScenes(String dirOrPath) {
        if (t != null) {
            t.removeScenes(dirOrPath);
        }
    }

    public void putScenes(String dir, Scene[] scenes, int index) {
        if (t != null) {
            t.putScenes(dir, scenes, index);
        }
    }

    public void setScenePath(String path, final Promise<Boolean> promise) {
        if (t != null) {
            t.setScenePath(path, promise);
        }
    }

    public void setScenePath(String path) {
        if (t != null) {
            t.setScenePath(path);
        }
    }

    public void getSceneState(Promise<SceneState> promise) {
        if (t != null) {
            t.getSceneState(promise);
        }
    }

    public void disableDeviceInputs(boolean disabled) {
        if (t != null) {
            t.disableDeviceInputs(disabled);
        }
        disableDeviceInputs = disabled;
    }

    public void disableDeviceInputsTemporary(boolean disabled) {
        if (t != null) {
            t.disableDeviceInputs(disabled);
        }
    }

    public boolean isDisableDeviceInputs() {
        return disableDeviceInputs == null ? false : disableDeviceInputs;
    }

    public void disableCameraTransform(boolean disabled) {
        if (t != null) {
            t.disableCameraTransform(disabled);
        }
        disableCameraTransform = disabled;
    }

    public boolean isDisableCameraTransform() {
        return disableCameraTransform == null ? false : disableCameraTransform;
    }

    public void setWritable(boolean writable) {
        if (t != null) {
            MemberState memberState = new MemberState();
            memberState.setCurrentApplianceName(appliance);
            memberState.setStrokeColor(strokeColor);
            memberState.setTextSize(textSize);
            memberState.setStrokeWidth(strokeWidth);
            t.setWritable(writable, new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                    Constants.INSTANCE.getAgoraLog().i(TAG + "->setWritable-then:" + aBoolean);
                    if (aBoolean) {
                        // restore memberState
                        t.setMemberState(memberState);
                        disableSerialization(false);
                    }
                }

                @Override
                public void catchEx(SDKError t) {
                    Constants.INSTANCE.getAgoraLog().e(TAG + "->setWritable-catchEx:" + t.getJsStack());
                }
            });
        }
        this.writable = writable;
    }

    @Override
    public void setWritable(boolean writable, Promise<Boolean> promise) {
        if (t != null) {
            MemberState memberState = new MemberState();
            memberState.setCurrentApplianceName(appliance);
            memberState.setStrokeColor(strokeColor);
            memberState.setTextSize(textSize);
            memberState.setStrokeWidth(strokeWidth);
            t.setWritable(writable, new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                    Constants.INSTANCE.getAgoraLog().i(TAG + "->setWritable-then:" + aBoolean);
                    if (aBoolean) {
                        // restore memberState
                        t.setMemberState(memberState);
                        disableSerialization(false);
                    }
                    FcrBoardMainWindow.this.writable = writable;
                    promise.then(aBoolean);
                }

                @Override
                public void catchEx(SDKError t) {
                    Constants.INSTANCE.getAgoraLog().e(TAG + "->setWritable-catchEx:" + t.getJsStack());
                    promise.catchEx(t);
                }
            });
        }
    }

    public void disconnect() {
        if (t != null) {
            t.disconnect();
        }
    }

    public void setGlobalState(GlobalState state) {
        if (t != null) {
            t.setGlobalState(state);
        }
    }

    @Override
    public void setWindowApp(@NotNull WindowAppParam param, @Nullable Promise<String> promise) {
        if (t != null) {
            t.addApp(param, promise);
        }
    }

    @Override
    public void getWindowManagerAttributes(Promise<String> promise) {
        if (t != null) {
            t.getWindowManagerAttributes(promise);
        }
    }

    @Override
    public void setWindowManagerAttributes(String attributes) {
        if (t != null) {
            t.setWindowManagerAttributes(attributes);
        }
    }

    @Override
    public void changeMixingState(int state, int errorCode) {
        whiteSdk.getAudioMixerImplement().setMediaState(state, errorCode);
    }

    public void disconnect(Promise<Object> promise) {
        if (t != null) {
            t.disconnect(promise);
        }
    }

    @Override
    public void setSceneIndex(Integer index, @androidx.annotation.Nullable Promise<Boolean> promise) {
        if (t != null) {
            t.setSceneIndex(index, promise);
        }
    }

    @Override
    public void redo() {
        if (t != null) {
            t.redo();
        }
    }

    @Override
    public void undo() {
        if (t != null) {
            t.undo();
        }
    }

    @Override
    public void addPage() {
        if (t != null) {
            t.addPage(new Scene(UUID.randomUUID().toString()), true);
        }
    }

    @Override
    public void removePage() {

    }

    @Override
    public void prevPage() {
        if (t != null) {
            t.prevPage(new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                    Constants.INSTANCE.getAgoraLog().i("prevPage :->" + aBoolean);
                }

                @Override
                public void catchEx(SDKError t) {
                    Constants.INSTANCE.getAgoraLog().i("prevPage :->" + t.getMessage());
                }
            });
        }
    }

    @Override
    public void nextPage() {
        if (t != null) {
            t.nextPage(new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                    Constants.INSTANCE.getAgoraLog().i("nextPage :->" + aBoolean);
                }

                @Override
                public void catchEx(SDKError t) {
                    Constants.INSTANCE.getAgoraLog().i("nextPage :->" + t.getMessage());
                }
            });

            /*val scenes = arrayOf(Scene(UUID.randomUUID().toString()))
            if (boardRoom.boardState != null && boardRoom.boardState.sceneState != null) {
                val scenePath = boardRoom.boardState.sceneState.scenePath

                val lastSlashIndex = scenePath.lastIndexOf("/")
                val sceneDir = if (lastSlashIndex == 0) {
                    scenePath.substring(0, lastSlashIndex)
                } else {
                    "/"
                }
                val targetIndex = boardRoom.boardState.sceneState.index + 1

                boardRoom.putScenes(
                    sceneDir,
                    scenes,
                    targetIndex
                )
                boardRoom.setSceneIndex(targetIndex)
            }*/
        }
    }

    @Override
    public void onPhaseChanged(RoomPhase phase) {
        Constants.INSTANCE.getAgoraLog().i(TAG + "->onPhaseChanged:" + phase.name());
        if (listener != null) {
            handler.post(() -> listener.onRoomPhaseChanged(phase));
        }
    }

    @Override
    public void onDisconnectWithError(Exception e) {
        Constants.INSTANCE.getAgoraLog().e(TAG + "->onDisconnectWithError:" + e.getMessage());
        if (listener != null) {
            handler.post(() -> listener.onDisconnectWithError(e));
        }
    }

    @Override
    public void onKickedWithReason(String reason) {
        Constants.INSTANCE.getAgoraLog().w(TAG + "->onKickedWithReason:" + reason);
    }

    @Override
    public void onRoomStateChanged(RoomState modifyState) {
        Constants.INSTANCE.getAgoraLog().d(TAG + "->onRoomStateChanged:" + new Gson().toJson(modifyState));
        if (modifyState.getBroadcastState() != null && modifyState.getBroadcastState().getBroadcasterId() == null) {
            Constants.INSTANCE.getAgoraLog().i(TAG + "->onRoomStateChanged:teacher is not here, scalePptToFit");
            scalePptToFit(AnimationMode.Continuous);
        }

        if (listener != null) {
            handler.post(() -> listener.onRoomStateChanged(modifyState));

            GlobalState state = modifyState.getGlobalState();
            if (state != null) {
                handler.post(() -> listener.onGlobalStateChanged(state));
            }
            MemberState memberState = modifyState.getMemberState();
            if (memberState != null) {
                handler.post(() -> listener.onMemberStateChanged(memberState));
            }
            SceneState sceneState = modifyState.getSceneState();
            if (sceneState != null) {
                handler.post(() -> listener.onSceneStateChanged(sceneState));
            }

            CameraState cameraState = modifyState.getCameraState();
            if (cameraState != null) {
                handler.post(() -> listener.onCameraStateChanged(cameraState));
            }
        }
    }

    @Override
    public void onCanUndoStepsUpdate(long canUndoSteps) {
        Constants.INSTANCE.getAgoraLog().w(TAG + "->onCanUndoStepsUpdate:" + canUndoSteps);
        if (listener != null) {
            listener.onCanUndoStepsUpdate(canUndoSteps);
        }
    }

    @Override
    public void onCanRedoStepsUpdate(long canRedoSteps) {
        Constants.INSTANCE.getAgoraLog().w(TAG + "->onCanRedoStepsUpdate:" + canRedoSteps);

        if (listener != null) {
            listener.onCanRedoStepsUpdate(canRedoSteps);
        }
    }

    /**
     * 结合 onCanUndoStepsUpdate & onCanRedoStepsUpdate 回掉使用的，
     * 设置false才有callback，
     * 注意要在setWritable为true调用
     *
     * @param disable
     */
    @Override
    public void disableSerialization(boolean disable) {
        if (t != null) {
            t.disableSerialization(disable);
        }
    }

    @Override
    public void setContainerSizeRatio(float ratio) {
        if (t != null) {
            t.setContainerSizeRatio(ratio);
        }
    }

    @Override
    public void onCatchErrorWhenAppendFrame(long userId, Exception error) {
        Constants.INSTANCE.getAgoraLog().e(TAG + "->onCatchErrorWhenAppendFrame->userId:" + userId + "error:" + error.getMessage());
    }

    @Override
    public void onSuccess(Room room) {
        Constants.INSTANCE.getAgoraLog().d(TAG + "->onSuccess->room:" + roomParams.toString());
        if (appliance != null) {
            setAppliance(appliance);
        }
        if (strokeColor != null) {
            setStrokeColor(strokeColor);
        }
        if (strokeWidth != -100f) {
            setStrokeWidth(strokeWidth);
        }
        if (textSize != -100f) {
            setTextSize(textSize);
        }
        if (disableDeviceInputs != null) {
            disableDeviceInputs(disableDeviceInputs);
        }
        if (disableCameraTransform != null) {
            disableCameraTransform(disableCameraTransform);
        }
        if (writable != null) {
            setWritable(writable);
        }
        if (listener != null) {
            if (!joinSuccess) {
                joinSuccess = true;
                if (getBoardState() != null) {
                    GlobalState state = getBoardState().getGlobalState();
                    if (state != null) {
                        listener.onJoinSuccess(state);
                    }
                }
            }
            if (room.getSceneState() != null) {
                listener.onSceneStateChanged(room.getSceneState());
            }
        }
    }

    @Override
    public void onFail(SDKError error) {
        Constants.INSTANCE.getAgoraLog().e(TAG + "->onFail:" + error.toString());
        String sdkInitFailed = "sdk init failed jsStack: Unknow stack";
        String magixConnectFailed = "magix connect fail";
        if (error.toString().contains(sdkInitFailed) && joinFailedRetry > 0) {
            if (whiteSdk == null && roomParams == null) {
                return;
            }
            Constants.INSTANCE.getAgoraLog().i(TAG + "->joinRoom-retry-sdkInitFailed");
            this.whiteSdk.joinRoom(this.roomParams, this, promise);
            joinFailedRetry--;
            return;
        } else if (error.toString().contains(magixConnectFailed) && connectFailedRetry > 0) {
            if (whiteSdk == null && roomParams == null) {
                return;
            }
            Constants.INSTANCE.getAgoraLog().i(TAG + "->joinRoom-retry-magixConnectFailed");
            this.whiteSdk.joinRoom(this.roomParams, this, promise);
            connectFailedRetry--;
            return;
        }
        listener.onJoinFail(error);
    }
}