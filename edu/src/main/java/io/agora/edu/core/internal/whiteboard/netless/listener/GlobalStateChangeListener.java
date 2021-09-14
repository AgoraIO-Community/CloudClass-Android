package io.agora.edu.core.internal.whiteboard.netless.listener;

import com.herewhite.sdk.domain.GlobalState;

public interface GlobalStateChangeListener {
    void onGlobalStateChanged(GlobalState state);
}
