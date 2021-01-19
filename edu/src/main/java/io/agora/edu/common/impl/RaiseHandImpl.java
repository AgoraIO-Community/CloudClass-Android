package io.agora.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.BusinessException;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.common.api.Base;
import io.agora.edu.common.api.RaiseHand;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.RaiseHandReq;
import io.agora.edu.common.service.RaiseHandService;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduMsg;

import static io.agora.edu.BuildConfig.API_BASE_URL;

public class RaiseHandImpl extends Base implements RaiseHand {
    private static final String TAG = "RaiseHandImpl";

    public RaiseHandImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void applyRaiseHand(@NotNull String toUserUuid, @NotNull String payload,
                               @NotNull EduCallback<Boolean> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, RaiseHandService.class)
                .raiseHand(appId, roomUuid, toUserUuid, new RaiseHandReq(payload))
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<Integer>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if(throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<Integer> res) {
                        if (res != null) {
                            callback.onSuccess(res.data == 1);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void cancelRaiseHand(@NotNull String toUserUuid, @NotNull String payload,
                                @NotNull EduCallback<Boolean> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, RaiseHandService.class)
                .raiseHand(appId, roomUuid, toUserUuid, new RaiseHandReq(payload))
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<Integer>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if(throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<Integer> res) {
                        if (res != null) {
                            callback.onSuccess(res.data == 1);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }
}
