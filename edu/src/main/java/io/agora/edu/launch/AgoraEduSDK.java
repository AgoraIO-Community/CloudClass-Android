package io.agora.edu.launch;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.agora.base.PreferenceManager;
import io.agora.base.ToastManager;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.R;
import io.agora.edu.classroom.ReplayActivity;
import io.agora.edu.common.api.RoomPre;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import io.agora.edu.common.impl.RoomPreImpl;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.manager.EduManager;
import io.agora.education.api.manager.EduManagerOptions;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.RoomType;
import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.BreakoutClassActivity;
import io.agora.edu.classroom.LargeClassActivity;
import io.agora.edu.classroom.MediumClassActivity;
import io.agora.edu.classroom.OneToOneClassActivity;
import io.agora.edu.classroom.SmallClassActivity;

import static io.agora.edu.classroom.BaseClassActivity.setEduManager;

public class AgoraEduSDK {
    private static final String TAG = "EduLaunch";

    public static final int REQUEST_CODE_RTC = 101;
    public static final int REQUEST_CODE_RTE = 909;
    public static final String CODE = "code";
    public static final String REASON = "reason";
    public static AgoraEduLaunchCallback agoraEduLaunchCallback = state -> Log.e(TAG, ":This is the default null implementation!");
    private static RoomPre roomPre;
    private static AgoraEduSDKConfig agoraEduSDKConfig;
    private static final AgoraEduClassRoom classRoom = new AgoraEduClassRoom();
    private static final AgoraEduReplay replay = new AgoraEduReplay();
    private static ActivityLifecycleListener classRoomListener = new ActivityLifecycleListener() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            Log.i(TAG, ":classRoomListener->onActivityCreated");
            classRoom.add(activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Log.i(TAG, ":classRoomListener->onActivityDestroyed");
            classRoom.updateState(AgoraEduEvent.AgoraEduEventDestroyed);
        }
    };
    private static ActivityLifecycleListener replayListener = new ActivityLifecycleListener() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            Log.i(TAG, ":replayListener->onActivityDestroyed");
            replay.add(activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Log.i(TAG, ":replayListener->onActivityDestroyed");
            replay.updateState(AgoraEduEvent.AgoraEduEventDestroyed);
            agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventDestroyed);
        }
    };

    public static String version() {
        return EduManager.Companion.version();
    }

    public static void setConfig(@NotNull AgoraEduSDKConfig agoraEduSDKConfig) {
        AgoraEduSDK.agoraEduSDKConfig = agoraEduSDKConfig;
    }

    public static AgoraEduClassRoom launch(@NotNull Context context, @NotNull AgoraEduLaunchConfig config,
                                           @NotNull AgoraEduLaunchCallback callback)
            throws Exception {
        if (!classRoom.isReady()) {
            String msg = "curState is not AgoraEduEventDestroyed, launch() cannot be called";
            errorTips(context, msg);
        }

        if(agoraEduSDKConfig.getEyeCare() != 0 && agoraEduSDKConfig.getEyeCare() != 1) {
            String msg = String.format(context.getString(R.string.parametererrpr), "The value of " +
                    "AgoraEduSDKConfig.eyeCare is not expected, it must be 0 or 1!");
            errorTips(context, msg);
        }

        if(!AgoraEduRoleType.isValid(config.getRoleType())) {
            String msg = String.format(context.getString(R.string.parametererrpr), "The value of " +
                    "AgoraEduLaunchConfig.roleType is not expected, it must be 2!");
            errorTips(context, msg);
        }

        if(!AgoraEduRoomType.isValid(config.getRoomType())) {
            String msg = String.format(context.getString(R.string.parametererrpr), "The value of " +
                    "AgoraEduLaunchConfig.roomType is not expected, it must be 0 or 1 or 2!");
            errorTips(context, msg);
        }


        ((Application) context.getApplicationContext()).unregisterActivityLifecycleCallbacks(classRoomListener);
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(classRoomListener);

        agoraEduLaunchCallback = state -> {
            callback.onCallback(state);
            classRoom.updateState(state);
        };
        ToastManager.init(context.getApplicationContext());
        PreferenceManager.init(context.getApplicationContext());

        /**step-0:get agoraEduSDKConfig and to configure*/
        if (agoraEduSDKConfig == null) {
            Log.e(TAG, ":agoraEduSDKConfig is null!");
            return null;
        }
        config.setAppId(agoraEduSDKConfig.getAppId());
        config.setEyeCare(agoraEduSDKConfig.getEyeCare());
        if (!TextUtils.isEmpty(config.getRtmToken())) {
            RetrofitManager.instance().addHeader("x-agora-token", config.getRtmToken());
            RetrofitManager.instance().addHeader("x-agora-uid", config.getUserUuid());
        }

        /**step-1:pull remote config*/
        roomPre = new RoomPreImpl(config.appId, config.getRoomUuid());
        roomPre.pullRemoteConfig(new EduCallback<EduRemoteConfigRes>() {
            @Override
            public void onSuccess(@Nullable EduRemoteConfigRes res) {
                EduRemoteConfigRes.NetLessConfig netLessConfig = res.getNetless();
                config.whiteBoardAppId = netLessConfig.getAppId();
                /**step-2:check classRoom and init EduManager*/
                checkAndInit(context, config);
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                String msg = "pullRemoteConfig failed->code:" + error.getType() + ",msg:" + error.getMsg();
                callbackError(context, msg);
            }
        });

        return classRoom;
    }

    private static void checkAndInit(@NotNull Context context, @NotNull AgoraEduLaunchConfig config) {
        RoomPreCheckReq req = new RoomPreCheckReq(config.getRoomName(), config.getRoomType());
        roomPre.preCheckClassRoom(config.getUserUuid(), req, new EduCallback<RoomPreCheckRes>() {
            @Override
            public void onSuccess(@Nullable RoomPreCheckRes res) {
                if (res.getState() != EduRoomState.END.getValue()) {
                    EduManagerOptions options = new EduManagerOptions(context, config.appId,
                            config.getRtmToken(), config.getUserUuid(), config.getUserName());
                    options.setLogFileDir(context.getCacheDir().getAbsolutePath());
                    EduManager.init(options, new EduCallback<EduManager>() {
                        @Override
                        public void onSuccess(@Nullable EduManager res) {
                            if (res != null) {
                                Log.e(TAG, ":初始化EduManager成功");
                                setEduManager(res);
                                Intent intent = createIntent(context, config);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            }
                        }

                        @Override
                        public void onFailure(@NotNull EduError error) {
                            String msg = "初始化EduManager失败->code:" + error.getType() + ",reason:" + error.getMsg();
                            callbackError(context, msg);
                        }
                    });
                } else {
                    String msg = "Room is End!";
                    callbackError(context, msg);
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                String msg = "preCheckClassRoom failed->code:" + error.getType() + ",msg:" + error.getMsg();
                callbackError(context, msg);
            }
        });
    }

    private static Intent createIntent(@NotNull Context context, @NotNull AgoraEduLaunchConfig config) {
        Intent intent = new Intent();
        int roomType = config.getRoomType();
        if (roomType == RoomType.ONE_ON_ONE.getValue()) {
            intent.setClass(context, OneToOneClassActivity.class);
        } else if (roomType == RoomType.SMALL_CLASS.getValue()) {
            intent.setClass(context, SmallClassActivity.class);
        } else if (roomType == RoomType.LARGE_CLASS.getValue()) {
            intent.setClass(context, LargeClassActivity.class);
        } else if (roomType == RoomType.BREAKOUT_CLASS.getValue()) {
            intent.setClass(context, BreakoutClassActivity.class);
        } else if (roomType == RoomType.MEDIUM_CLASS.getValue()) {
            intent.setClass(context, MediumClassActivity.class);
        }
        intent.putExtra(BaseClassActivity.LAUNCHCONFIG, config);
        return intent;
    }

    private static void callbackError(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        Log.e(TAG, msg);
        agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventDestroyed);
    }

    private static void errorTips(Context context, String msg) throws Exception {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        Log.e(TAG, msg);
        throw new Exception(msg);
    }


    public static final String WHITEBOARD_APP_ID = "whiteboardAppId";
    public static final String WHITEBOARD_START_TIME = "whiteboardStartTime";
    public static final String WHITEBOARD_END_TIME = "whiteboardEndTime";
    public static final String VIDEO_URL = "videoURL";
    public static final String WHITEBOARD_ID = "whiteboardId";
    public static final String WHITEBOARD_TOKEN = "whiteboardToken";

    public static AgoraEduReplay replay(@NotNull Context context, @NotNull AgoraEduReplayConfig config, @NotNull AgoraEduLaunchCallback callback)
            throws IllegalStateException {
        if (!replay.isReady()) {
            throw new IllegalStateException("curState is not AgoraEduEventDestroyed, replay() cannot be called");
        }

        ((Application) context.getApplicationContext()).unregisterActivityLifecycleCallbacks(replayListener);
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(replayListener);

        agoraEduLaunchCallback = callback;
        ToastManager.init(context.getApplicationContext());
        PreferenceManager.init(context.getApplicationContext());

        Intent intent = new Intent(context, ReplayActivity.class);
        intent.putExtra(WHITEBOARD_APP_ID, config.getWhiteBoardAppId());
        intent.putExtra(WHITEBOARD_START_TIME, config.getBeginTime());
        intent.putExtra(WHITEBOARD_END_TIME, config.getEndTime());
        intent.putExtra(VIDEO_URL, config.getVideoUrl());
        intent.putExtra(WHITEBOARD_ID, config.getWhiteBoardId());
        intent.putExtra(WHITEBOARD_TOKEN, config.getWhiteBoardToken());
        context.startActivity(intent);
        replay.updateState(AgoraEduEvent.AgoraEduEventReady);
        agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventReady);
        return replay;
    }
}
