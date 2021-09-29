package io.agora.agoraeducore.core.internal.whiteboard.netless.listener;

import com.herewhite.sdk.domain.GlobalState;

public interface GlobalStateChangeListener {
    void onGlobalStateChanged(GlobalState state);
}
