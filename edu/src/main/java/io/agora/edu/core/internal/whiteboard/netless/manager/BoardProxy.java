package io.agora.edu.core.internal.whiteboard.netless.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.herewhite.sdk.Room;
import com.herewhite.sdk.RoomCallbacks;
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

import io.agora.edu.core.internal.whiteboard.netless.annotation.Appliance;
import io.agora.edu.core.internal.whiteboard.netless.listener.BoardEventListener;

public class BoardProxy extends NetlessManager<Room> implements RoomCallbacks {
    private static final String TAG = "BoardProxy";

    private String appliance;
    private int[] strokeColor;
    private double strokeWidth = -100f, textSize = -100f;
    private Boolean disableDeviceInputs;
    private Boolean disableCameraTransform;
    private Boolean writable;

    private Handler handler = new Handler(Looper.getMainLooper());
    private BoardEventListener listener;
    private boolean joinSuccess = false;

    private final String sdkInitFailed = "sdk init failed jsStack: Unknow stack";
    private int joinFailedRetry = 2;
    private final String magixConnectFailed = "magix connect fail";
    private int connectFailedRetry = 2;
    private WhiteSdk whiteSdk;
    private RoomParams roomParams;

    public void setListener(BoardEventListener listener) {
        this.listener = listener;
    }

    public void init(WhiteSdk sdk, RoomParams params) {
        this.whiteSdk = sdk;
        this.roomParams = params;
//        if (t != null) {
//            Log.i(TAG, "joinRoom-disconnect");
//            disconnect(new Promise() {
//                @Override
//                public void then(Object o) {
//                    Log.i(TAG, "joinRoom-disconnect-success");
//                    Log.i(TAG, "joinRoom-");
//                    sdk.joinRoom(params, BoardProxy.this, promise);
//                }
//
//                @Override
//                public void catchEx(SDKError t) {
//                    Log.i(TAG, "joinRoom-disconnect-failed->" + t.getJsStack());
//                    Log.i(TAG, "joinRoom-");
//                    sdk.joinRoom(params, BoardProxy.this, promise);
//                }
//            });
//        } else {
//            Log.i(TAG, "joinRoom-");
//            sdk.joinRoom(params, BoardProxy.this, promise);
//        }
        Log.i(TAG, "joinRoom-");
        sdk.joinRoom(params, BoardProxy.this, promise);
    }

    public void setAppliance(@Appliance String appliance) {
        if (t != null) {
            MemberState state = new MemberState();
            state.setCurrentApplianceName(appliance);
            t.setMemberState(state);
        } else {
            this.appliance = appliance;
        }
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
        } else {
            this.strokeColor = color;
        }
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
        } else {
            this.strokeWidth = width;
        }
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
        } else {
            this.textSize = size;
        }
    }

    public Double getTextSize() {
        if (t != null) {
            return t.getMemberState().getTextSize();
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
        if (t != null) {
            t.setViewMode(follow ? ViewMode.Follower : ViewMode.Freedom);
        }
    }

    public void scalePptToFit(AnimationMode mode) {
        if (t != null) {
            t.scalePptToFit(mode);
            t.scaleIframeToFit();
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

    public void getSceneState(Promise<SceneState> mise) {
        if(t != null) {
            t.getSceneState(mise);
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
            t.setWritable(writable, new Promise<Boolean>() {
                @Override
                public void then(Boolean aBoolean) {
                }

                @Override
                public void catchEx(SDKError t) {
                }
            });
        }
        this.writable = writable;
    }

    public void disconnect() {
        if (t != null) {
            t.disconnect();
        }
    }

    public void disconnect(Promise promise) {
        if (t != null) {
            t.disconnect(promise);
        }
    }

    @Override
    public void onPhaseChanged(RoomPhase phase) {
        Log.e(TAG, "onPhaseChanged->" + phase.name());
        if (listener != null) {
            handler.post(() -> listener.onRoomPhaseChanged(phase));
        }
    }

    @Override
    public void onDisconnectWithError(Exception e) {
        Log.e(TAG, "onDisconnectWithError->" + e.getMessage());
        if (listener != null) {
            handler.post(() -> listener.onDisconnectWithError(e));
        }
    }

    @Override
    public void onKickedWithReason(String reason) {

    }

    @Override
    public void onRoomStateChanged(RoomState modifyState) {
        Log.e(TAG, "onRoomStateChanged->" + new Gson().toJson(modifyState));
        if (modifyState.getBroadcastState() != null && modifyState.getBroadcastState()
                .getBroadcasterId() == null) {
            scalePptToFit(AnimationMode.Continuous);
        }
        if (listener != null) {
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

    }

    @Override
    public void onCanRedoStepsUpdate(long canRedoSteps) {

    }

    @Override
    public void onCatchErrorWhenAppendFrame(long userId, Exception error) {

    }

    @Override
    void onSuccess(Room room) {
        Log.e(TAG, "onSuccess:");
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
                listener.onJoinSuccess(getBoardState().getGlobalState());
            }
            listener.onSceneStateChanged(room.getSceneState());
        }
    }

    @Override
    void onFail(SDKError error) {
        Log.e(TAG, "onFail " + error.toString());
        if (error.toString().contains(sdkInitFailed) && joinFailedRetry > 0) {
            if (whiteSdk == null && roomParams == null) {
                return;
            }
            Log.e(TAG, "joinRoom-retry-sdkInitFailed");
            this.whiteSdk.joinRoom(this.roomParams, this, promise);
            joinFailedRetry--;
            return;
        } else if (error.toString().contains(magixConnectFailed) && connectFailedRetry > 0) {
            if (whiteSdk == null && roomParams == null) {
                return;
            }
            Log.e(TAG, "joinRoom-retry-magixConnectFailed");
            this.whiteSdk.joinRoom(this.roomParams, this, promise);
            connectFailedRetry--;
            return;
        }
        listener.onJoinFail(error);
    }
}
