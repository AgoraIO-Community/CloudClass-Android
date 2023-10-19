package io.agora.education.request.bean;

/**
 * author : felix
 * date : 2022/9/13
 * description :
 */
public class FcrJoinRoomReq {
    public String roomId;
    public int role;
    /**
     * 登录用户，可以不传递
     */
    public String userUuid;

    public FcrJoinRoomReq(String roomId, int role) {
        this.roomId = roomId;
        this.role = role;
    }

    public FcrJoinRoomReq(String roomId, int role, String userUuid) {
        this.roomId = roomId;
        this.role = role;
        this.userUuid = userUuid;
    }
}
