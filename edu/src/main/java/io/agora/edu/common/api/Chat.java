package io.agora.edu.common.api;

import org.jetbrains.annotations.NotNull;

import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduChatMsg;

public interface Chat {
    void roomChat(@NotNull String fromUuid, @NotNull String message, EduCallback<EduChatMsg> callback);
}
