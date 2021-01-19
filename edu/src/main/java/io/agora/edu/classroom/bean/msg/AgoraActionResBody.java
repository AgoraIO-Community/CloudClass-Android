package io.agora.edu.classroom.bean.msg;

import com.google.gson.Gson;

public class AgoraActionResBody<T> {
    private int cmd;
    private T data;

    /**
     * 申请邀请消息
     */
    public final static int ApplyInviteActionCMD = 1;

    public AgoraActionResBody(int cmd, T data) {
        this.cmd = cmd;
        this.data = data;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public T getData() {
        return data;
    }

    public String getDataJson() {
        return new Gson().toJson(data);
    }

    public void setData(T data) {
        this.data = data;
    }
}
