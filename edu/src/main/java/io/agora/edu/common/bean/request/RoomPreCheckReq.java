package io.agora.edu.common.bean.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoomPreCheckReq {
    private String roomName;
    private int roomType;
    private String role;
    private long startTime;
    private long duration;
    private String userName;

    public RoomPreCheckReq(@NotNull String roomName, int roomType, @NotNull String role,
                           @Nullable Long startTime, @Nullable Long duration, @NotNull String userName) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.role = role;
        this.startTime = startTime;
        this.duration = duration;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
