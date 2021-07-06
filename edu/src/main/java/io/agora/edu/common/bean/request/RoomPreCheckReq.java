package io.agora.edu.common.bean.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RoomPreCheckReq {
    private String roomName;
    private int roomType;
    private String role;
    private Long startTime;
    private Long duration;
    private String userName;
    private String boardRegion;
    private Map<String, String> userProperties;

    public RoomPreCheckReq(@NotNull String roomName, int roomType, @NotNull String role,
                           @Nullable Long startTime, @Nullable Long duration, @NotNull String userName,
                           @Nullable String boardRegion, @Nullable Map<String, String> userProperties) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.role = role;
        this.startTime = startTime;
        this.duration = duration;
        this.userName = userName;
        this.boardRegion = boardRegion;
        this.userProperties = userProperties;
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

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getBoardRegion() {
        return boardRegion;
    }

    public void setBoardRegion(String boardRegion) {
        this.boardRegion = boardRegion;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(Map<String, String> userProperties) {
        this.userProperties = userProperties;
    }
}
