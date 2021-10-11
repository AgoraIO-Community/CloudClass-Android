package io.agora.edu.core.internal.edu.common.impl;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.agora.edu.core.internal.base.callback.ThrowableCallback;
import io.agora.edu.core.internal.base.network.BusinessException;
import io.agora.edu.core.internal.base.network.RetrofitManager;
import io.agora.edu.core.internal.edu.common.api.Base;
import io.agora.edu.core.internal.edu.common.api.Chat;
import io.agora.edu.core.internal.framework.data.EduChatMessageType;
import io.agora.edu.core.internal.server.struct.request.ChatTranslateReq;
import io.agora.edu.core.internal.server.struct.response.ChatRecordItem;
import io.agora.edu.core.internal.server.struct.response.ChatRecordRes;
import io.agora.edu.core.internal.server.struct.response.ChatTranslateRes;
import io.agora.edu.core.internal.server.struct.response.ConversationRecordItem;
import io.agora.edu.core.internal.server.struct.response.ConversationRecordRes;
import io.agora.edu.core.internal.server.struct.response.ConversationRes;
import io.agora.edu.core.internal.server.struct.response.DataResponseBody;
import io.agora.edu.core.internal.server.struct.response.SendChatRes;
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.ChatService;
import io.agora.edu.core.internal.launch.AgoraEduSDK;
import io.agora.edu.core.internal.framework.data.EduCallback;
import io.agora.edu.core.internal.framework.data.EduError;
import io.agora.edu.core.internal.server.struct.request.EduRoomChatMsgReq;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

public class ChatImpl extends Base implements Chat {
    private static final String TAG = "ChatImpl";

    public ChatImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void roomChat(@NotNull String fromUuid, @NotNull String message, EduCallback<Integer> callback) {
        EduRoomChatMsgReq req = new EduRoomChatMsgReq(message, EduChatMessageType.Text.getValue());
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), ChatService.class)
                .roomChat(appId, roomUuid, fromUuid, req)
                .enqueue(new Callback<SendChatRes>() {
                    @Override
                    @EverythingIsNonNull
                    public void onResponse(Call<SendChatRes> call, Response<SendChatRes> response) {
                        SendChatRes res = response.body();
                        if (res != null && res.getCode() == 0) {
                            callback.onSuccess(res.getData().getMessageId());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }

                    @Override
                    @EverythingIsNonNull
                    public void onFailure(Call<SendChatRes> call, Throwable t) {
                        if(t instanceof BusinessException) {
                            BusinessException e = (BusinessException) t;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(t.getMessage()));
                        }
                    }
                });
    }

    @Override
    public void conversation(@NotNull String userUuid, @NotNull String message, EduCallback<String> callback) {
        EduRoomChatMsgReq req = new EduRoomChatMsgReq(message, EduChatMessageType.Text.getValue());
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), ChatService.class)
                .conversation(appId, roomUuid, userUuid, req)
                .enqueue(new Callback<ConversationRes>() {
                    @Override
                    @EverythingIsNonNull
                    public void onResponse(Call<ConversationRes> call, Response<ConversationRes> response) {
                        ConversationRes res = response.body();
                        if (res != null && res.getCode() == 0) {
                            callback.onSuccess(res.getData().getPeerMessageId());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }

                    @Override
                    @EverythingIsNonNull
                    public void onFailure(Call<ConversationRes> call, Throwable t) {
                        if (t instanceof BusinessException) {
                            BusinessException e = (BusinessException) t;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(t.getMessage()));
                        }
                    }
                });
    }

    @Override
    public void translate(@NotNull ChatTranslateReq req, EduCallback<ChatTranslateRes> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), ChatService.class)
                .translate(appId, req)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<DataResponseBody<ChatTranslateRes>>() {
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
                    public void onSuccess(@Nullable DataResponseBody<ChatTranslateRes> res) {
                        if (res != null && res.getData() != null) {
                            callback.onSuccess(res.getData());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void pullRoomChatRecords(@Nullable String nextId, int count,
                                    boolean reverse, EduCallback<List<ChatRecordItem>> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), ChatService.class)
                .pullChatRecords(appId, roomUuid, count, nextId, reverse ? 0 : 1)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<DataResponseBody<ChatRecordRes>>() {
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
                    public void onSuccess(@Nullable DataResponseBody<ChatRecordRes> res) {
                        if(res != null && res.getData() != null) {
                            callback.onSuccess(res.getData().getList());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }
                }));
    }

    @Override
    public void pullConversationRecords(@Nullable String nextId, String userUuid,
                                        boolean reverse, EduCallback<List<ConversationRecordItem>> callback) {
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), ChatService.class)
                .pullConversationRecords(appId, roomUuid, userUuid, nextId, reverse ? 0 : 1)
                .enqueue(new RetrofitManager.Callback(0,
                        new ThrowableCallback<DataResponseBody<ConversationRecordRes>>() {
                    @Override
                    public void onSuccess(@Nullable DataResponseBody<ConversationRecordRes> res) {
                        if (res != null && res.getData() != null) {
                            callback.onSuccess(res.getData().getList());
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError("response is null"));
                        }
                    }

                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if (throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }
                }));
    }
}