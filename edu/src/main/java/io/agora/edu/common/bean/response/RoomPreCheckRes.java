package io.agora.edu.common.bean.response;

public class RoomPreCheckRes {
    /**{@link io.agora.education.api.room.data.EduRoomState}*/
    private int state;
    /**{@link io.agora.record.bean.RecordState}*/
    private int recordState;
    /**{@link io.agora.education.api.room.data.EduMuteState}*/
    private int muteChat;

    public RoomPreCheckRes(int state, int recordState, int muteChat) {
        this.state = state;
        this.recordState = recordState;
        this.muteChat = muteChat;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getRecordState() {
        return recordState;
    }

    public void setRecordState(int recordState) {
        this.recordState = recordState;
    }

    public int getMuteChat() {
        return muteChat;
    }

    public void setMuteChat(int muteChat) {
        this.muteChat = muteChat;
    }
}
