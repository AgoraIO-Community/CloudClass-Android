package io.agora.edu.common.bean.response;

import android.os.Parcel;
import android.os.Parcelable;

import io.agora.edu.common.bean.board.BoardInfo;

public class RoomPreCheckRes implements Parcelable {
    /**
     * {@link io.agora.education.api.room.data.EduRoomState}
     */
    private int state;
    private long startTime;
    private long duration;
    private long closeDelay;
    private long lastMessageId;
    /**
     * {@link io.agora.education.api.room.data.EduMuteState}
     */
    private int muteChat;
    private BoardInfo board;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
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

    public long getCloseDelay() {
        return closeDelay;
    }

    public void setCloseDelay(long closeDelay) {
        this.closeDelay = closeDelay;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public int getMuteChat() {
        return muteChat;
    }

    public void setMuteChat(int muteChat) {
        this.muteChat = muteChat;
    }

    public BoardInfo getBoard() {
        return board;
    }

    public void setBoard(BoardInfo board) {
        this.board = board;
    }

    public RoomPreCheckRes(int state, long startTime, long duration, long closeDelay,
                           long lastMessageId, int muteChat, BoardInfo board) {
        this.state = state;
        this.startTime = startTime;
        this.duration = duration;
        this.closeDelay = closeDelay;
        this.lastMessageId = lastMessageId;
        this.muteChat = muteChat;
        this.board = board;
    }

    protected RoomPreCheckRes(Parcel in) {
        state = in.readInt();
        startTime = in.readLong();
        duration = in.readLong();
        closeDelay = in.readLong();
        lastMessageId = in.readLong();
        muteChat = in.readInt();
        board = in.readParcelable(BoardInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(state);
        dest.writeLong(startTime);
        dest.writeLong(duration);
        dest.writeLong(closeDelay);
        dest.writeLong(lastMessageId);
        dest.writeInt(muteChat);
        dest.writeParcelable(board, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RoomPreCheckRes> CREATOR = new Creator<RoomPreCheckRes>() {
        @Override
        public RoomPreCheckRes createFromParcel(Parcel in) {
            return new RoomPreCheckRes(in);
        }

        @Override
        public RoomPreCheckRes[] newArray(int size) {
            return new RoomPreCheckRes[size];
        }
    };
}
