package io.agora.edu.common.api;

import org.jetbrains.annotations.NotNull;

public abstract class Base {
    @NotNull
    protected String appId;
    @NotNull
    protected String roomUuid;

    protected Base(@NotNull String appId, @NotNull String roomUuid) {
        this.appId = appId;
        this.roomUuid = roomUuid;
    }

    @NotNull
    protected String getAppId() {
        return appId;
    }

    protected void setAppId(@NotNull String appId) {
        this.appId = appId;
    }

    @NotNull
    protected String getRoomUuid() {
        return roomUuid;
    }

    protected void setRoomUuid(@NotNull String roomUuid) {
        this.roomUuid = roomUuid;
    }
}
