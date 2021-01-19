package io.agora.edu.classroom;

import android.util.Log;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.edu.R2;
import io.agora.edu.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomChangeType;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserLeftType;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.data.EduUserStateChangeType;
import io.agora.edu.classroom.bean.channel.Room;
import io.agora.edu.classroom.widget.RtcVideoView;

import static io.agora.education.impl.Constants.AgoraLog;


public class OneToOneClassActivity extends BaseClassActivity {
    private static final String TAG = OneToOneClassActivity.class.getSimpleName();

    @BindView(R2.id.layout_video_teacher)
    protected RtcVideoView video_teacher;
    @BindView(R2.id.layout_video_student)
    protected RtcVideoView video_student;
    @BindView(R2.id.layout_im)
    protected View layout_im;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_one2one_class;
    }

    @Override
    protected void initData() {
        super.initData();
        joinRoomAsStudent(getMainEduRoom(), agoraEduLaunchConfig.getUserName(), agoraEduLaunchConfig.getUserUuid(), true, true, true,
                new EduCallback<EduStudent>() {
                    @Override
                    public void onSuccess(@org.jetbrains.annotations.Nullable EduStudent res) {
                        runOnUiThread(() -> showFragmentWithJoinSuccess());
                        getLocalUserInfo(new EduCallback<EduUserInfo>() {
                            @Override
                            public void onSuccess(@Nullable EduUserInfo userInfo) {
                                video_student.setName(userInfo.getUserName());
                            }

                            @Override
                            public void onFailure(@NotNull EduError error) {

                            }
                        });
                        initTitleTimeState();
                        initParseBoardInfo(getMainEduRoom());
                        renderTeacherStream();
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                        joinFailed(error.getType(), error.getMsg());
                    }
                });
    }

    @Override
    protected void initView() {
        super.initView();
        video_teacher.init(R.layout.layout_video_one2one_class, false);
        video_student.init(R.layout.layout_video_one2one_class, true);
        video_student.setOnClickAudioListener(v -> OneToOneClassActivity.this.muteLocalAudio(!video_student.isAudioMuted()));
        video_student.setOnClickVideoListener(v -> OneToOneClassActivity.this.muteLocalVideo(!video_student.isVideoMuted()));
    }

    @Override
    protected int getClassType() {
        return Room.Type.ONE2ONE;
    }

    @OnClick(R2.id.iv_float)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        view.setSelected(!isSelected);
        layout_im.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

    private void renderTeacherStream() {
        getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streams) {
                if (streams != null) {
                    for (EduStreamInfo streamInfo : streams) {
                        if (streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                            switch (streamInfo.getVideoSourceType()) {
                                case CAMERA:
                                    renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
                                    video_teacher.update(streamInfo);
                                    break;
                                case SCREEN:
                                    /**有屏幕分享的流进入，说明是老师打开了屏幕分享，此时把这个流渲染出来*/
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
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }

    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersInitialized(users, classRoom);
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
    }

    @Override
    public void onRemoteUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduRoom classRoom) {
        super.onRemoteUserLeft(userEvent, classRoom);
    }

    @Override
    public void onRemoteUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type,
                                    @NotNull EduRoom classRoom) {
        super.onRemoteUserUpdated(userEvent, type, classRoom);

    }

    /**
     * 群聊自定义消息回调
     */
    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onRoomMessageReceived(message, classRoom);
    }

    /**
     * 私聊自定义消息回调
     */
    @Override
    public void onUserMessageReceived(@NotNull EduMsg message) {
        super.onUserMessageReceived(message);
    }

    /**
     * 群聊消息回调
     */
    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, classRoom);
    }

    /**
     * 私聊消息回调
     */
    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {
        super.onUserChatMessageReceived(chatMsg);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams,
                                           @NotNull EduRoom classRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom);
        AgoraLog.e(TAG + ":onRemoteStreamsInitialized");
        renderTeacherStream();
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            /**一对一场景下，远端流就是老师的流*/
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
                    video_teacher.update(streamInfo);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRemoteStreamUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamUpdated(streamEvents, classRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            /**一对一场景下，远端流就是老师的流*/
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
                    video_teacher.update(streamInfo);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom);
        /**一对一场景下，远端流就是老师的流*/
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    renderStream(getMainEduRoom(), streamInfo, null);
                    video_teacher.update(streamInfo);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull EduRoomChangeType event, @NotNull EduUserInfo operatorUser,
                                    @NotNull EduRoom classRoom) {
        super.onRoomStatusChanged(event, operatorUser, classRoom);
    }

    @Override
    public void onRoomPropertiesChanged(@NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        super.onRoomPropertiesChanged(classRoom, cause);
//        runOnUiThread(() -> {
//            /**小班课，默认学生可以针对白板进行输入*/
//            whiteboardFragment.disableCameraTransform(false);
//            whiteboardFragment.disableDeviceInputs(false);
//        });
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user,
                                        @NotNull EduRoom classRoom) {
        super.onNetworkQualityChanged(quality, user, classRoom);
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull EduRoom classRoom) {
        super.onConnectionStateChanged(state, classRoom);
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type) {
        super.onLocalUserUpdated(userEvent, type);
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(getMainEduRoom(), streamInfo, video_student.getVideoLayout());
        video_student.muteMedia(streamInfo);
        AgoraLog.e(TAG + ":本地流被添加：" + getLocalCameraStream().getHasAudio() + "," + streamInfo.getHasVideo());
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        video_student.muteMedia(streamInfo);
        AgoraLog.e(TAG + ":本地流被修改：" + streamInfo.getHasAudio() + "," + streamInfo.getHasVideo());
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
        /**一对一场景下，此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(getMainEduRoom(), streamInfo, null);
        video_student.muteMedia(true, true);
        AgoraLog.e(TAG + ":本地流被移除");
    }

    @Override
    public void onLocalUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduUserLeftType leftType) {
    }
}
