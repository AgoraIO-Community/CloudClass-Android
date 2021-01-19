package io.agora.edu.common.bean.request;

public class RoomPreCheckReq {
    private String roomName;
    private int roomType;

    public RoomPreCheckReq(String roomName, int roomType) {
        this.roomName = roomName;
        this.roomType = roomType;
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
}
