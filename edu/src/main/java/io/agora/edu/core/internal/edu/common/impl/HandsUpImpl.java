package io.agora.edu.core.internal.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.agora.edu.core.internal.base.callback.ThrowableCallback;
import io.agora.edu.core.internal.base.network.BusinessException;
import io.agora.edu.core.internal.base.network.ResponseBody;
import io.agora.edu.core.internal.base.network.RetrofitManager;
import io.agora.edu.core.internal.edu.common.api.Base;
import io.agora.edu.core.internal.edu.common.api.HandsUp;
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.HandsUpService;
import io.agora.edu.core.internal.launch.AgoraEduSDK;
import io.agora.edu.core.internal.framework.data.EduCallback;
import io.agora.edu.core.internal.framework.data.EduError;

public class HandsUpImpl extends Base implements HandsUp {
    private static final String TAG = "RaiseHandImpl";

    public HandsUpImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void applyHandsUp(@NotNull EduCallback<Boolean> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), HandsUpService.class)
                .applyHandsUp(appId, roomUuid)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<String>>() {
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
                    public void onSuccess(@Nullable ResponseBody<String> res) {
                        if (res != null) {
                            callback.onSuccess(true);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void cancelApplyHandsUp(@NotNull EduCallback<Boolean> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), HandsUpService.class)
                .cancelApplyHandsUp(appId, roomUuid)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<String>>() {
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
                    public void onSuccess(@Nullable ResponseBody<String> res) {
                        if (res != null) {
                            callback.onSuccess(true);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void exitHandsUp(EduCallback<Boolean> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), HandsUpService.class)
                .exitHandsUp(appId, roomUuid)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<String>>() {
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
                    public void onSuccess(@Nullable ResponseBody<String> res) {
                        if (res != null) {
                            callback.onSuccess(true);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }
}
