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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.agora.base.PreferenceManager;
import io.agora.base.ToastManager;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.BuildConfig;
import io.agora.edu.R;
import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.LargeClassActivity;
import io.agora.edu.classroom.OneToOneClassActivity;
import io.agora.edu.classroom.SmallClassActivity;
import io.agora.edu.common.api.BoardPreload;
import io.agora.edu.common.api.RoomPre;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import io.agora.edu.common.impl.BoardPreloadImpl;
import io.agora.edu.common.impl.RoomPreImpl;
import io.agora.edu.common.listener.BoardPreloadListener;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.manager.EduManager;
import io.agora.education.api.manager.EduManagerOptions;
import io.agora.education.api.room.data.RoomType;
import io.agora.extension.AgoraExtAppConfiguration;
import io.agora.extension.AgoraExtAppEngine;
import io.agora.report.ReportManager;
import io.agora.report.reporters.APaasReporter;
import io.agora.uicomponent.UiWidgetConfig;
import io.agora.uicomponent.UiWidgetManager;
import io.agora.uikit.impl.chat.AgoraUIChatWindow;
import io.agora.uikit.impl.chat.EaseChatWidget;

import static io.agora.edu.common.impl.RoomPreImpl.ROOMEND;
import static io.agora.edu.common.impl.RoomPreImpl.ROOMFULL;

public class AgoraEduSDK {
    private static final String TAG = "AgoraEduSDK";

    public static final int REQUEST_CODE_RTC = 101;
    public static final int REQUEST_CODE_RTE = 909;
    public static final String CODE = "code";
    public static final String REASON = "reason";
    public static AgoraEduLaunchCallback defaultCallback = state -> Log.e(TAG, ":This is the default null implementation!");
    public static AgoraEduLaunchCallback agoraEduLaunchCallback = defaultCallback;
    private static RoomPre roomPre;
    private static AgoraEduSDKConfig agoraEduSDKConfig;
    private static final AgoraEduClassRoom classRoom = new AgoraEduClassRoom();
    public static final String DYNAMIC_URL = "https://convertcdn.netless.link/dynamicConvert/%s.zip";
    public static final String STATIC_URL = "https://convertcdn.netless.link/staticConvert/%s.zip";
    public static final String PUBLIC_FILE_URL = "https://convertcdn.netless.link/publicFiles.zip";
    public static List<AgoraEduCourseware> COURSEWARES = Collections.synchronizedList(new ArrayList<>());
    private static String baseUrl = BuildConfig.API_BASE_URL;
    private static String reportUrl = BuildConfig.REPORT_BASE_URL;
    private static String reportUrlV2 = BuildConfig.REPORT_BASE_URL_V2;

    public static String baseUrl() {
        return baseUrl;
    }

    public static String reportUrl() {
        return reportUrl;
    }

    public static String reportUrlV2() {
        return reportUrlV2;
    }

    public static void setParameters(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("edu.apiUrl")) {
                baseUrl = obj.getString("edu.apiUrl");
            }

            if (obj.has("edu.reportUrl")) {
                reportUrl = obj.getString("edu.reportUrl");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static final ActivityLifecycleListener classRoomListener = new ActivityLifecycleListener() {
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

    private static BoardPreload boardPreload;

    public static String version() {
        return EduManager.Companion.version();
    }

    public static void dispose() {
        COURSEWARES.clear();
        agoraEduSDKConfig = null;
        if (boardPreload != null) {
            boardPreload.cancelAllPreloadTask();
        }
        agoraEduLaunchCallback = defaultCallback;
    }

    public static void configCoursewares(@NotNull List<AgoraEduCourseware> coursewares) {
        for (AgoraEduCourseware courseware : coursewares) {
            if (!COURSEWARES.contains(coursewares)) {
                COURSEWARES.add(courseware);
            }
        }
    }

    public static void downloadCoursewares(@NotNull Context context, @Nullable AgoraEduCoursewarePreloadListener listener)
            throws Exception {
        if (!classRoom.isIdle()) {
            // classRoom is running, return
            callbackError(context, "classRoom is running!");
            return;
        }
        if (boardPreload == null || !boardPreload.isAvailable()) {
            boardPreload = new BoardPreloadImpl(context);
        }
        boardPreload.preload(PUBLIC_FILE_URL, null);
        for (AgoraEduCourseware ware : COURSEWARES) {
            if (!TextUtils.isEmpty(ware.getResourceUrl())) {
                boardPreload.preload(ware.getResourceUrl(), new BoardPreloadListener() {
                    @Override
                    public void onStartDownload(@NotNull String url) {
                        listener.onStartDownload(ware);
                    }

                    @Override
                    public void onProgress(@NotNull String url, double progress) {
                        listener.onProgress(ware, progress);
                    }

                    @Override
                    public void onComplete(@NotNull String url) {
                        listener.onComplete(ware);
                    }

                    @Override
                    public void onFailed(@NotNull String url) {
                        listener.onFailed(ware);
                    }
                });
            } else {
                callbackError(context, "resourceUrl is empty!");
                listener.onFailed(ware);
            }
        }
    }

    private static void pauseAllCacheTask() {
        if (boardPreload != null && boardPreload.isAvailable()) {
            boardPreload.cancelAllPreloadTask();
        }
    }

    public static void setConfig(@NotNull AgoraEduSDKConfig agoraEduSDKConfig) {
        AgoraEduSDK.agoraEduSDKConfig = agoraEduSDKConfig;
    }

    public static void registerExtApps(List<AgoraExtAppConfiguration> apps) {
        AgoraExtAppEngine.Companion.registerExtAppList(apps);
    }

    private static APaasReporter getReporter() {
        return ReportManager.INSTANCE.getAPaasReporter();
    }

    public static AgoraEduClassRoom launch(@NotNull Context context,
                                           @NotNull AgoraEduLaunchConfig config,
                                           @NotNull AgoraEduLaunchCallback callback) {
        // Register default widgets globally here because we must ensure
        // users call this register method just before they use our edu
        // library and will relief them registering default widgets in their code.
        // Then there will be a chance to replace the widgets of their own.
        // Widget registering will not depend on any other part of classroom
        // mechanism, so we handle it at the beginning of the classroom launch.
        ArrayList<UiWidgetConfig> widgetConfigs = new ArrayList<>();
        // Not need it for now, if you need, please open notes.
//        widgetConfigs.add(new UiWidgetConfig(
//                UiWidgetManager.DefaultWidgetId.Chat.name(),
//                AgoraUIChatWindow.class));
        widgetConfigs.add(new UiWidgetConfig(
                UiWidgetManager.DefaultWidgetId.HyphenateChat.name(),
                EaseChatWidget.class));
        UiWidgetManager.Companion.registerDefaultOnce(widgetConfigs);

        // Register user-defined widgets as long as they maintain
        // their widget ids somewhere else.
        // Can replace any widgets that are registered as default above.
        if (config.getWidgetConfigs() != null) {
            UiWidgetManager.Companion.registerAndReplace(config.getWidgetConfigs());
        }

        // before launch, pause All CacheTask
        pauseAllCacheTask();
        // step-0: get agoraEduSDKConfig and to configure
        if (agoraEduSDKConfig == null) {
            Log.e(TAG, ":agoraEduSDKConfig is null!");
            return null;
        }

        config.setAppId(agoraEduSDKConfig.getAppId());
        config.setEyeCare(agoraEduSDKConfig.getEyeCare());

        ReportManager.INSTANCE.init("flexibleClass", "android", config.getAppId());
        ReportManager.INSTANCE.setJoinRoomInfo(config.getRoomUuid(),
                config.getUserUuid(), UUID.randomUUID().toString());
        getReporter().reportRoomEntryStart(null);

        if (!classRoom.isIdle()) {
            String msg = "curState is not AgoraEduEventDestroyed, launch() cannot be called";
            callbackError(context, msg);
        }

        if (agoraEduSDKConfig.getEyeCare() != 0 && agoraEduSDKConfig.getEyeCare() != 1) {
            String msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduSDKConfig.eyeCare is not expected, it must be 0 or 1!");
            callbackError(context, msg);
        }

        if (!AgoraEduRoleType.isValid(config.getRoleType())) {
            String msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduLaunchConfig.roleType is not expected, it must be 2!");
            callbackError(context, msg);
        }

        if (!AgoraEduRoomType.isValid(config.getRoomType())) {
            String msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduLaunchConfig.roomType is not expected, it must be 0 or 4 or 2 !");
            callbackError(context, msg);
        }

        ((Application) context.getApplicationContext()).unregisterActivityLifecycleCallbacks(classRoomListener);
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(classRoomListener);

        agoraEduLaunchCallback = state -> {
            callback.onCallback(state);
            classRoom.updateState(state);
        };

        ToastManager.init(context.getApplicationContext());
        PreferenceManager.init(context.getApplicationContext());

        if (!TextUtils.isEmpty(config.getRtmToken())) {
            RetrofitManager.instance().addHeader("x-agora-token", config.getRtmToken());
            RetrofitManager.instance().addHeader("x-agora-uid", config.getUserUuid());
        }

        // step-1:pull remote config
        roomPre = new RoomPreImpl(config.getAppId(), config.getRoomUuid());
        roomPre.pullRemoteConfig(new EduCallback<EduRemoteConfigRes>() {
            @Override
            public void onSuccess(@Nullable EduRemoteConfigRes res) {
                EduRemoteConfigRes.NetLessConfig netLessConfig = res.getNetless();
                config.setWhiteBoardAppId(netLessConfig.getAppId());
                config.setVendorId(res.getVid());
                // step-2:check classRoom and init EduManager
                checkAndInit(context, config);
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                String msg = "pullRemoteConfig failed->code:" + error.getType() + ",msg:" + error.getMsg();
                callbackError(context, msg);
                getReporter().reportRoomEntryEnd("0", error.getType() + "",
                        error.getHttpError() + "", null);
            }
        });

        return classRoom;
    }

    private static void checkAndInit(@NotNull Context context, @NotNull AgoraEduLaunchConfig config) {
        getReporter().reportPreCheckStart();

        RoomPreCheckReq req = new RoomPreCheckReq(config.getRoomName(), config.getRoomType(),
                String.valueOf(AgoraEduRoleType.AgoraEduRoleTypeStudent.getValue()),
                config.getStartTime(), config.getDuration(), config.getUserName(),
                config.getBoardRegion(), config.getUserProperties());
        roomPre.preCheckClassRoom(config.getUserUuid(), req, new EduCallback<RoomPreCheckRes>() {
            @Override
            public void onSuccess(@Nullable RoomPreCheckRes preCheckRes) {
                assert preCheckRes != null;
                EduManagerOptions options = new EduManagerOptions(context, config.getAppId(),
                        config.getRtmToken(), config.getUserUuid(), config.getUserName());
                options.setLogFileDir(context.getCacheDir().getAbsolutePath());
                EduManager.init(options, new EduCallback<EduManager>() {
                    @Override
                    public void onSuccess(@Nullable EduManager res) {
                        if (res != null) {
                            Log.e(TAG, ":init EduManager success");
                            BaseClassActivity.EduManagerDelegate.setEduManager(res);
                            Intent intent = createIntent(context, config, preCheckRes);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                        String msg = "init EduManager failed->code:" + error.getType() + ",reason:" + error.getMsg();
                        callbackError(context, msg);
                        getReporter().reportRoomEntryEnd("0", error.getType() + "",
                                error.getHttpError() + "", null);
                    }
                });
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                String msg = "preCheckClassRoom failed->code:" + error.getType() + ",msg:" + error.getMsg();
                switch (error.getType()) {
                    case ROOMEND:
                        msg = "Room is End!";
                        break;
                    case ROOMFULL:
                        msg = context.getString(R.string.room_full);
                        break;
                    default:
                        break;
                }

                if (error.getType() == 30403100) {
                    callbackError(context, AgoraEduEvent.AgoraEduEventForbidden, msg);
                } else {
                    callbackError(context, msg);
                }
                getReporter().reportRoomEntryEnd("0", error.getType() + "",
                        error.getHttpError() + "", null);
            }
        });
    }

    private static Intent createIntent(@NotNull Context context, @NotNull AgoraEduLaunchConfig config,
                                       @NotNull RoomPreCheckRes preCheckRes) {
        Intent intent = new Intent();
        int roomType = config.getRoomType();
        if (roomType == RoomType.ONE_ON_ONE.getValue()) {
            intent.setClass(context, OneToOneClassActivity.class);
        } else if (roomType == RoomType.SMALL_CLASS.getValue()) {
            intent.setClass(context, SmallClassActivity.class);
        } else if (roomType == RoomType.LARGE_CLASS.getValue()) {
            intent.setClass(context, LargeClassActivity.class);
        }

        intent.putExtra(BaseClassActivity.Data.launchConfig, config);
        intent.putExtra(BaseClassActivity.Data.precheckData, preCheckRes);
        return intent;
    }

    private static void callbackError(Context context, String msg) {
        callbackError(context, AgoraEduEvent.AgoraEduEventFailed, msg);
    }

    private static void callbackError(Context context, AgoraEduEvent event, String msg) {
        Log.e(TAG, msg);
        agoraEduLaunchCallback.onCallback(event);
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
        Log.e(TAG, msg);
    }
}