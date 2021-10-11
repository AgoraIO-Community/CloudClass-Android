package io.agora.edu.core.internal.edu.common.api;

import io.agora.edu.core.internal.framework.data.EduCallback;

public interface HandsUp {

    void applyHandsUp(EduCallback<Boolean> callback);

    // cancel before teacher handle
    void cancelApplyHandsUp(EduCallback<Boolean> callback);

    void exitHandsUp(EduCallback<Boolean> callback);
}
