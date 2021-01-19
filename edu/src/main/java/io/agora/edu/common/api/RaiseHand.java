package io.agora.edu.common.api;

import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduMsg;

public interface RaiseHand {

    void applyRaiseHand(String toUserUuid, String payload, EduCallback<Boolean> callback);

    void cancelRaiseHand(String toUserUuid, String payload, EduCallback<Boolean> callback);
}
