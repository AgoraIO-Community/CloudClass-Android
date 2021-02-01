package io.agora.edu.classroom;

import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.herewhite.sdk.domain.MemberState;
import com.herewhite.sdk.domain.SceneState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.agora.edu.R;
import io.agora.edu.classroom.bean.channel.Room;
import io.agora.edu.classroom.widget.chat.ChatWindow;
import io.agora.edu.classroom.widget.video.VideoWindow;
import io.agora.edu.classroom.widget.whiteboard.PageControlWindow;
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardWindow;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcChannel;
import io.agora.rte.RteEngineImpl;
import io.agora.rte.data.RteLocalVideoError;
import io.agora.rte.data.RteLocalVideoState;
import io.agora.rte.data.RteRemoteVideoState;

import static io.agora.education.impl.Constants.AgoraLog;

public class AcadsocActivity extends BaseClassActivity_acadsoc implements View.OnClickListener,
        VideoWindow.OnMediaControlListener {
    private static final String TAG = "AscadsocActivity";
    private PageControlWindow pageControlWindow;
    private VideoWindow teacherVideo, studentVideo;
    private RelativeLayout teacherFoldLayout, studentFoldLayout;
    private AppCompatTextView teacherNameText, studentNameText;
    private AppCompatImageView teacherUnfoldIMg, studentUnfoldImg;
    private ChatWindow chatWindow;

    @Override
    protected int getLayoutResId() {
        return R.layout.classroom_window_layout;
    }

    @Override
    protected int getClassType() {
        return Room.Type.ONE2ONE;
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
                                /**设置RTE进行音量的回调*/
                                RteEngineImpl.INSTANCE.enableAudioVolumeIndication(300, 3, false);
                                studentVideo.setUserName(userInfo.getUserName());
                                studentNameText.setText(userInfo.getUserName());
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
        whiteBoardWindow = findViewById(R.id.whiteBoard_Window);
        whiteBoardWindow.setGlobalStateChangeListener(this);
        whiteBoardWindow.setWhiteBoardEventListener(this);
        whiteBoardWindow.setWhiteBoardAppId(agoraEduLaunchConfig.whiteBoardAppId);
        whiteBoardWindow.setInputWhileFollow(true);
        pageControlWindow = findViewById(R.id.pageControl_Window);
        pageControlWindow.setPageControlListener(whiteBoardWindow);
        teacherVideo = findViewById(R.id.teacher_Window);
        teacherVideo.init(false);
        teacherFoldLayout = findViewById(R.id.teacherFold_Layout);
        teacherVideo.setMinimizedView(teacherFoldLayout);
        teacherNameText = findViewById(R.id.teacherName_Text);
        teacherUnfoldIMg = findViewById(R.id.teacherUnfold_Img);
        teacherUnfoldIMg.setOnClickListener(this);
        studentVideo = findViewById(R.id.student_Window);
        studentVideo.init(true);
        studentVideo.setOnMediaControlListener(this);
        studentFoldLayout = findViewById(R.id.studentFold_Layout);
        studentVideo.setMinimizedView(studentFoldLayout);
        studentNameText = findViewById(R.id.studentName_Text);
        studentUnfoldImg = findViewById(R.id.studentUnfold_Img);
        studentUnfoldImg.setOnClickListener(this);
        chatWindow = findViewById(R.id.chat_Window);
    }

    private void renderTeacherStream() {
        getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> streams) {
                if (streams != null) {
                    for (EduStreamInfo streamInfo : streams) {
                        if (streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                            renderStream(getMainEduRoom(), streamInfo, teacherVideo.getVideoContainer());
                            teacherVideo.update(streamInfo.getPublisher().getUserName(),
                                    streamInfo.getHasAudio(), streamInfo.getHasVideo());
                            break;
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
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.teacherUnfold_Img) {
            teacherVideo.restoreMinimize();
        } else if (id == R.id.studentUnfold_Img) {
            studentVideo.restoreMinimize();
        }
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(getMainEduRoom(), streamInfo, studentVideo.videoContainer);
        studentVideo.update(streamInfo.getPublisher().getUserName(), streamInfo.getHasAudio(),
                streamInfo.getHasVideo());
        AgoraLog.e(TAG + ":本地流被添加：" + getLocalCameraStream().getHasAudio() + "," + streamInfo.getHasVideo());
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        studentVideo.update(streamInfo.getPublisher().getUserName(), streamInfo.getHasAudio(),
                streamInfo.getHasVideo());
        AgoraLog.e(TAG + ":本地流被修改：" + streamInfo.getHasAudio() + "," + streamInfo.getHasVideo());
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
        /**一对一场景下，此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(getMainEduRoom(), streamInfo, null);
        studentVideo.update(streamInfo.getPublisher().getUserName(), streamInfo.getHasAudio(),
                streamInfo.getHasVideo());
        AgoraLog.e(TAG + ":本地流被移除");
    }

    @Override
    public void onRemoteVideoStateChanged(@Nullable RtcChannel rtcChannel, int uid, int state, int reason, int elapsed) {
        super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed);
        if (state == RteRemoteVideoState.REMOTE_VIDEO_STATE_DECODING.getValue()) {
            teacherVideo.updateState(VideoWindow.State.Normal);
        } else if (state == RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.getValue()) {
            teacherVideo.updateState(VideoWindow.State.NoCamera);
        }
    }

    @Override
    public void onLocalVideoStateChanged(int localVideoState, int error) {
        super.onLocalVideoStateChanged(localVideoState, error);
        if (localVideoState == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_ENCODING.getValue()) {
            studentVideo.updateState(VideoWindow.State.Normal);
        } else if (localVideoState == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.getValue()) {
            if (error == RteLocalVideoError.LOCAL_VIDEO_STREAM_ERROR_CAPTURE_FAILURE.getValue()) {
                studentVideo.updateState(VideoWindow.State.NoCamera);
            } else {
                studentVideo.updateState(VideoWindow.State.VideoOff);
            }
        }
    }

    @Override
    public void onAudioVolumeIndicationOfLocalSpeaker(@Nullable IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        super.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume);
        studentVideo.updateAudioVolume(totalVolume);
    }

    @Override
    public void onAudioVolumeIndicationOfRemoteSpeaker(@Nullable IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        super.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume);
        teacherVideo.updateAudioVolume(totalVolume);
    }

    @Override
    public void onDisableDeviceInput(boolean disable) {
        super.onDisableDeviceInput(disable);
    }

    @Override
    public void onDisableCameraTransform(boolean disable) {
        super.onDisableCameraTransform(disable);
    }

    @Override
    public void onSceneStateChanged(@Nullable SceneState state) {
        super.onSceneStateChanged(state);
        pageControlWindow.setPageIndex(state.getIndex(), state.getScenes().length);
    }

    @Override
    public void onMemberStateChanged(@Nullable MemberState state) {
        super.onMemberStateChanged(state);
    }

    @Override
    public void onVideo(boolean mute) {
        muteLocalVideo(mute);
    }

    @Override
    public void onAudio(boolean mute) {
        muteLocalAudio(mute);
    }
}
