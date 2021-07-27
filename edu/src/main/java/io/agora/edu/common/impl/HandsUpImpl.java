package io.agora.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.BusinessException;
import io.agora.base.network.ResponseBody;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.common.api.Base;
import io.agora.edu.common.api.HandsUp;
import io.agora.edu.common.service.HandsUpService;
import io.agora.edu.launch.AgoraEduSDK;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;

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
