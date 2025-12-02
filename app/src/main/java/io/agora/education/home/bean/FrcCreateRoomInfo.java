package io.agora.education.home.bean;

/**
 * author : felix
 * date : 2022/9/7
 * description :
 */
public class FrcCreateRoomInfo {
    public int roomTypeRes;
    public int roomDescRes;
    public int roomBgRes;

    public FrcCreateRoomInfo(int roomTypeRes, int roomDescRes, int roomBgRes) {
        this.roomTypeRes = roomTypeRes;
        this.roomDescRes = roomDescRes;
        this.roomBgRes = roomBgRes;
    }
}
