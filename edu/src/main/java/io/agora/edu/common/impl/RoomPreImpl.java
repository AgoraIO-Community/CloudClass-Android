package io.agora.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.BusinessException;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.common.api.Base;
import io.agora.edu.common.api.RoomPre;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.AllocateGroupReq;
import io.agora.edu.common.bean.request.RoomCreateOptionsReq;
import io.agora.edu.common.bean.request.RoomPreCheckReq;
import io.agora.edu.common.bean.response.EduRemoteConfigRes;
import io.agora.edu.common.bean.response.RoomPreCheckRes;
import io.agora.edu.common.service.RoomPreService;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.room.data.EduRoomInfo;

import static io.agora.edu.BuildConfig.API_BASE_URL;

public class RoomPreImpl extends Base implements RoomPre {
    private static final String TAG = "RoomPreImpl";

    public RoomPreImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void allocateGroup(AllocateGroupReq req, EduCallback<EduRoomInfo> callback) {
    }

    @Override
    public void createClassRoom(RoomCreateOptionsReq roomCreateOptionsReq, EduCallback<String> callback) {

    }

    @Override
    public void preCheckClassRoom(String userUuid, RoomPreCheckReq req, EduCallback<RoomPreCheckRes> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomPreService.class)
                .preCheckClassroom(appId, roomUuid, userUuid, req)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<RoomPreCheckRes>>() {
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
                    public void onSuccess(@Nullable ResponseBody<RoomPreCheckRes> res) {
                        if (res != null) {
                            callback.onSuccess(res.data);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void pullRemoteConfig(EduCallback<EduRemoteConfigRes> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomPreService.class)
                .pullRemoteConfig(appId)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<EduRemoteConfigRes>>() {
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
                    public void onSuccess(@Nullable ResponseBody<EduRemoteConfigRes> res) {
                        if (res != null) {
                            callback.onSuccess(res.data);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }
}
