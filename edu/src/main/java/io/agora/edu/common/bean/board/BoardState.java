package io.agora.edu.common.bean.board;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.herewhite.sdk.domain.GlobalState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.jvm.Synchronized;

public class BoardState extends GlobalState implements Parcelable {
    private float follow;
    private List<String> grantUsers = new ArrayList<>();
    private boolean granted = true;
    private boolean teacherFirstLogin = false;
    private List<BoardDynamicTaskInfo> dynamicTaskUuidList = new ArrayList<>();
    private List<BoardDynamicTaskInfo> materialList = new ArrayList<>();
    private boolean isFullScreen = false;
    private Map<String, Object> flexBoardState = new HashMap<>();
    private Map<String, ExtAppMovement> extAppMoveTracks = new HashMap<>();

    public BoardState() {

    }

    public BoardState(float follow, List<String> grantUsers, boolean granted, boolean teacherFirstLogin, List<BoardDynamicTaskInfo> dynamicTaskUuidList, List<BoardDynamicTaskInfo> materialList, boolean isFullScreen) {
        this.follow = follow;
        this.grantUsers = grantUsers;
        this.granted = granted;
        this.teacherFirstLogin = teacherFirstLogin;
        this.dynamicTaskUuidList = dynamicTaskUuidList;
        this.materialList = materialList;
        this.isFullScreen = isFullScreen;
    }

    public BoardState(float follow, boolean granted, boolean teacherFirstLogin, List<BoardDynamicTaskInfo> dynamicTaskUuidList, List<BoardDynamicTaskInfo> materialList, boolean isFullScreen) {
        this.follow = follow;
        this.granted = granted;
        this.teacherFirstLogin = teacherFirstLogin;
        this.dynamicTaskUuidList = dynamicTaskUuidList;
        this.materialList = materialList;
        this.isFullScreen = isFullScreen;
    }

    protected BoardState(Parcel in) {
        follow = in.readFloat();
        grantUsers = in.createStringArrayList();
        granted = in.readByte() != 0;
        teacherFirstLogin = in.readByte() != 0;
        dynamicTaskUuidList = in.createTypedArrayList(BoardDynamicTaskInfo.CREATOR);
        materialList = in.createTypedArrayList(BoardDynamicTaskInfo.CREATOR);
        isFullScreen = in.readByte() != 0;
        in.readMap(flexBoardState, ClassLoader.getSystemClassLoader());
        in.readMap(extAppMoveTracks, ClassLoader.getSystemClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(follow);
        dest.writeStringList(grantUsers);
        dest.writeByte((byte) (granted ? 1 : 0));
        dest.writeByte((byte) (teacherFirstLogin ? 1 : 0));
        dest.writeTypedList(dynamicTaskUuidList);
        dest.writeTypedList(materialList);
        dest.writeByte((byte) (isFullScreen ? 1 : 0));
        dest.writeMap(flexBoardState != null
                ? flexBoardState
                : new HashMap<String, Object>());
        dest.writeMap(extAppMoveTracks != null
                ? extAppMoveTracks
                : new HashMap<String, ExtAppMovement>());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BoardState> CREATOR = new Creator<BoardState>() {
        @Override
        public BoardState createFromParcel(Parcel in) {
            return new BoardState(in);
        }

        @Override
        public BoardState[] newArray(int size) {
            return new BoardState[size];
        }
    };

    public boolean isFollow() {
        return follow == 1.0;
    }

    public void setFollow(float follow) {
        this.follow = follow;
    }

    public List<String> getGrantUsers() {
        return grantUsers;
    }

    public void setGrantUsers(List<String> grantUsers) {
        this.grantUsers = grantUsers;
    }

    public boolean isGranted(String userUuid) {
        return grantUsers.contains(userUuid);
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public boolean isTeacherFirstLogin() {
        return teacherFirstLogin;
    }

    public void setTeacherFirstLogin(boolean teacherFirstLogin) {
        this.teacherFirstLogin = teacherFirstLogin;
    }

    public List<BoardDynamicTaskInfo> getDynamicTaskUuidList() {
        return dynamicTaskUuidList;
    }

    public void setDynamicTaskUuidList(List<BoardDynamicTaskInfo> dynamicTaskUuidList) {
        this.dynamicTaskUuidList = dynamicTaskUuidList;
    }

    public List<BoardDynamicTaskInfo> getMaterialList() {
        return materialList;
    }

    public void setMaterialList(List<BoardDynamicTaskInfo> materialList) {
        this.materialList = materialList;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        this.isFullScreen = fullScreen;
    }

    public void setFlexBoardState(@NonNull Map<String, Object> properties) {
        flexBoardState = properties;
    }

    public @NonNull Map<String, Object> getFlexBoardState() {
        return flexBoardState;
    }

    public boolean userDefinedPropertyEquals(BoardState another) {
        if (another != null) {
            return this.flexBoardState.equals(another.flexBoardState);
        } else {
            return this.flexBoardState.isEmpty();
        }
    }

    public BoardState copy() {
        BoardState state = new BoardState(follow, grantUsers, granted,
                teacherFirstLogin, dynamicTaskUuidList, materialList, isFullScreen);
        state.flexBoardState = flexBoardState;
        return state;
    }

    public static class ExtAppMovement {
        public String userId;
        public float x;
        public float y;

        public ExtAppMovement(String userId, float x, float y) {
            this.userId = userId;
            this.x = x;
            this.y = y;
        }

        public boolean equals(ExtAppMovement another) {
            if (another == null) return false;
            if (this.userId == null && another.userId != null) {
                return false;
            } else if (this.userId != null) {
                return this.userId.equals(another.userId) &&
                        this.x == another.x && this.y == another.y;
            } else {
                return false;
            }
        }
    }

    @Synchronized
    public Map<String, ExtAppMovement> getExtAppMovements() {
        return extAppMoveTracks;
    }

    @Synchronized
    public void setExtAppMovement(String id, String userId, float x, float y) {
        if (extAppMoveTracks.containsKey(id) && extAppMoveTracks.get(id) != null) {
            ExtAppMovement m = extAppMoveTracks.get(id);
            m.userId = userId;
            m.x = x;
            m.y = y;
        } else {
            extAppMoveTracks.put(id, new ExtAppMovement(userId, x, y));
        }
    }

    public boolean extAppTracksEquals(BoardState another) {
        if (another != null) {
            return this.extAppMoveTracks.equals(another.extAppMoveTracks);
        } else {
            return this.extAppMoveTracks.isEmpty();
        }
    }
}
