package io.agora.education.request.bean;

/**
 * author : felix
 * date : 2022/9/13
 * description :
 */
public class FcrJoinListRoomReq {
    public String nextId;
    public int count = 10;

    public FcrJoinListRoomReq() {
    }

    public FcrJoinListRoomReq(String nextId) {
        this.nextId = nextId;
    }
}
