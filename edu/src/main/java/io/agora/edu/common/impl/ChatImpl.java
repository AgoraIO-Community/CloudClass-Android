package io.agora.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.BusinessException;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.common.api.Base;
import io.agora.edu.common.api.Chat;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.ChatTranslateReq;
import io.agora.edu.common.bean.response.ChatRecordItem;
import io.agora.edu.common.bean.response.ChatRecordRes;
import io.agora.edu.common.bean.response.ChatTranslateRes;
import io.agora.edu.common.service.ChatService;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduChatMsgType;
import io.agora.education.impl.user.data.request.EduRoomChatMsgReq;

import static io.agora.edu.BuildConfig.API_BASE_URL;

public class ChatImpl extends Base implements Chat {
    private static final String TAG = "ChatImpl";

    public ChatImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void roomChat(@NotNull String fromUuid, @NotNull String message, EduCallback<Boolean> callback) {
        EduRoomChatMsgReq req = new EduRoomChatMsgReq(message, EduChatMsgType.Text.getValue());
        RetrofitManager.instance().getService(API_BASE_URL, ChatService.class)
                .roomChat(appId, roomUuid, fromUuid, req)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<io.agora.base.network.ResponseBody<Boolean>>() {
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
                    public void onSuccess(@Nullable io.agora.base.network.ResponseBody<Boolean> res) {
                        if(res != null) {
                            callback.onSuccess(res.code == 0);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void translate(@NotNull ChatTranslateReq req, EduCallback<ChatTranslateRes> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, ChatService.class)
                .translate(appId, req)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<ChatTranslateRes>>() {
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
                    public void onSuccess(@Nullable ResponseBody<ChatTranslateRes> res) {
                        if(res != null && res.data != null) {
                            callback.onSuccess(res.data);
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void pullRecords(@Nullable String nextId, boolean reverse, EduCallback<List<ChatRecordItem>> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, ChatService.class)
                .pullChatRecords(appId, roomUuid, nextId, reverse ? 0 : 1)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<ChatRecordRes>>() {
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
                    public void onSuccess(@Nullable ResponseBody<ChatRecordRes> res) {
                        if(res != null && res.data != null) {
                            callback.onSuccess(res.data.getList());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }
}
