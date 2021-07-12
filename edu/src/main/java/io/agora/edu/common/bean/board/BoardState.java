package io.agora.edu.common.bean.board;

import android.os.Parcel;
import android.os.Parcelable;

import com.herewhite.sdk.domain.GlobalState;

import java.util.ArrayList;
import java.util.List;

public class BoardState extends GlobalState implements Parcelable {
    private float follow;
    private List<String> grantUsers = new ArrayList<>();
    private boolean granted = true;
    private boolean teacherFirstLogin = false;
    private List<BoardDynamicTaskInfo> dynamicTaskUuidList;
    private List<BoardDynamicTaskInfo> materialList;
    private boolean isFullScreen = false;

//    public boolean isGranted(String userUuid) {
//        if(grantUsers!= null && grantUsers.size() > 0) {
//            return grantUsers.contains(userUuid);
//        }
//        return false;
//    }

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

//    public boolean isGranted() {
//        return granted;
//    }

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
}
