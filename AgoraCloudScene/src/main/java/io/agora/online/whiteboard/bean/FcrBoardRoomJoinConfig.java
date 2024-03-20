package io.agora.online.whiteboard.bean;

import java.util.HashMap;

/**
 * author : felix
 * date : 2022/6/7
 * description :
 */
public class FcrBoardRoomJoinConfig {
    public String roomId;
    public String roomToken;
    public float boardRatio;
    public boolean hasOperationPrivilege;
    public String userId;
    public String userName;
    public HashMap<String, String> collectorStyles;

    @Override
    public String toString() {
        return "FcrBoardRoomJoinConfig{" +
                "roomId='" + roomId + '\'' +
                ", roomToken='" + roomToken + '\'' +
                ", boardRatio=" + boardRatio +
                ", hasOperationPrivilege=" + hasOperationPrivilege +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
