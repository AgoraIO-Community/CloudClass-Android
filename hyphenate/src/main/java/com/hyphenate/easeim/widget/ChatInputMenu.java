package com.hyphenate.easeim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.interfaces.EmojiViewlistener;
import com.hyphenate.easeim.interfaces.ChatInputMenuListener;
import com.hyphenate.easeim.interfaces.ChatPrimaryMenuListener;


public class ChatInputMenu extends LinearLayout implements ChatPrimaryMenuListener, EmojiViewlistener {

    private FrameLayout primaryMenuContainer;
    private FrameLayout emojiconContainer;
    private ChatPrimaryMenu chatPrimaryMenu;
    private EmojiGridView emojiGridView;
    private ChatInputMenuListener chatInputMenuListener;

    public ChatInputMenu(Context context) {
        this(context, null);
    }

    public ChatInputMenu(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatInputMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.chat_input_menu, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        primaryMenuContainer = findViewById(R.id.primary_menu_container);
        emojiconContainer = findViewById(R.id.emoji_page_container);
        init();
    }

    private void init(){
        if(chatPrimaryMenu == null){
            chatPrimaryMenu = new ChatPrimaryMenu(getContext());
            chatPrimaryMenu.setChatPrimaryMenuListener(this);
        }
        primaryMenuContainer.removeAllViews();
        primaryMenuContainer.addView(chatPrimaryMenu);
        if(emojiGridView == null){
            emojiGridView = new EmojiGridView(getContext());
            emojiGridView.setEmojiViewlistener(this);
        }
        emojiconContainer.removeAllViews();
        emojiconContainer.addView(emojiGridView);
        emojiconContainer.setVisibility(GONE);
    }

    public void setChatInputMenuListener(ChatInputMenuListener chatInputMenuListener){
        this.chatInputMenuListener = chatInputMenuListener;
    }

    @Override
    public void onSendBtnClicked(String content) {
        chatInputMenuListener.onSendMessage(content);
    }

    @Override
    public void onFaceViewClicked(boolean state) {
        if(state){
            emojiconContainer.setVisibility(View.GONE);
            chatPrimaryMenu.showNormalFaceImage();
        }else{
            emojiconContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showTextStatus() {
        emojiconContainer.setVisibility(View.GONE);
    }

    @Override
    public void onEmojiItemClick(String emoji) {
        chatPrimaryMenu.onEmojiconInputEvent(emoji);
    }

    /**
     * 重置UI
     */
    public void reset(){
        chatPrimaryMenu.hideSoftKeyboard();
        chatPrimaryMenu.showNormalStatus();
        emojiconContainer.setVisibility(View.GONE);
    }

    /***
     * 输入框获取焦点
     */
    public void etHasFocus(){
        chatPrimaryMenu.showSoftKeyboard();
    }

    public boolean isEmojiViewVisible(){
        return emojiconContainer.getVisibility() == View.VISIBLE;
    }
}
