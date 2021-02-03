package io.agora.edu.common.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.agora.edu.common.bean.request.ChatTranslateReq;
import io.agora.edu.common.bean.response.ChatRecordItem;
import io.agora.edu.common.bean.response.ChatTranslateRes;
import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduChatMsg;

public interface Chat {
    void roomChat(@NotNull String fromUuid, @NotNull String message, EduCallback<EduChatMsg> callback);

    void translate(@NotNull ChatTranslateReq req, EduCallback<ChatTranslateRes> callback);

    /**
     * @param nextId 下一次查询起始id，不传则从头开始获取
     * @param reverse 逆向 or 正向*/
    void pullRecords(@Nullable String nextId, boolean reverse, EduCallback<List<ChatRecordItem>> callback);
}
