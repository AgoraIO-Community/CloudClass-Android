package io.agora.edu.classroom;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.herewhite.sdk.domain.GlobalState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.agora.agoraactionprocess.AgoraActionConfigInfo;
import io.agora.agoraactionprocess.AgoraActionListener;
import io.agora.agoraactionprocess.AgoraActionMsgRes;
import io.agora.agoraactionprocess.AgoraActionProcessConfig;
import io.agora.agoraactionprocess.AgoraActionProcessManager;
import io.agora.base.ToastManager;
import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.R;
import io.agora.edu.common.api.Chat;
import io.agora.edu.common.impl.ChatImpl;
import io.agora.edu.widget.EyeProtection;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.logger.DebugItem;
import io.agora.education.api.manager.EduManager;
import io.agora.education.api.manager.listener.EduManagerEventListener;
import io.agora.education.api.message.EduActionMessage;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduChatMsgType;
import io.agora.education.api.message.EduFromUserInfo;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomChangeType;
import io.agora.education.api.room.data.EduRoomInfo;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomCreateOptions;
import io.agora.education.api.room.data.RoomJoinOptions;
import io.agora.education.api.room.data.RoomMediaOptions;
import io.agora.education.api.room.data.RoomType;
import io.agora.education.api.room.listener.EduRoomEventListener;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.LocalStreamInitOptions;
import io.agora.education.api.stream.data.VideoSourceType;
import io.agora.education.api.user.EduAssistant;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.EduTeacher;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduLocalUserInfo;
import io.agora.edu.R2;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.data.EduUserStateChangeType;
import io.agora.education.api.user.listener.EduUserEventListener;
import io.agora.edu.base.BaseActivity;
import io.agora.edu.common.bean.board.BoardBean;
import io.agora.edu.common.bean.board.BoardFollowMode;
import io.agora.edu.common.bean.board.BoardInfo;
import io.agora.edu.common.bean.board.BoardState;
import io.agora.edu.classroom.bean.channel.Room;
import io.agora.edu.classroom.bean.channel.User;
import io.agora.edu.classroom.bean.msg.ChannelMsg;
import io.agora.record.bean.RecordBean;
import io.agora.record.bean.RecordMsg;
import io.agora.edu.classroom.fragment.ChatRoomFragment;
import io.agora.edu.classroom.fragment.WhiteBoardFragment;
import io.agora.edu.classroom.widget.TitleView;
import io.agora.edu.launch.AgoraEduLaunchConfig;
import io.agora.edu.common.service.BoardService;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.util.AppUtil;
import io.agora.edu.widget.ConfirmDialog;
import io.agora.covideo.AgoraCoVideoAction;
import io.agora.whiteboard.netless.listener.GlobalStateChangeListener;
import kotlin.Unit;

import static io.agora.edu.BuildConfig.API_BASE_URL;
import static io.agora.edu.classroom.bean.PropertyCauseType.CMD;
import static io.agora.edu.classroom.bean.PropertyCauseType.RECORDSTATECHANGED;
import static io.agora.edu.common.bean.board.BoardBean.BOARD;
import static io.agora.edu.launch.AgoraEduSDK.CODE;
import static io.agora.edu.launch.AgoraEduSDK.REASON;
import static io.agora.edu.launch.AgoraEduSDK.agoraEduLaunchCallback;
import static io.agora.edu.launch.AgoraEduEvent.AgoraEduEventReady;
import static io.agora.edu.launch.AgoraEduEvent.AgoraEduEventDestroyed;
import static io.agora.education.impl.Constants.AgoraLog;
import static io.agora.record.bean.RecordBean.RECORD;
import static io.agora.record.bean.RecordAction.END;

public abstract class BaseClassActivity extends BaseActivity implements EduRoomEventListener, EduUserEventListener,
        EduManagerEventListener, GlobalStateChangeListener, AgoraActionListener {
    private static final String TAG = BaseClassActivity.class.getSimpleName();

    public static final String LAUNCHCONFIG = "LAUNCHCONFIG";
    public static final String EDUMANAGER = "eduManager";
    public static final int RESULT_CODE = 808;

    @BindView(R2.id.title_view)
    protected TitleView title_view;
    @BindView(R2.id.layout_whiteboard)
    protected FrameLayout layout_whiteboard;
    @BindView(R2.id.layout_share_video)
    protected FrameLayout layout_share_video;

    protected WhiteBoardFragment whiteboardFragment = new WhiteBoardFragment();
    protected ChatRoomFragment chatRoomFragment = new ChatRoomFragment();

    private static EduManager eduManager;
    protected AgoraEduLaunchConfig agoraEduLaunchConfig;
    private volatile boolean isJoining = false, joinSuccess = false;
    private EduRoom mainEduRoom;
    private EduStreamInfo localCameraStream, localScreenStream;
    protected BoardBean mainBoardBean;
    protected RecordBean mainRecordBean;
    protected volatile boolean revRecordMsg = false;
    protected AgoraActionProcessManager actionProcessManager;
    protected List<AgoraActionConfigInfo> actionConfigs = new ArrayList<>();
    private ConfirmDialog audioInviteDialog, videoInviteDialog;
    private Chat chat;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AgoraLog.e(TAG + ":onCreate");
    }

    @Override
    protected void initData() {
        if (eduManager != null) {
            eduManager.setEduManagerEventListener(this);
        }
        agoraEduLaunchConfig = getIntent().getParcelableExtra(LAUNCHCONFIG);
        whiteboardFragment.setWhiteBoardAppId(agoraEduLaunchConfig.whiteBoardAppId);
        chatRoomFragment.setAppId(agoraEduLaunchConfig.appId, agoraEduLaunchConfig.whiteBoardAppId);
        RoomCreateOptions createOptions = new RoomCreateOptions(agoraEduLaunchConfig.getRoomUuid(),
                agoraEduLaunchConfig.getRoomName(), agoraEduLaunchConfig.getRoomType());
        try {
            mainEduRoom = buildEduRoom(createOptions, null);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        chat = new ChatImpl(agoraEduLaunchConfig.appId, agoraEduLaunchConfig.getRoomUuid());
    }

    @Override
    protected void initView() {
        if (getClassType() == RoomType.ONE_ON_ONE.getValue()) {
            whiteboardFragment.setInputWhileFollow(true);
        }
    }

    @Override
    protected void onStart() {
        EyeProtection.setNeedShow(agoraEduLaunchConfig.getEyeCare() == 1);
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        /**尝试主动释放TimeView中的handle*/
        title_view.setTimeState(false, 0);
        /**退出activity之前释放eduRoom资源*/
        mainEduRoom = null;
        if (eduManager != null) {
            eduManager.setEduManagerEventListener(null);
            eduManager.release();
        }
        super.onDestroy();
        /*已销毁的状态回调出去*/
        agoraEduLaunchCallback.onCallback(AgoraEduEventDestroyed);
    }

    @Override
    public void onBackPressed() {
        showLeaveDialog();
    }

    @Room.Type
    protected abstract int getClassType();

    public static void setEduManager(EduManager eduManager) {
        BaseClassActivity.eduManager = eduManager;
    }

    protected void showFragmentWithJoinSuccess() {
        title_view.setTitle(agoraEduLaunchConfig.getRoomName());
        getSupportFragmentManager().beginTransaction()
                .remove(whiteboardFragment)
                .remove(chatRoomFragment)
                .commitNowAllowingStateLoss();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_whiteboard, whiteboardFragment)
                .add(R.id.layout_chat_room, chatRoomFragment)
                .show(whiteboardFragment)
                .show(chatRoomFragment)
                .commitNowAllowingStateLoss();
    }

    /**
     * @param options        创建room对象需要的参数
     * @param parentRoomUuid 父房间的uuid
     */
    protected EduRoom buildEduRoom(RoomCreateOptions options, String parentRoomUuid) throws NullPointerException {
        int roomType = options.getRoomType();
        if (options.getRoomType() == RoomType.BREAKOUT_CLASS.getValue()
                || options.getRoomType() == RoomType.MEDIUM_CLASS.getValue()) {
            roomType = TextUtils.isEmpty(parentRoomUuid) ? RoomType.LARGE_CLASS.getValue() :
                    RoomType.SMALL_CLASS.getValue();
        }
        options = new RoomCreateOptions(options.getRoomUuid(), options.getRoomName(), roomType);
        EduRoom room = null;
        if (eduManager != null) {
            room = eduManager.createClassroom(options);
            room.setEventListener(BaseClassActivity.this);
        } else {
            throw new NullPointerException("eduManager is null");
        }
        return room;
    }

    protected void joinRoomAsStudent(EduRoom eduRoom, String yourNameStr, String yourUuid, boolean autoSubscribe,
                                     boolean autoPublish, boolean needUserListener, EduCallback<EduStudent> callback) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        RoomJoinOptions options = new RoomJoinOptions(yourUuid, yourNameStr, EduUserRole.STUDENT,
                new RoomMediaOptions(autoSubscribe, autoPublish), agoraEduLaunchConfig.getRoomType());
        eduRoom.joinClassroom(options, new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser user) {
                if (user != null) {
//                    /**设置全局的userToken(注意同一个user在不同的room内，token不一样)*/
//                    RetrofitManager.instance().addHeader("token", user.getUserInfo().getUserToken());
                    joinSuccess = true;
                    isJoining = false;
                    if (needUserListener) {
                        user.setEventListener(BaseClassActivity.this);
                    }
                    EduStudent student = (EduStudent) user;
                    callback.onSuccess(student);
                    /*join完成的信息回调出去*/
                    agoraEduLaunchCallback.onCallback(AgoraEduEventReady);
                } else {
                    EduError error = EduError.Companion.internalError("join failed: localUser is null");
                    callback.onFailure(error);
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                isJoining = false;
                callback.onFailure(error);
            }
        });
    }

    protected void joinRoomAsTeacher(EduRoom eduRoom, String yourNameStr, String yourUuid, boolean autoSubscribe,
                                     boolean autoPublish, boolean needUserListener, EduCallback<EduTeacher> callback) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        RoomJoinOptions options = new RoomJoinOptions(yourUuid, yourNameStr, EduUserRole.TEACHER,
                new RoomMediaOptions(autoSubscribe, autoPublish), agoraEduLaunchConfig.getRoomType());
        eduRoom.joinClassroom(options, new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser user) {
                if (user != null) {
//                    /**设置全局的userToken(注意同一个user在不同的room内，token不一样)*/
//                    RetrofitManager.instance().addHeader("token", user.getUserInfo().getUserToken());
                    joinSuccess = true;
                    isJoining = false;
                    if (needUserListener) {
                        user.setEventListener(BaseClassActivity.this);
                    }
                    EduTeacher teacher = (EduTeacher) user;
                    callback.onSuccess(teacher);
                } else {
                    callback.onFailure(EduError.Companion.internalError("join failed: localUser is null"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                isJoining = false;
                callback.onFailure(error);
            }
        });
    }

    protected void joinRoomAsAssistant(EduRoom eduRoom, String yourNameStr, String yourUuid, boolean autoSubscribe,
                                       boolean autoPublish, boolean needUserListener, EduCallback<EduAssistant> callback) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        RoomJoinOptions options = new RoomJoinOptions(yourUuid, yourNameStr, EduUserRole.ASSISTANT,
                new RoomMediaOptions(autoSubscribe, autoPublish), agoraEduLaunchConfig.getRoomType());
        eduRoom.joinClassroom(options, new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser user) {
                if (user != null) {
//                    /**设置全局的userToken(注意同一个user在不同的room内，token不一样)*/
//                    RetrofitManager.instance().addHeader("token", user.getUserInfo().getUserToken());
                    joinSuccess = true;
                    isJoining = false;
                    if (needUserListener) {
                        user.setEventListener(BaseClassActivity.this);
                    }
                    EduAssistant assistant = (EduAssistant) user;
                    callback.onSuccess(assistant);
                } else {
                    callback.onFailure(EduError.Companion.internalError("join failed: localUser is null"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                isJoining = false;
                callback.onFailure(error);
            }
        });
    }

    /**
     * 加入失败，回传数据并结束当前页面
     */
    protected void joinFailed(int code, String reason) {
        String msg = "join classRoom failed->code:" + code + ",reason:" + reason;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        AgoraLog.e(TAG, msg);
        agoraEduLaunchCallback.onCallback(AgoraEduEventDestroyed);
        /*回传错误信息*/
        Intent intent = getIntent().putExtra(CODE, code).putExtra(REASON, reason);
        setResult(RESULT_CODE, intent);
        finish();
    }

    protected void recoveryFragmentWithConfigChanged() {
        if (joinSuccess) {
            showFragmentWithJoinSuccess();
        }
    }

    /**
     * 禁止本地音频
     */
    public final void muteLocalAudio(boolean isMute) {
        if (localCameraStream != null) {
            switchLocalVideoAudio(getMyMediaRoom(), localCameraStream.getHasVideo(), !isMute);
        }
    }

    public final void muteLocalVideo(boolean isMute) {
        if (localCameraStream != null) {
            switchLocalVideoAudio(getMyMediaRoom(), !isMute, localCameraStream.getHasAudio());
        }
    }

    private void switchLocalVideoAudio(EduRoom room, boolean openVideo, boolean openAudio) {
        /**先更新本地流信息和rte状态*/
        if (localCameraStream != null) {
            room.getLocalUser(new EduCallback<EduUser>() {
                @Override
                public void onSuccess(@Nullable EduUser eduUser) {
                    if (eduUser != null) {
                        eduUser.initOrUpdateLocalStream(new LocalStreamInitOptions(localCameraStream.getStreamUuid(),
                                openVideo, openAudio), new EduCallback<EduStreamInfo>() {
                            @Override
                            public void onSuccess(@Nullable EduStreamInfo res) {
                                /**把更新后的流信息同步至服务器*/
                                eduUser.muteStream(res, new EduCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(@Nullable Boolean res) {
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
                    } else {
                    }
                }

                @Override
                public void onFailure(@NotNull EduError error) {

                }
            });
        }
    }

    public EduRoom getMainEduRoom() {
        return mainEduRoom;
    }

    public EduRoom getMyMediaRoom() {
        return mainEduRoom;
    }

    public void getLocalUser(EduCallback<EduUser> callback) {
        if (getMainEduRoom() != null) {
            getMainEduRoom().getLocalUser(callback);
        }
        callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
    }

    public void getLocalUserInfo(EduCallback<EduUserInfo> callback) {
        getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser res) {
                if (res != null) {
                    callback.onSuccess(res.getUserInfo());
                } else {
                    callback.onFailure(EduError.Companion.internalError("current eduUser is null"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });

    }

    public EduStreamInfo getLocalCameraStream() {
        return localCameraStream;
    }

    protected void setLocalCameraStream(EduStreamInfo streamInfo) {
        this.localCameraStream = streamInfo;
    }

    protected void sendRoomChatMsg(String msg, EduCallback<EduChatMsg> callback) {
        getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser res) {
                if (res != null) {
                    res.sendRoomChatMessage(msg, callback);
                } else {
                    callback.onFailure(EduError.Companion.internalError("current eduUser is null"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    public void sendRoomChatMsg(String fromUuid, String msg, EduCallback<EduChatMsg> callback) {
        chat.roomChat(fromUuid, msg, callback);
    }

    protected void getCurFullStream(EduCallback<List<EduStreamInfo>> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getFullStreamList(callback);
        }
    }

    protected void getCurFullUser(EduCallback<List<EduUserInfo>> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getFullUserList(callback);
        }
    }

    protected void getTeacher(EduCallback<EduUserInfo> callback) {
        getCurFullUser(new EduCallback<List<EduUserInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduUserInfo> res) {
                for (EduUserInfo userInfo : res) {
                    if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                        callback.onSuccess(userInfo);
                        return;
                    }
                }
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    protected String getRoleStr(int role) {
        int resId;
        switch (role) {
            case User.Role.TEACHER:
                resId = R.string.teacher;
                break;
            case User.Role.ASSISTANT:
                resId = R.string.assistant;
                break;
            case User.Role.STUDENT:
            default:
                resId = R.string.student;
                break;
        }
        return getString(resId);
    }

    protected void getScreenShareStream(EduCallback<EduStreamInfo> callback) {
        getCurFullStream(new EduCallback<List<EduStreamInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduStreamInfo> res) {
                for (EduStreamInfo stream : res) {
                    if (stream.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                        callback.onSuccess(stream);
                        return;
                    }
                }
                callback.onFailure(EduError.Companion.internalError("there is no screenShare"));
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * 尝试解析录制消息
     */
    protected void parseRecordMsg(Map<String, Object> roomProperties, Map<String, Object> cause) {
        if(cause != null && !cause.isEmpty()) {
            int causeType = (int) Float.parseFloat(cause.get(CMD).toString());
            if(causeType == RECORDSTATECHANGED) {
                String recordJson = getProperty(roomProperties, RECORD);
                if (!TextUtils.isEmpty(recordJson)) {
                    RecordBean tmp = RecordBean.fromJson(recordJson, RecordBean.class);
                    if (mainRecordBean == null || tmp.getState() != mainRecordBean.getState()) {
                        mainRecordBean = tmp;
                        if (mainRecordBean.getState() == END) {
                            getLocalUserInfo(new EduCallback<EduUserInfo>() {
                                @Override
                                public void onSuccess(@Nullable EduUserInfo userInfo) {
                                    getMediaRoomUuid(new EduCallback<String>() {
                                        @Override
                                        public void onSuccess(@Nullable String uuid) {
                                            EduFromUserInfo fromUser = new EduFromUserInfo(userInfo.getUserUuid(),
                                                    userInfo.getUserName(), userInfo.getRole());
                                            RecordMsg recordMsg = new RecordMsg(uuid, fromUser,
                                                    getString(R.string.replay_link), System.currentTimeMillis(),
                                                    EduChatMsgType.Text.getValue());
                                            recordMsg.isMe = true;
                                            chatRoomFragment.addMessage(recordMsg);
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
                    }
                }
            }
        }
    }

    public final void showLeaveDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null && !fragmentManager.isDestroyed()) {
            ConfirmDialog.normal(getString(R.string.confirm_leave_room_content), confirm -> {
                if (confirm) {
                    /**退出白板*/
                    whiteboardFragment.releaseBoard();
                    /**退出教室*/
                    if (getMainEduRoom() != null) {
                        getMainEduRoom().leave(new EduCallback<Unit>() {
                            @Override
                            public void onSuccess(@Nullable Unit res) {
                                BaseClassActivity.super.finish();
                            }

                            @Override
                            public void onFailure(@NotNull EduError error) {
                                AgoraLog.e(TAG + ":leave EduRoom error->code:" + error.getType() + ",reason:" + error.getMsg());
                                BaseClassActivity.super.finish();
                            }
                        });
                    }
                }
            }).show(fragmentManager, null);
        }
    }

    /**课堂结束、被提出时调用*/
    public final void showLeavedDialog(int strId) {
        runOnUiThread(() -> {
            /**退出白板*/
            whiteboardFragment.releaseBoard();
            /**退出教室*/
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
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null && !fragmentManager.isDestroyed()) {
            ConfirmDialog.single(getString(strId), confirm -> {
                if (confirm) {
                    BaseClassActivity.super.finish();
                }
            }).setCancel(false).show(fragmentManager, null);
        }
    }

    /**
     * 显示允许远端打开本地音视频的确认框
     */
    public final void confirmInvite(AgoraCoVideoAction action) {
        ConfirmDialog dialog = null;
        String content = "";
        LocalStreamInitOptions options = new LocalStreamInitOptions("", false, false);
        /**此处对本地流的操作是追加而不是覆盖，所以尝试获取本地流并同步音视频流状态*/
        if (localCameraStream != null) {
            options.setEnableMicrophone(localCameraStream.getHasAudio());
            options.setEnableCamera(localCameraStream.getHasVideo());
        }
        switch (action.getAction()) {
            case 0:
                if (audioInviteDialog != null && audioInviteDialog.isVisible()) {
                    return;
                }
                content = getString(R.string.teacherinviteaudio);
                options.setEnableMicrophone(true);
                dialog = audioInviteDialog = ConfirmDialog.normal(content, confirm -> {
                    if (confirm) {
                        upsertLocalStream(options);
                    }
                });
                break;
            case 1:
                if (videoInviteDialog != null && videoInviteDialog.isVisible()) {
                    return;
                }
                content = getString(R.string.teacherinvitevideo);
                options.setEnableCamera(true);
                dialog = videoInviteDialog = ConfirmDialog.normal(content, confirm -> {
                    if (confirm) {
                        upsertLocalStream(options);
                    }
                });
                break;
        }
        final ConfirmDialog finalDialog = dialog;
        runOnUiThread(() -> {
            CountDownTimer countDownTimer = new CountDownTimer(10500, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    finalDialog.setConfirmText(getString(R.string.confirm)
                            .concat(String.format("(%d)", millisUntilFinished / 1000 + 1)));
                }

                @Override
                public void onFinish() {
                    finalDialog.dismiss();
                }
            };
            countDownTimer.start();
            finalDialog.setConfirmText(getString(R.string.confirm).concat("(10)"));
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null && !fragmentManager.isDestroyed()) {
                finalDialog.show(getSupportFragmentManager(), null);
            }
        });
    }

    /**
     * 新建/更新本地流
     */
    private final void upsertLocalStream(LocalStreamInitOptions options) {
        getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser localUser) {
                if (localUser != null) {
                    options.setStreamUuid(localUser.getUserInfo().getStreamUuid());
                    localUser.initOrUpdateLocalStream(options, new EduCallback<EduStreamInfo>() {
                        @Override
                        public void onSuccess(@Nullable EduStreamInfo stream) {
                            if (stream != null) {
                                localUser.publishStream(stream, new EduCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(@Nullable Boolean res) {
                                    }

                                    @Override
                                    public void onFailure(@NotNull EduError error) {
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(@NotNull EduError error) {
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }

    private final void showLogId(String logId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null && !fragmentManager.isDestroyed()) {
            ConfirmDialog.singleWithButton(getString(R.string.uploadlog_success).concat(logId),
                    getString(R.string.copy1), confirm -> {
                        if (confirm) {
                            AppUtil.copyToClipboard(BaseClassActivity.this, logId);
                        }
                    }).show(fragmentManager, null);
        }
    }

    public final void uploadLog(EduCallback callback) {
        if (eduManager != null) {
            eduManager.uploadDebugItem(DebugItem.LOG, new EduCallback<String>() {
                @Override
                public void onSuccess(@Nullable String res) {
                    callback.onSuccess(res);
                    if (res != null) {
                        showLogId(res);
                    }
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                    callback.onSuccess(error);
                    ToastManager.showShort(String.format(getString(R.string.function_error),
                            error.getType(), error.getMsg()));
                }
            });
        }
    }

    protected void getMediaRoomInfo(EduCallback<EduRoomInfo> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getRoomInfo(callback);
        }
    }

    protected void getMediaRoomStatus(EduCallback<EduRoomStatus> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getRoomStatus(callback);
        }
    }

    protected final void getMediaRoomUuid(EduCallback<String> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getRoomInfo(new EduCallback<EduRoomInfo>() {
                @Override
                public void onSuccess(@Nullable EduRoomInfo res) {
                    callback.onSuccess(res.getRoomUuid());
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                    callback.onFailure(error);
                }
            });
        }
    }

    protected final void getMediaRoomName(EduCallback<String> callback) {
        if (getMyMediaRoom() == null) {
            callback.onFailure(EduError.Companion.internalError("current eduRoom is null"));
        } else {
            getMyMediaRoom().getRoomInfo(new EduCallback<EduRoomInfo>() {
                @Override
                public void onSuccess(@Nullable EduRoomInfo res) {
                    callback.onSuccess(res.getRoomName());
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                    callback.onFailure(error);
                }
            });
        }
    }

    /**
     * 为流(主要是视频流)设置一个渲染区域
     */
    public void renderStream(EduRoom room, EduStreamInfo eduStreamInfo, @Nullable ViewGroup viewGroup) {
        runOnUiThread(() -> getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser eduUser) {
                room.getRoomInfo(new EduCallback<EduRoomInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduRoomInfo res) {
                        eduUser.setStreamView(eduStreamInfo, res.getRoomUuid(), viewGroup);
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        }));
    }

    protected String getProperty(Map<String, Object> properties, String key) {
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                if (property.getKey().equals(key)) {
                    return new Gson().toJson(property.getValue());
                }
            }
        }
        return null;
    }

    /**
     * 当前白板是否开启跟随模式
     */
    protected boolean whiteBoardIsFollowMode(BoardState state) {
        if (state == null) {
            return false;
        }
        return state.getFollow() == BoardFollowMode.FOLLOW;
    }

    /**
     * 当前本地用户是否得到白板授权
     */
    protected void whiteBoardIsGranted(BoardState state, EduCallback<Boolean> callback) {
        getLocalUserInfo(new EduCallback<EduUserInfo>() {
            @Override
            public void onSuccess(@Nullable EduUserInfo userInfo) {
                if (state != null) {
                    if (state.getGrantUsers() != null) {
                        for (String uuid : state.getGrantUsers()) {
                            if (uuid.equals(userInfo.getUserUuid())) {
                                callback.onSuccess(true);
                                return;
                            }
                        }
                        callback.onSuccess(false);
                    } else {
                        callback.onSuccess(false);
                    }
                } else {
                    callback.onFailure(EduError.Companion.internalError("current boardState is null"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    protected void requestBoardInfo(String userToken, String appId, String roomUuid) {
        RetrofitManager.instance().getService(API_BASE_URL, BoardService.class)
                .getBoardInfo(userToken, appId, roomUuid)
                .enqueue(new RetrofitManager.Callback<>(0, new ThrowableCallback<ResponseBody<BoardBean>>() {
                    @Override
                    public void onFailure(@androidx.annotation.Nullable Throwable throwable) {
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<BoardBean> res) {
                    }
                }));
    }

    protected void initTitleTimeState() {
        getMediaRoomStatus(new EduCallback<EduRoomStatus>() {
            @Override
            public void onSuccess(@Nullable EduRoomStatus status) {
                title_view.setTimeState(status.getCourseState() == EduRoomState.START,
                        System.currentTimeMillis() - status.getStartTime());
                chatRoomFragment.setMuteAll(!status.isStudentChatAllowed());
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    protected void initParseBoardInfo(EduRoom classRoom) {
        /**处理roomProperties*/
        Map<String, Object> roomProperties = classRoom.getRoomProperties();
        /**判断roomProperties中是否有白板属性信息，如果没有，发起请求,等待RTM通知*/
        String boardJson = getProperty(roomProperties, BOARD);
        getLocalUserInfo(new EduCallback<EduUserInfo>() {
            @Override
            public void onSuccess(@Nullable EduUserInfo userInfo) {
                if (TextUtils.isEmpty(boardJson) && mainBoardBean == null) {
                    requestBoardInfo(((EduLocalUserInfo) userInfo).getUserToken(), agoraEduLaunchConfig.appId,
                            agoraEduLaunchConfig.getRoomUuid());
                } else {
                    mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
                    BoardInfo info = mainBoardBean.getInfo();
                    AgoraLog.e(TAG + ":白板信息已存在->" + boardJson);
                    runOnUiThread(() -> whiteboardFragment.initBoardWithRoomToken(info.getBoardId(),
                            info.getBoardToken(), userInfo.getUserUuid()));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    /**
     * 务必在joinSuccess成功后调用
     */
    protected void buildActionProcessManager() {
        getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser localUser) {
                if (localUser != null) {
                    EduLocalUserInfo localUserInfo = localUser.getUserInfo();
                    AgoraActionProcessConfig config = new AgoraActionProcessConfig(agoraEduLaunchConfig.appId,
                            agoraEduLaunchConfig.getRoomUuid(), localUserInfo.getUserToken(), API_BASE_URL);
                    actionProcessManager = new AgoraActionProcessManager(config, BaseClassActivity.this);
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    /**
     * 务必在buildActionProcessManager之后调用
     */
    protected void parseAgoraActionConfig(EduRoom room) {
        if (actionProcessManager != null) {
            actionConfigs = actionProcessManager.parseConfigInfo(room.getRoomProperties());
        } else {
            AgoraLog.e(TAG + ":actionProcessManager is null!");
        }
    }

    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        initTitleTimeState();
        initParseBoardInfo(getMainEduRoom());
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到远端用户加入的回调");
    }

    @Override
    public void onRemoteUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到远端用户离开的回调");
    }

    @Override
    public void onRemoteUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type,
                                    @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到远端用户修改的回调");
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {

    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        /**收到群聊消息，进行处理并展示*/
        ChannelMsg.ChatMsg chatMsg = new ChannelMsg.ChatMsg(eduChatMsg.getFromUser(),
                eduChatMsg.getMessage(), eduChatMsg.getTimestamp(), eduChatMsg.getType());
        classRoom.getLocalUser(new EduCallback<EduUser>() {
            @Override
            public void onSuccess(@Nullable EduUser user) {
                chatMsg.isMe = chatMsg.getFromUser().equals(user.getUserInfo());
                chatRoomFragment.addMessage(chatMsg);
                AgoraLog.e(TAG + ":成功添加一条聊天消息");
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":onRemoteStreamsInitialized");
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到添加远端流的回调");
        Iterator<EduStreamEvent> iterator = streamEvents.iterator();
        while (iterator.hasNext()) {
            EduStreamEvent streamEvent = iterator.next();
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                /**老师打开了屏幕分享，此时把这个流渲染出来*/
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(getMainEduRoom(), streamInfo, layout_share_video);
                });
                /**屏幕分享流已处理，移出集合*/
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void onRemoteStreamUpdated(@NotNull List<EduStreamEvent> streamEvents,
                                      @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到修改远端流的回调");
        Iterator<EduStreamEvent> iterator = streamEvents.iterator();
        while (iterator.hasNext()) {
            EduStreamEvent streamEvent = iterator.next();
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(getMainEduRoom(), streamInfo, layout_share_video);
                });
                /**屏幕分享流已处理，移出集合*/
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        AgoraLog.e(TAG + ":收到移除远端流的回调");
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                /**老师关闭了屏幕分享，移除屏幕分享的布局*/
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.VISIBLE);
                    layout_share_video.setVisibility(View.GONE);
                    layout_share_video.removeAllViews();
                    renderStream(getMainEduRoom(), streamInfo, null);
                });
                break;
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull EduRoomChangeType event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom classRoom) {
        classRoom.getRoomStatus(new EduCallback<EduRoomStatus>() {
            @Override
            public void onSuccess(@Nullable EduRoomStatus roomStatus) {
                switch (event) {
                    case CourseState:
                        title_view.setTimeState(roomStatus.getCourseState() == EduRoomState.START,
                                System.currentTimeMillis() - roomStatus.getStartTime());
                        if (roomStatus.getCourseState().equals(EduRoomState.END)) {
                            /*课堂结束，强制退出*/
                            showLeavedDialog(R.string.courseend);
                        }
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

    @Override
    public void onRoomPropertiesChanged(@NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        AgoraLog.e(TAG + ":收到roomProperty改变的数据");
        Map<String, Object> roomProperties = classRoom.getRoomProperties();
        String boardJson = getProperty(roomProperties, BOARD);
        getLocalUserInfo(new EduCallback<EduUserInfo>() {
            @Override
            public void onSuccess(@Nullable EduUserInfo userInfo) {
                if (mainBoardBean == null) {
                    AgoraLog.e(TAG + ":首次获取到白板信息->" + boardJson);
                    /**首次获取到白板信息*/
                    mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
                    runOnUiThread(() -> {
                        whiteboardFragment.initBoardWithRoomToken(mainBoardBean.getInfo().getBoardId(),
                                mainBoardBean.getInfo().getBoardToken(), userInfo.getUserUuid());
                    });
                }
                if(cause != null && !cause.isEmpty()) {
                    int causeType = (int) Float.parseFloat(cause.get(CMD).toString());
                    if(causeType == RECORDSTATECHANGED) {
                        String recordJson = getProperty(roomProperties, RECORD);
                        if (!TextUtils.isEmpty(recordJson)) {
                            RecordBean tmp = RecordBean.fromJson(recordJson, RecordBean.class);
                            if (mainRecordBean == null || tmp.getState() != mainRecordBean.getState()) {
                                mainRecordBean = tmp;
                                if (mainRecordBean.getState() == END) {
                                    getMediaRoomUuid(new EduCallback<String>() {
                                        @Override
                                        public void onSuccess(@Nullable String uuid) {
                                            revRecordMsg = true;
                                            EduFromUserInfo fromUser = new EduFromUserInfo(userInfo.getUserUuid(),
                                                    userInfo.getUserName(), userInfo.getRole());
                                            RecordMsg recordMsg = new RecordMsg(uuid, fromUser,
                                                    getString(R.string.replay_link), System.currentTimeMillis(),
                                                    EduChatMsgType.Text.getValue());
                                            recordMsg.isMe = true;
                                            chatRoomFragment.addMessage(recordMsg);
                                        }

                                        @Override
                                        public void onFailure(@NotNull EduError error) {

                                        }
                                    });
                                }
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
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {
//        AgoraLog.e(TAG + ":onNetworkQualityChanged->" + quality.getValue());
        title_view.setNetworkQuality(quality);
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull EduRoom classRoom) {
        classRoom.getRoomInfo(new EduCallback<EduRoomInfo>() {
            @Override
            public void onSuccess(@Nullable EduRoomInfo roomInfo) {
                AgoraLog.e(TAG + ":onNetworkQualityChanged->" + state.getValue() + ",room:"
                        + roomInfo.getRoomUuid());
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type) {
        /**更新用户信息*/
        EduUserInfo userInfo = userEvent.getModifiedUser();
        chatRoomFragment.setMuteLocal(!userInfo.isChatAllowed());
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        AgoraLog.e(TAG + ":收到添加本地流的回调");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                localCameraStream = streamEvent.getModifiedStream();
                AgoraLog.e(TAG + ":收到添加本地Camera流的回调");
                break;
            case SCREEN:
                localScreenStream = streamEvent.getModifiedStream();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        AgoraLog.e(TAG + ":收到更新本地流的回调");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                localCameraStream = streamEvent.getModifiedStream();
                break;
            case SCREEN:
                localScreenStream = streamEvent.getModifiedStream();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        AgoraLog.e(TAG + ":收到移除本地流的回调");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                localCameraStream = null;
                break;
            case SCREEN:
                localScreenStream = null;
                break;
            default:
                break;
        }
    }

    /**
     * eduManager的回调
     */
    @Override
    public void onUserMessageReceived(@NotNull EduMsg message) {

    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {

    }

    @Override
    public void onUserActionMessageReceived(@NotNull EduActionMessage actionMessage) {

    }

    private boolean followTips = false;
    private boolean curFollowState = false;

    /**
     * 白板的全局回调
     */
    @Override
    public void onGlobalStateChanged(GlobalState state) {
        BoardState boardState = (BoardState) state;
        boolean follow = whiteBoardIsFollowMode(boardState);
        if (followTips) {
            if (curFollowState != follow) {
                curFollowState = follow;
                ToastManager.showShort(follow ? R.string.open_follow_board : R.string.relieve_follow_board);
            }
        } else {
            followTips = true;
            curFollowState = follow;
        }
        whiteboardFragment.disableCameraTransform(follow);
        if (getClassType() == RoomType.ONE_ON_ONE.getValue()) {
            /**一对一，学生始终有输入权限*/
            whiteboardFragment.disableDeviceInputs(false);
            return;
        }
        whiteBoardIsGranted(boardState, new EduCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean granted) {
                whiteboardFragment.disableDeviceInputs(!granted);
            }

            @Override
            public void onFailure(@NotNull EduError error) {

            }
        });
    }


    @Override
    public void onApply(@NotNull AgoraActionMsgRes actionMsgRes) {

    }

    @Override
    public void onInvite(@NotNull AgoraActionMsgRes actionMsgRes) {

    }

    @Override
    public void onAccept(@NotNull AgoraActionMsgRes actionMsgRes) {

    }

    @Override
    public void onReject(@NotNull AgoraActionMsgRes actionMsgRes) {

    }

    @Override
    public void onCancel(@NotNull AgoraActionMsgRes actionMsgRes) {

    }
}
