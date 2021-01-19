package io.agora.edu.classroom.fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import io.agora.base.ToastManager;
import io.agora.edu.R;
import io.agora.edu.launch.AgoraEduEvent;
import io.agora.edu.launch.AgoraEduLaunchCallback;
import io.agora.edu.launch.AgoraEduSDK;
import io.agora.edu.launch.AgoraEduReplayConfig;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduChatMsgType;
import io.agora.education.api.message.EduFromUserInfo;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.edu.base.BaseFragment;
import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.adapter.MessageListAdapter;
import io.agora.edu.classroom.bean.msg.ChannelMsg;
import io.agora.record.Replay;
import io.agora.record.ReplayImpl;
import io.agora.record.bean.RecordMsg;
import io.agora.edu.R2;
import io.agora.record.ReplayRes;

public class ChatRoomFragment extends BaseFragment implements OnItemChildClickListener, View.OnKeyListener {
    public static final String TAG = ChatRoomFragment.class.getSimpleName();

    @BindView(R2.id.rcv_msg)
    protected RecyclerView rcv_msg;
    @BindView(R2.id.edit_send_msg)
    protected EditText edit_send_msg;

    private MessageListAdapter adapter;
    private boolean isMuteAll = false;
    private boolean isMuteLocal;
    private String appId, whiteBoardAppId;
    private Replay replay = new ReplayImpl();

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_chatroom;
    }

    @Override
    protected void initData() {
        adapter = new MessageListAdapter();
        adapter.setOnItemChildClickListener(this);
    }

    @Override
    protected void initView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rcv_msg.setLayoutManager(layoutManager);
        rcv_msg.setAdapter(adapter);
        edit_send_msg.setOnKeyListener(this);
        setEditTextEnable(!(this.isMuteAll));
    }

    public void setMuteAll(boolean isMuteAll) {
        this.isMuteAll = isMuteAll;
        setEditTextEnable(!(this.isMuteAll));
    }

    public void setMuteLocal(boolean isMuteLocal) {
        this.isMuteLocal = isMuteLocal;
        setEditTextEnable(!(this.isMuteAll || isMuteLocal));
    }

    private void setEditTextEnable(boolean isEnable) {
        runOnUiThread(() -> {
            if (edit_send_msg != null) {
                edit_send_msg.setEnabled(isEnable);
                if (isEnable) {
                    edit_send_msg.setHint(R.string.hint_im_message);
                } else {
                    edit_send_msg.setHint(R.string.chat_muting);
                }
            }
        });
    }

    public void addMessage(ChannelMsg.ChatMsg chatMsg) {
        runOnUiThread(() -> {
            if (rcv_msg != null) {
                adapter.addData(chatMsg);
                rcv_msg.scrollToPosition(adapter.getItemPosition(chatMsg));
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void setAppId(String appId, String whiteBoardAppId) {
        this.appId = appId;
        this.whiteBoardAppId = whiteBoardAppId;
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (view.getId() == R.id.tv_content) {
            Object object = adapter.getItem(position);
            if (object instanceof RecordMsg) {
                RecordMsg msg = (RecordMsg) object;
                if (context instanceof BaseClassActivity) {
                    replay.allReplayList(appId, msg.getRoomUuid(), 0, new EduCallback<List<ReplayRes.RecordDetail>>() {
                        @Override
                        public void onSuccess(@Nullable List<ReplayRes.RecordDetail> res) {
                            if (res != null && res.size() > 0) {
                                try {
                                    /*find latest record*/
                                    long max = 0;
                                    ReplayRes.RecordDetail recordDetail = null;
                                    for (ReplayRes.RecordDetail detail : res) {
                                        if (detail.startTime > max) {
                                            max = detail.startTime;
                                            recordDetail = detail;
                                        }
                                    }
                                    if (recordDetail.isFinished()) {
                                        String url = recordDetail.url;
                                        if (!TextUtils.isEmpty(url)) {
                                            AgoraEduReplayConfig config = new AgoraEduReplayConfig(
                                                    recordDetail.startTime, recordDetail.endTime, url,
                                                    whiteBoardAppId, recordDetail.boardId,
                                                    recordDetail.boardToken, null);
                                            AgoraEduSDK.replay(requireContext(), config, state -> {
                                            });
                                        }
                                    } else {
                                        ToastManager.showShort(R.string.wait_record);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                ToastManager.showShort(R.string.noreplaydata);
                            }
                        }

                        @Override
                        public void onFailure(@NotNull EduError error) {
                            Log.e(TAG, error.getMsg());
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (!edit_send_msg.isEnabled()) {
            return false;
        }
        String text = edit_send_msg.getText().toString();
        if (KeyEvent.KEYCODE_ENTER == keyCode && KeyEvent.ACTION_DOWN == event.getAction() && text.trim().length() > 0) {
            if (context instanceof BaseClassActivity) {
                edit_send_msg.setText("");
                BaseClassActivity activity = (BaseClassActivity) getActivity();
                activity.getLocalUserInfo(new EduCallback<EduUserInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                        /*本地消息直接添加*/
                        EduFromUserInfo fromUser = new EduFromUserInfo(userInfo.getUserUuid(),
                                userInfo.getUserName(), userInfo.getRole());
                        ChannelMsg.ChatMsg msg = new ChannelMsg.ChatMsg(fromUser, text,
                                System.currentTimeMillis(),
                                EduChatMsgType.Text.getValue());
                        msg.isMe = true;
                        addMessage(msg);
                        activity.sendRoomChatMsg(userInfo.getUserUuid(), text,
                                new EduCallback<EduChatMsg>() {
                                    @Override
                                    public void onSuccess(@Nullable EduChatMsg res) {
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
            return true;
        }
        return false;
    }

}
