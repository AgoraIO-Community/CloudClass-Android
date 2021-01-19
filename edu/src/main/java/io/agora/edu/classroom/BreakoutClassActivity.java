package io.agora.edu.classroom;

import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.herewhite.sdk.domain.GlobalState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduFromUserInfo;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomChangeType;
import io.agora.education.api.room.data.EduRoomInfo;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomCreateOptions;
import io.agora.education.api.room.data.RoomType;
import io.agora.education.api.statistics.AgoraError;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduLocalUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserLeftType;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.data.EduUserStateChangeType;
import io.agora.edu.classroom.adapter.ClassVideoAdapter;
import io.agora.edu.common.bean.board.BoardBean;
import io.agora.edu.common.bean.board.BoardInfo;
import io.agora.edu.classroom.bean.channel.Room;
import io.agora.edu.classroom.bean.msg.ChannelMsg;
import io.agora.edu.classroom.fragment.UserListFragment;
import io.agora.edu.common.service.RoomPreService;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.request.AllocateGroupReq;
import io.agora.edu.common.bean.response.EduRoomInfoRes;
import kotlin.Unit;
import io.agora.edu.R2;

import static io.agora.edu.BuildConfig.API_BASE_URL;
import static io.agora.edu.common.bean.board.BoardBean.BOARD;
import static io.agora.education.impl.Constants.AgoraLog;

public class BreakoutClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "BreakoutClassActivity";

    @BindView(R2.id.layout_placeholder)
    protected ConstraintLayout layout_placeholder;
    @BindView(R2.id.rcv_videos)
    protected RecyclerView rcv_videos;
    @BindView(R2.id.layout_im)
    protected View layout_im;
    @BindView(R2.id.layout_tab)
    protected TabLayout layout_tab;

    private ClassVideoAdapter classVideoAdapter;
    private UserListFragment userListFragment;
    private View teacherPlaceholderView;

    private EduRoom subEduRoom;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_breakout_class;
    }

    @Override
    protected void initData() {
        super.initData();
        /**needUserListener为false,将不会收到大班级中的任何local回调*/
        joinRoomAsStudent(getMainEduRoom(), agoraEduLaunchConfig.getUserName(), agoraEduLaunchConfig.getUserUuid(), true, false, true,
                new EduCallback<EduStudent>() {
                    @Override
                    public void onSuccess(@Nullable EduStudent res) {
                        initTitleTimeState();
                        /**处理roomProperties*/
                        Map<String, Object> roomProperties = getMainEduRoom().getRoomProperties();
                        String boardJson = getProperty(roomProperties, BOARD);
                        if (!TextUtils.isEmpty(boardJson)) {
                            AgoraLog.e(TAG + ":大班级的白板信息已存在->" + boardJson);
                            mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
                        }
                        renderTeacherStream();
                        joinSubEduRoom(getMainEduRoom(), agoraEduLaunchConfig.getUserUuid(), agoraEduLaunchConfig.getUserName());
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                        joinFailed(error.getType(), error.getMsg());
                    }
                });
        classVideoAdapter = new ClassVideoAdapter();
    }

    /**
     * @param roomUuid 大班的roomUuid
     */
    private void allocateGroup(String roomUuid, EduCallback<EduRoomInfo> callback) {
        AllocateGroupReq req = new AllocateGroupReq();
        RetrofitManager.instance().getService(API_BASE_URL, RoomPreService.class)
                .allocateGroup(agoraEduLaunchConfig.appId, roomUuid, req)
                .enqueue(new RetrofitManager.Callback<>(0, new ThrowableCallback<ResponseBody<EduRoomInfoRes>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        AgoraLog.e(TAG + ":申请小班信息失败:" + throwable.getMessage());
                        getMainEduRoom().leave(new EduCallback<Unit>() {
                            @Override
                            public void onSuccess(@Nullable Unit res) {
                            }

                            @Override
                            public void onFailure(@NotNull EduError error) {
                            }
                        });
                        joinFailed(AgoraError.INTERNAL_ERROR.getValue(), throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<EduRoomInfoRes> res) {
                        if (res != null && res.data != null) {
                            EduRoomInfo info = res.data;
                            callback.onSuccess(new EduRoomInfo(info.getRoomUuid(), info.getRoomName()));
                        }
                    }
                }));
    }

    /**
     * 根据主教室的信息去请求服务端分配一个小教室
     *
     * @param mainRoom 大教室
     * @param userUuid 学生uuid
     */
    private void joinSubEduRoom(EduRoom mainRoom, String userUuid, String userName) {
        allocateGroup(agoraEduLaunchConfig.getRoomUuid(), new EduCallback<EduRoomInfo>() {
            @Override
            public void onSuccess(@Nullable EduRoomInfo res) {
                if (res != null) {
                    RoomCreateOptions createOptions = new RoomCreateOptions(res.getRoomUuid(),
                            res.getRoomName(), RoomType.BREAKOUT_CLASS.getValue());
                    subEduRoom = buildEduRoom(createOptions, res.getRoomUuid());
                    joinRoomAsStudent(subEduRoom, userName, userUuid, true, true, true, new EduCallback<EduStudent>() {
                        @Override
                        public void onSuccess(@Nullable EduStudent res) {
                            /**设置全局的userToken(注意同一个user在不同的room内，token不一样)*/
                            subEduRoom.getLocalUser(new EduCallback<EduUser>() {
                                @Override
                                public void onSuccess(@Nullable EduUser user) {
                                    if (user != null) {
                                        RetrofitManager.instance().addHeader("token",
                                                user.getUserInfo().getUserToken());
                                    }
                                }

                                @Override
                                public void onFailure(@NotNull EduError error) {
                                }
                            });
                            runOnUiThread(() -> showFragmentWithJoinSuccess());
                            /**判断大班级中的roomProperties中是否有白板信息，如果没有，发起请求,等待RTM通知*/
                            if (mainBoardBean == null) {
                                AgoraLog.e(TAG + ":请求大房间的白板信息");
                                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                                    @Override
                                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                                        requestBoardInfo(((EduLocalUserInfo) userInfo).getUserToken(),
                                                agoraEduLaunchConfig.appId, agoraEduLaunchConfig.getRoomUuid());
                                    }

                                    @Override
                                    public void onFailure(@NotNull EduError error) {
                                    }
                                });
                            } else {
                                BoardInfo info = mainBoardBean.getInfo();
                                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                                    @Override
                                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                                        runOnUiThread(() -> whiteboardFragment.initBoardWithRoomToken(
                                                info.getBoardId(), info.getBoardToken(), userInfo.getUserUuid()));
                                    }

                                    @Override
                                    public void onFailure(@NotNull EduError error) {
                                    }
                                });
                            }
                            subEduRoom.getLocalUser(new EduCallback<EduUser>() {
                                @Override
                                public void onSuccess(@Nullable EduUser user) {
                                    userListFragment.setLocalUserUuid(user.getUserInfo().getUserUuid());
                                }

                                @Override
                                public void onFailure(@NotNull EduError error) {
                                }
                            });
                            notifyStudentList();
                            getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
                                @Override
                                public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                                    showVideoList(streamInfos);
                                }

                                @Override
                                public void onFailure(@NotNull EduError error) {
                                }
                            });
                        }

                        @Override
                        public void onFailure(@NotNull EduError error) {
                            getMainEduRoom().leave(new EduCallback<Unit>() {
                                @Override
                                public void onSuccess(@Nullable Unit res) {
                                }

                                @Override
                                public void onFailure(@NotNull EduError error) {
                                }
                            });
                            joinFailed(error.getType(), error.getMsg());
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                AgoraLog.e(TAG + ":进入小班失败->code:" + error.getType() + ", reason:" + error.getMsg());
            }
        });
    }

    @Override
    protected void initView() {
        super.initView();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rcv_videos.setLayoutManager(layoutManager);
        rcv_videos.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                if (parent.getChildAdapterPosition(view) > 0) {
                    outRect.left = getResources().getDimensionPixelSize(R.dimen.dp_2_5);
                }
            }
        });
        rcv_videos.setAdapter(classVideoAdapter);
        layout_tab.addOnTabSelectedListener(this);
        userListFragment = new UserListFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_chat_room, userListFragment)
                .show(userListFragment)
                .commitNow();
    }

    @Override
    protected int getClassType() {
        return Room.Type.BREAKOUT;
    }

    @Override
    public EduRoom getMyMediaRoom() {
        return subEduRoom;
    }

    @Override
    public void sendRoomChatMsg(String msg, EduCallback<EduChatMsg> callback) {
        subEduRoom.getRoomInfo(new EduCallback<EduRoomInfo>() {
            @Override
            public void onSuccess(@Nullable EduRoomInfo subRoomInfo) {
                /**消息需要添加roomUuid*/
                /**调用super方法把消息发送到大房间中去；但是fromRoomUuid是小房间的-Web端需要*/
                BreakoutClassActivity.super.sendRoomChatMsg(new ChannelMsg.BreakoutChatMsgContent(
                        EduUserRole.STUDENT.getValue(), msg, subRoomInfo.getRoomUuid(),
                        subRoomInfo.getRoomName()).toJsonString(), callback);
                subEduRoom.getLocalUser(new EduCallback<EduUser>() {
                    @Override
                    public void onSuccess(@Nullable EduUser user) {
                        /**把消息发送到小房间去*/
                        user.sendRoomChatMessage(new ChannelMsg.BreakoutChatMsgContent(
                                EduUserRole.STUDENT.getValue(),
                                msg, subRoomInfo.getRoomUuid(), subRoomInfo.getRoomName()).toJsonString(), callback);
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {

                    }
                });
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    @Override
    public void renderStream(EduRoom room, EduStreamInfo eduStreamInfo, @Nullable ViewGroup viewGroup) {
        /**判断发流者的角色:
         * TEACHER->mainEduRoom
         * STUDENT->subEduRoom*/
        EduBaseUserInfo publish = eduStreamInfo.getPublisher();
        if (publish.getRole().equals(EduUserRole.STUDENT)) {
            room = subEduRoom;
        }
        super.renderStream(room, eduStreamInfo, viewGroup);
    }

    /**
     * 获取当前所在 超级小班 的 小班级 中的所有学生的流
     */
    private void getCurAllStudentStream(EduCallback<List<EduStreamInfo>> callback) {
        subEduRoom.getFullStreamList(callback);
    }

    @Override
    protected void getCurFullUser(EduCallback<List<EduUserInfo>> callback) {
        subEduRoom.getFullUserList(new EduCallback<List<EduUserInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduUserInfo> subUsers) {
                List<EduUserInfo> list = new ArrayList<>();
                list.addAll(subUsers);
                callback.onSuccess(list);
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    @Override
    protected void getCurFullStream(EduCallback<List<EduStreamInfo>> callback) {
        getMainEduRoom().getFullStreamList(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> mainStreams) {
                List<EduStreamInfo> list = new ArrayList<>();
                list.addAll(mainStreams);
                if (subEduRoom != null) {
                    subEduRoom.getFullStreamList(new EduCallback<List<EduStreamInfo>>() {
                        @Override
                        public void onSuccess(@Nullable List<EduStreamInfo> subStreams) {
                            list.addAll(subStreams);
                            callback.onSuccess(list);
                        }

                        @Override
                        public void onFailure(@NotNull EduError error) {
                            callback.onFailure(error);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    private void showVideoList(List<EduStreamInfo> list) {
        runOnUiThread(() -> {
            for (int i = 0; i < list.size(); i++) {
                EduStreamInfo streamInfo = list.get(i);
                if (streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                    /*隐藏老师的占位布局*/
                    layout_placeholder.setVisibility(View.GONE);
                    if (i != 0) {
                        Collections.swap(list, 0, i);
                    }
                    classVideoAdapter.setNewList(list);
                    return;
                }
            }
            /*显示老师的占位布局*/
            if (teacherPlaceholderView == null) {
                teacherPlaceholderView = LayoutInflater.from(this).inflate(R.layout.layout_video_small_class,
                        layout_placeholder);
            }
            layout_placeholder.setVisibility(View.VISIBLE);
            classVideoAdapter.setNewList(list);
        });
    }

    private void notifyStudentList() {
        subEduRoom.getFullStreamList(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                userListFragment.setUserList(streamInfos);
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }

    /**
     * 刷新视频列表和学生列表
     */
    private void notifyVideoUserListForLocal(boolean notifyCameraVideo) {
        getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                /**刷新视频列表*/
                if (notifyCameraVideo) {
                    showVideoList(streamInfos);
                }
                userListFragment.updateLocalStream(getLocalCameraStream());
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    private void notifyVideoUserListForRemote(boolean notifyCameraVideo, EduRoom classRoom) {
        getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                /**刷新视频列表*/
                if (notifyCameraVideo) {
                    showVideoList(streamInfos);
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
        if (classRoom.equals(subEduRoom)) {
            notifyStudentList();
        }
    }

    private void renderTeacherStream() {
        getMainEduRoom().getFullStreamList(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streams) {
                if (streams != null) {
                    boolean notify = false;
                    for (EduStreamInfo streamInfo : streams) {
                        EduBaseUserInfo publisher = streamInfo.getPublisher();
                        if (publisher.getRole().equals(EduUserRole.TEACHER)) {
                            switch (streamInfo.getVideoSourceType()) {
                                case CAMERA:
                                    notify = true;
                                    break;
                                case SCREEN:
                                    /*老师打开了屏幕分享，此时把这个流渲染出来*/
                                    runOnUiThread(() -> {
                                        layout_whiteboard.setVisibility(View.GONE);
                                        layout_share_video.setVisibility(View.VISIBLE);
                                        layout_share_video.removeAllViews();
                                        renderStream(getMainEduRoom(), streamInfo, layout_share_video);
                                    });
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    if (notify) {
                        /*此时小组房间可能还没有加入成功，所以只刷新大房间的流*/
                        getMainEduRoom().getFullStreamList(new EduCallback<List<EduStreamInfo>>() {
                            @Override
                            public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                                showVideoList(streamInfos);
                            }

                            @Override
                            public void onFailure(@NotNull EduError error) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }


    @OnClick(R2.id.iv_float)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        view.setSelected(!isSelected);
        layout_im.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (getMyMediaRoom() != null) {
            getMyMediaRoom().leave(new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                    leaveMainEduRoom();
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                    leaveMainEduRoom();
                }
            });
            subEduRoom = null;
        }
        super.onDestroy();
    }

    private void leaveMainEduRoom() {
        if (getMainEduRoom() != null) {
            getMainEduRoom().leave(new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                    AgoraLog.e(TAG + ":leave EduRoom error->code:" + error.getType() + ",reason:" + error.getMsg());
                }
            });
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (tab.getPosition() == 0) {
            transaction.show(chatRoomFragment).hide(userListFragment);
        } else {
            transaction.show(userListFragment).hide(chatRoomFragment);
        }
        transaction.commitNow();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        if (classRoom.equals(subEduRoom)) {
            /**判断大班级中的roomProperties中是否有白板信息，如果没有，发起请求,等待RTM通知*/
            if (mainBoardBean == null) {
                AgoraLog.e(TAG + ":请求大房间的白板信息");
                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                        requestBoardInfo(((EduLocalUserInfo) userInfo).getUserToken(),
                                agoraEduLaunchConfig.appId, agoraEduLaunchConfig.getRoomUuid());
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            } else {
                BoardInfo info = mainBoardBean.getInfo();
                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                        runOnUiThread(() -> whiteboardFragment.initBoardWithRoomToken(
                                info.getBoardId(), info.getBoardToken(), userInfo.getUserUuid()));
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            }
        } else {
            initTitleTimeState();
            /**处理roomProperties*/
            Map<String, Object> roomProperties = classRoom.getRoomProperties();
            String boardJson = getProperty(roomProperties, BOARD);
            if (!TextUtils.isEmpty(boardJson)) {
                AgoraLog.e(TAG + ":大班级的白板信息已存在->" + boardJson);
                mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
            }
        }
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
        if (classRoom.equals(subEduRoom)) {
            notifyStudentList();
        }
    }

    @Override
    public void onRemoteUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduRoom classRoom) {
        super.onRemoteUserLeft(userEvent, classRoom);
        if (classRoom.equals(subEduRoom)) {
            notifyStudentList();
        }
    }

    @Override
    public void onRemoteUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type,
                                    @NotNull EduRoom classRoom) {
        super.onRemoteUserUpdated(userEvent, type, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onRoomMessageReceived(message, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onUserMessageReceived(@NotNull EduMsg message) {
        super.onUserMessageReceived(message);
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        /**收到群聊消息，进行处理并展示*/
        EduFromUserInfo fromUser = eduChatMsg.getFromUser();
        ChannelMsg.ChatMsg chatMsg = new ChannelMsg.ChatMsg(fromUser, eduChatMsg.getMessage(),
                System.currentTimeMillis(), eduChatMsg.getType(), true,
                getRoleStr(fromUser.getRole().getValue()));
        classRoom.getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser user) {
                chatMsg.isMe = fromUser.equals(user.getUserInfo());
                ChannelMsg.BreakoutChatMsgContent msgContent = new Gson().fromJson(chatMsg.getMessage(),
                        ChannelMsg.BreakoutChatMsgContent.class);
                chatMsg.setMessage(msgContent.getContent());
                boolean isTeacherMsgToMain = classRoom.equals(getMainEduRoom()) && fromUser.getRole()
                        .equals(EduUserRole.TEACHER) && TextUtils.isEmpty(msgContent.getFromRoomUuid());
                subEduRoom.getRoomInfo(new EduCallback<EduRoomInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduRoomInfo roomInfo) {
                        boolean isTeacherMsgToSub = classRoom.equals(getMainEduRoom()) && fromUser.getRole()
                                .equals(EduUserRole.TEACHER) && msgContent.getFromRoomUuid().equals(
                                roomInfo.getRoomUuid());
                        boolean isGroupMsg = classRoom.equals(subEduRoom);
                        if (isTeacherMsgToMain || isTeacherMsgToSub || isGroupMsg) {
                            chatRoomFragment.addMessage(chatMsg);
                            AgoraLog.e(TAG + ":成功添加一条聊天消息");
                        }
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {
        super.onUserChatMessageReceived(chatMsg);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams,
                                           @NotNull EduRoom classRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom);
        if (classRoom.equals(subEduRoom)) {
            classRoom.getLocalUser(new EduCallback<EduUser>() {
                @Override
                public void onSuccess(@Nullable EduUser user) {
                    userListFragment.setLocalUserUuid(user.getUserInfo().getUserUuid());
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                }
            });
            notifyStudentList();
            getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
                @Override
                public void onSuccess(@Nullable List<EduStreamInfo> streamInfos) {
                    showVideoList(streamInfos);
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                }
            });
        } else {
            renderTeacherStream();
        }
    }

    @Override
    public void onRemoteStreamsAdded
            (@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        /**老师的屏幕分享流在super方法中处理*/
        super.onRemoteStreamsAdded(streamEvents, classRoom);
        /**处理摄像头流*/
        boolean notify = false;
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    notify = true;
                    break;
                default:
                    break;
            }
        }
        if (notify) {
            AgoraLog.e(TAG + ":有远端Camera流添加，刷新视频列表");
        }
        notifyVideoUserListForRemote(notify, classRoom);
    }

    @Override
    public void onRemoteStreamUpdated(@NotNull List<EduStreamEvent> streamEvents,
                                      @NotNull EduRoom classRoom) {
        /**老师的屏幕分享流在super方法中处理*/
        super.onRemoteStreamUpdated(streamEvents, classRoom);
        /**处理摄像头流*/
        boolean notify = false;
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    notify = true;
                    break;
                default:
                    break;
            }
        }
        if (notify) {
            AgoraLog.e(TAG + ":有远端Camera流被修改，刷新视频列表");
        }
        notifyVideoUserListForRemote(notify, classRoom);
    }

    @Override
    public void onRemoteStreamsRemoved
            (@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        /**老师的屏幕分享流在super方法中处理*/
        super.onRemoteStreamsRemoved(streamEvents, classRoom);
        /**处理摄像头流*/
        boolean notify = false;
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    notify = true;
                    break;
                default:
                    break;
            }
        }
        if (notify) {
            AgoraLog.e(TAG + ":有远端Camera流被移除，刷新视频列表");
        }
        notifyVideoUserListForRemote(notify, classRoom);
    }

    @Override
    public void onRoomStatusChanged(@NotNull EduRoomChangeType event, @NotNull EduUserInfo
            operatorUser, @NotNull EduRoom classRoom) {
        /**不调用父类中的super方法*/
        if (classRoom.equals(getMainEduRoom())) {
            classRoom.getRoomStatus(new EduCallback<EduRoomStatus>() {
                @Override
                public void onSuccess(@Nullable EduRoomStatus roomStatus) {
                    switch (event) {
                        case CourseState:
                            AgoraLog.e(TAG + ":班级:" + agoraEduLaunchConfig.getRoomUuid() + "内的课堂状态->"
                                    + roomStatus.getCourseState());
                            title_view.setTimeState(roomStatus.getCourseState() == EduRoomState.START,
                                    System.currentTimeMillis() - roomStatus.getStartTime());
                            break;
                        case AllStudentsChat:
                            chatRoomFragment.setMuteAll(!roomStatus.isStudentChatAllowed());
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                }
            });
        }
    }

    @Override
    public void onRoomPropertiesChanged(@NotNull EduRoom
                                                classRoom, @Nullable Map<String, Object> cause) {
        if (classRoom.equals(getMainEduRoom())) {
            AgoraLog.e(TAG + ":收到大房间的roomProperty改变的数据");
            Map<String, Object> roomProperties = classRoom.getRoomProperties();
            String boardJson = getProperty(roomProperties, BOARD);
            if (!TextUtils.isEmpty(boardJson) && mainBoardBean == null) {
                AgoraLog.e(TAG + ":首次获取到大房间的白板信息->" + boardJson);
                /**首次获取到白板信息*/
                mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                        runOnUiThread(() -> whiteboardFragment.initBoardWithRoomToken(
                                mainBoardBean.getInfo().getBoardId(),
                                mainBoardBean.getInfo().getBoardToken(), userInfo.getUserUuid()));
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            }
            parseRecordMsg(roomProperties, cause);
        }
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo
            user, @NotNull EduRoom classRoom) {
        if (classRoom.equals(subEduRoom)) {
            title_view.setNetworkQuality(quality);
        }
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull EduRoom
            classRoom) {
        super.onConnectionStateChanged(state, classRoom);
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent
                                           userEvent, @NotNull EduUserStateChangeType type) {
        super.onLocalUserUpdated(userEvent, type);
        notifyVideoUserListForLocal(true);
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        notifyVideoUserListForLocal(true);
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        notifyVideoUserListForLocal(true);
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
        /**此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
        AgoraLog.e(TAG + ":本地流被移除:" + streamEvent.getModifiedStream().getStreamUuid());
    }

    @Override
    public void onGlobalStateChanged(GlobalState state) {
        super.onGlobalStateChanged(state);
    }

    @Override
    public void onLocalUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduUserLeftType leftType) {

    }
}
