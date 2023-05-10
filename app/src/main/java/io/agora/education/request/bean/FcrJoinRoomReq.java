package io.agora.education.request.bean;

/**
 * author : hefeng
 * date : 2022/9/13
 * description :
 */
public class FcrJoinRoomReq {
    public String roomId;
    public int role;

    public FcrJoinRoomReq(String roomId, int role) {
        this.roomId = roomId;
        this.role = role;
    }
}
