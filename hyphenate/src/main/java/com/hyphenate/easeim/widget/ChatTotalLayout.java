package com.hyphenate.easeim.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCustomMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.constant.DemoConstant;
import com.hyphenate.easeim.domain.Gift;
import com.hyphenate.easeim.interfaces.ChatInputMenuListener;
import com.hyphenate.easeim.interfaces.GiftViewListener;
import com.hyphenate.easeim.modules.danmaku.Danmaku;
import com.hyphenate.easeim.modules.danmaku.DanmakuCreator;
import com.hyphenate.easeim.modules.danmaku.DanmakuManager;
import com.hyphenate.easeim.modules.danmaku.DanmakuView;
import com.hyphenate.easeim.utils.ScreenUtil;
import com.hyphenate.easeim.utils.SoftInputUtil;
import com.hyphenate.easeim.widget.ChatInputMenu;
import com.hyphenate.easeim.widget.GiftView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatTotalLayout extends RelativeLayout implements View.OnClickListener, View.OnTouchListener, ChatInputMenuListener, GiftViewListener {
    private FrameLayout mContainer;

    private ChatInputMenu chatInputMenu;

    private LinearLayout bottom;

    private DanmakuManager mManager;

    private TextView mDanmakuSend;
    private Context context;
    private GiftView giftView;
    private ImageView gift;

    private String chatRoomId;
    private String nickName;
    private String avatarUrl;
    private String roomUuid;

    private static final int MESSAGE_CODE = 0;
    private static final int TOAST_CODE = 1;
    private static final int ENABLE_CODE = 2;
    private static final int UN_ENABLE_CODE = 3;
    private static final int REMOVE_DANMAKU = 4;

    protected Handler handler;

    //创建handler
    private void initHandler(Context context) {
        handler = new Handler(context.getMainLooper()) {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_CODE) {
                    EMMessage message = (EMMessage) msg.obj;
                    Danmaku danmaku = mDanmakuCreator.create(message);
                    mManager.send(danmaku);
                } else if (msg.what == TOAST_CODE) {
                    String toast = (String) msg.obj;
                    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
                } else if (msg.what == UN_ENABLE_CODE) {
                    mDanmakuSend.setText((String) msg.obj);
                    unEnableBottomView();
                } else if (msg.what == ENABLE_CODE) {
                    mDanmakuSend.setText((String) msg.obj);
                    enableBottomView();
                } else if (msg.what == REMOVE_DANMAKU) {
                    String msgId = (String) msg.obj;
                    DanmakuView view = mManager.getDanmakuView(msgId);
                    if (view != null)
                        mContainer.removeView(view);
                }
            }
        };
    }


    private DanmakuCreator mDanmakuCreator;


    public ChatTotalLayout(Context context) {
        this(context, null);
    }

    public ChatTotalLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatTotalLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.chat_total_layout, this);
        init();
    }

    private void init() {
        initView();
        initDanmaku();
        initHandler(context);
    }

    private void initView() {
        mContainer = findViewById(R.id.danmaku_container);
        bottom = findViewById(R.id.danmaku_bottom);

        chatInputMenu = findViewById(R.id.input_menu);

        mDanmakuSend = findViewById(R.id.etSend);

        giftView = findViewById(R.id.gift_view);
        initData();
        gift = findViewById(R.id.gift);
        initListener();
    }

    private void initData(){
        List<Gift> giftList = new ArrayList<>();
        giftList.add(new Gift("鲜花", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPng00ac5efe8c1d9a682b605523806cba0a7663025682aceda8973fd30f4e9d25aa", "50","呐~这朵花花送给你"));
        giftList.add(new Gift("比心", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPnga261018b5c2d2d1b0fb81d42ea8149f2bed327c2b06afeedcba7d0dd1bc70613", "100","老师好棒，我是你的铁粉"));
        giftList.add(new Gift("鸡腿", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPngb457abd9d2e7f4561a59d22a51db8f6622a9fcec37ed6b8d0d1e239c73da65e6", "200","讲得好，加鸡腿"));
        giftList.add(new Gift("可乐", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPngebbd7053ac2c14d970abc8f73d84d3c24183ef6a6872bf1f64125b43d0dbdfd1", "200","一起干了这杯82年的可乐"));
        giftList.add(new Gift("润喉糖", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPngbb46fbcc43fbbf8e1bd4cbbbf9039c6f145a0238e0db2b5c422d94d4d51c5ffc", "200","老师辛苦了，润润喉"));
        giftList.add(new Gift("血包", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPng8001de704f6f6b801a88bdfa4c36765c1e7b162eee0dbc57d82ecd1ae9385aec", "500","给老师回回血"));
        giftList.add(new Gift("火箭", "https://lanhu.oss-cn-beijing.aliyuncs.com/SketchPnge79dd141528e728de3f138525972396633e0d925aae12eb311f566bc8eb8ee9e", "500","神仙老师，浑身都是优点"));
        giftView.init(giftList, "2000");
    }
    private void initListener() {
        mContainer.setOnTouchListener(this);
        mDanmakuSend.setOnClickListener(this);
        chatInputMenu.setChatInputMenuListener(this);
        giftView.setGiftViewListener(this);
        gift.setOnClickListener(this);
        SoftInputUtil softInputUtil = new SoftInputUtil();
        softInputUtil.attachSoftInput(chatInputMenu, new SoftInputUtil.ISoftInputChanged() {
            @Override
            public void onChanged(boolean isSoftInputShow, int softInputHeight, int viewOffset) {
                if (isSoftInputShow) {
                    if(chatInputMenu.isEmojiViewVisible()){
                        chatInputMenu.onFaceViewClicked(true);
                    }
                    chatInputMenu.setTranslationY(chatInputMenu.getTranslationY() - viewOffset);
                } else {
                    chatInputMenu.setTranslationY(0);
                }
            }
        });
    }


    /**
     * 初始化弹幕参数
     */
    private void initDanmaku() {
        mManager = DanmakuManager.getInstance();
        mManager.init(context, mContainer); // 必须首先调用init方法

        DanmakuManager.Config config = mManager.getConfig(); // 弹幕相关设置
        config.setLineHeight(ScreenUtil.autoSize(60)); // 设置行高

        mDanmakuCreator = new DanmakuCreator();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.etSend) {
            showInputMenu();
        } else if (id == R.id.gift) {
            showGiftView();
        }
    }

    /***
     * 显示礼物View
     */
    private void showGiftView() {
        giftView.setVisibility(View.VISIBLE);
        hideInputBottom();
    }

    /***
     * 隐藏礼物View
     */
    private void hideGiftView() {
        giftView.setVisibility(View.INVISIBLE);
        showInputBottom();
    }

    /***
     * 发送消息
     * @param content
     */
    @Override
    public void onSendMessage(String content) {
        sendTextMessage(content);
        hideInputMenu();
    }

    /***
     * 隐藏底部View
     */
    private void hideInputBottom() {
        bottom.setVisibility(View.INVISIBLE);
        hideInputMenu();
    }

    /***
     * 显示底部View
     */
    private void showInputBottom() {
        bottom.setVisibility(View.VISIBLE);
    }

    /***
     * 隐藏输入框
     */
    private void hideInputMenu() {
        chatInputMenu.setVisibility(View.INVISIBLE);
        chatInputMenu.reset();
    }

    /***
     * 显示输入框
     */
    private void showInputMenu() {
        chatInputMenu.setVisibility(View.VISIBLE);
        chatInputMenu.etHasFocus();
    }

    /***
     * handle发送弹幕
     * @param message
     */
    public void sendHandleMessage(EMMessage message) {
        Message msg = Message.obtain(handler, MESSAGE_CODE, message);
        handler.sendMessage(msg);
    }

    /**
     * handle删除弹幕View
     *
     * @param msgId
     */
    public void sendHandleRemoveDanmaku(String msgId) {
        Message msg = Message.obtain(handler, REMOVE_DANMAKU, msgId);
        handler.sendMessage(msg);
    }

    /**
     * handle设置UI是否可以点击和修改文本提示
     *
     * @param content
     * @param enable
     */
    public void sendHandleEnable(String content, Boolean enable) {
        Message msg = Message.obtain(handler, enable ? ENABLE_CODE : UN_ENABLE_CODE, content);
        handler.sendMessage(msg);
    }

    public void sendHandleToast(String content) {
        Message msg = Message.obtain(handler, TOAST_CODE, content);
        handler.sendMessage(msg);
    }

    /***
     * 发送文本消息
     * @param content
     */
    private void sendTextMessage(String content) {
        EMMessage message = EMMessage.createTxtSendMessage(content, chatRoomId);
        sendMessage(message);
    }

    /***
     * 发送礼物
     * @param gift
     */
    private void sendGiftMessage(Gift gift) {
        EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CUSTOM);
        EMCustomMessageBody body = new EMCustomMessageBody("gift");
        Map<String, String> params = new HashMap<>();
        params.put(DemoConstant.NUMBER, gift.getScore());
        params.put(DemoConstant.DES, gift.getDesc());
        params.put(DemoConstant.URL, gift.getImg());
        body.setParams(params);
        message.addBody(body);
        message.setTo(chatRoomId);
        message.setAttribute(DemoConstant.AVATAR_URL, avatarUrl);
        sendMessage(message);

    }

    /***
     * 发送消息api
     * @param message
     */
    private void sendMessage(EMMessage message) {
        message.setChatType(EMMessage.ChatType.ChatRoom);
        message.setAttribute(DemoConstant.ROOM_UUID, roomUuid);
        message.setAttribute(DemoConstant.MSG_TYPE, DemoConstant.MSG_TYPE_NORMAL);
        message.setAttribute(DemoConstant.ROLE, DemoConstant.ROLE_STUDENT);
        message.setAttribute(DemoConstant.NICK_NAME, nickName);
        message.setAttribute(DemoConstant.AVATAR_URL, avatarUrl);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                sendHandleMessage(message);
            }

            @Override
            public void onError(int code, String error) {
                if (code == EMError.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    Message msg = Message.obtain(handler, TOAST_CODE, context.getResources().getString(R.string.message_incloud_illegal_content));
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    @Override
    public void onGiftSend(Gift gift) {
        sendGiftMessage(gift);
    }

    @Override
    public void onCloseGiftView() {
        hideGiftView();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int id = view.getId();
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (id == R.id.danmaku_container) {
                if (chatInputMenu.getVisibility() == View.VISIBLE) {
                    hideInputMenu();
                }
            }
            return false;
        }
        return false;
    }

    /**
     * 禁言UI
     */
    public void unEnableBottomView() {
        mDanmakuSend.setEnabled(false);
        gift.setEnabled(false);
        hideInputMenu();
    }

    /**
     * 解除禁言UI
     */
    public void enableBottomView() {
        mDanmakuSend.setEnabled(true);
        gift.setEnabled(true);
    }

    public void cancelHandler() {
        handler.removeCallbacksAndMessages(null);
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setRoomUuid(String roomUuid) {
        this.roomUuid = roomUuid;
    }
}
