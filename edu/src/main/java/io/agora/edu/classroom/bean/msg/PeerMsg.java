package io.agora.edu.classroom.bean.msg;

import androidx.annotation.IntDef;

import com.google.gson.Gson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.agora.base.bean.JsonBean;

public class PeerMsg extends JsonBean {

    @Cmd
    public int cmd;
    public Object data;
    public Object payload;

    public PeerMsg(int cmd, Object data) {
        this.cmd = cmd;
        this.data = data;
    }

    public PeerMsg(int cmd, Object data, Object payload) {
        this.cmd = cmd;
        this.data = data;
        this.payload = payload;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Object getData() {
        return data;
    }

    public String getDataJson() {
        return new Gson().toJson(data);
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getPayload() {
        return payload;
    }

    public String getPayloadJson() {
        return new Gson().toJson(payload);
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public <T> T getMsg(Class<T> tClass) {
        return new Gson().fromJson(new Gson().toJson(data), tClass);
    }

    @Override
    public String toJsonString() {
        return super.toJsonString();
    }

    @IntDef({Cmd.CO_VIDEO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cmd {
        /**
         * co-video operation msg
         */
        int CO_VIDEO = 1;
        /**
         * 申请/邀请消息
         */
        int ApplyInviteActionCMD = 1;
        /**
         * teacher open student`s media
         */
        int UnMutePeerCMD = 10;
    }

    public static class CoVideoMsg {
        @Type
        public int type;
        public String userId;
        public String userName;

        public CoVideoMsg(int type, String userId, String userName) {
            this.type = type;
            this.userId = userId;
            this.userName = userName;
        }

        @IntDef({Type.APPLY, Type.REJECT, Type.CANCEL, Type.ACCEPT, Type.ABORT, Type.EXIT})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
            /**
             * student apply co-video
             */
            int APPLY = 1;
            /**
             * teacher reject apply
             */
            int REJECT = 2;
            /**
             * student cancel apply
             */
            int CANCEL = 3;
            /**
             * teacher accept apply
             */
            int ACCEPT = 4;
            /**
             * teacher abort co-video
             */
            int ABORT = 5;
            /**
             * student exit co-video
             */
            int EXIT = 6;
        }


        @IntDef({Status.DisCoVideo, Status.Applying, Status.CoVideoing})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Status {
            /**
             * 初始状态
             */
            int DisCoVideo = 0;
            /**
             * 申请中
             */
            int Applying = 1;
            /**
             * 连麦中
             */
            int CoVideoing = 2;
        }
    }
}
