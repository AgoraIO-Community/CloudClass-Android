package io.agora.edu.core.internal.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.agora.edu.core.internal.base.callback.ThrowableCallback;
import io.agora.edu.core.internal.base.network.BusinessException;
import io.agora.edu.core.internal.base.network.RetrofitManager;
import io.agora.edu.core.internal.edu.common.api.Base;
import io.agora.edu.core.internal.edu.common.api.RoomPre;
import io.agora.edu.core.internal.edu.common.bean.ResponseBody;
import io.agora.edu.core.internal.server.struct.request.DeviceStateUpdateReq;
import io.agora.edu.core.internal.server.struct.request.RoomPreCheckReq;
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.RoomPreService;
import io.agora.edu.core.internal.launch.AgoraEduSDK;
import io.agora.edu.core.internal.server.struct.response.EduRemoteConfigRes;
import io.agora.edu.core.internal.server.struct.response.RoomPreCheckRes;
import io.agora.edu.core.internal.util.TimeUtil;
import io.agora.edu.core.internal.framework.data.EduCallback;
import io.agora.edu.core.internal.framework.data.EduError;
import io.agora.edu.core.internal.report.ReportManager;
import io.agora.edu.core.internal.report.reporters.APaasReporter;

public class RoomPreImpl extends Base implements RoomPre {
    private static final String TAG = "RoomPreImpl";
    public static final int ROOMEND = 20410100;
    public static final int ROOMFULL = 20403001;

    public  RoomPreImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void preCheckClassRoom(String userUuid, RoomPreCheckReq req, EduCallback<RoomPreCheckRes> callback) {
        APaasReporter reporter = ReportManager.INSTANCE.getAPaasReporter();
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), RoomPreService.class)
                .preCheckClassroom(appId, roomUuid, userUuid, req)
                .enqueue(new RetrofitManager.Callback<>(0, new ThrowableCallback<ResponseBody<RoomPreCheckRes>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if (throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                            reporter.reportPreCheckResult("0", e.getCode() + "",
                                    e.getHttpCode() + "", null);
                        } else {
                            EduError error = EduError.Companion.customMsgError(throwable.getMessage());
                            callback.onFailure(error);
                            reporter.reportPreCheckResult("0", error.getType() + "", null, null);
                        }
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<RoomPreCheckRes> res) {
                        if (res != null && res.data != null) {
                            TimeUtil.calibrateTimestamp(res.ts);
                            callback.onSuccess(res.data);
                            reporter.reportPreCheckResult("1", null, null, null);
                        } else {
                            EduError error = EduError.Companion.customMsgError("response is null");
                            callback.onFailure(error);
                            reporter.reportPreCheckResult("0", error.getType() + "", null, null);
                        }
                    }
                }));
    }

    @Override
    public void pullRemoteConfig(EduCallback<EduRemoteConfigRes> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), RoomPreService.class)
                .pullRemoteConfig(appId)
                .enqueue(new RetrofitManager.Callback<>(0, new ThrowableCallback<ResponseBody<EduRemoteConfigRes>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if (throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<EduRemoteConfigRes> res) {
                        if (res != null) {
                            callback.onSuccess(res.data);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void updateDeviceState(String userUuid, DeviceStateUpdateReq req) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), RoomPreService.class)
                .updateDeviceState(appId, roomUuid, userUuid, req)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<String>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<String> res) {
                    }
                }));
    }
}