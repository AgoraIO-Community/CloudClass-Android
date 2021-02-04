package io.agora.edu.common.bean.request;

public class RoomPreCheckReq {
    private String roomName;
    private int roomType;
    private String role;

    public RoomPreCheckReq(String roomName, int roomType, String role) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.role = role;
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
}
