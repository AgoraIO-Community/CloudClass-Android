package io.agora.agoraeducore.core.internal.edu.common.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.agora.agoraeducore.core.internal.framework.data.EduCallback;
import io.agora.agoraeducore.core.internal.server.struct.request.ChatTranslateReq;
import io.agora.agoraeducore.core.internal.server.struct.response.ChatRecordItem;
import io.agora.agoraeducore.core.internal.server.struct.response.ChatTranslateRes;
import io.agora.agoraeducore.core.internal.server.struct.response.ConversationRecordItem;

public interface Chat {
    void roomChat(@NotNull String fromUuid, @NotNull String message, EduCallback<Integer> callback);

    void conversation(@NotNull String userUuid, @NotNull String message, EduCallback<String> callback);

    void translate(@NotNull ChatTranslateReq req, EduCallback<ChatTranslateRes> callback);

    /**
     * @param nextId 下一次查询起始id，不传则从头开始获取
     * @param reverse 逆向 or 正向*/
    void pullRoomChatRecords(@Nullable String nextId, int count, boolean reverse, EduCallback<List<ChatRecordItem>> callback);

    void pullConversationRecords(@Nullable String nextId, String userUuid, boolean reverse, EduCallback<List<ConversationRecordItem>> callback);
}
