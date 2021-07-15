package com.hyphenate.easeim.interfaces;

import android.widget.EditText;


public interface IChatPrimaryMenu {

    /**
     * 文本输入模式
     */
    void showTextStatus();


    /**
     * 输入表情
     * @param emojiContent
     */
    void onEmojiconInputEvent(String emojiContent);

    /**
     * 删除表情
     */
    void onEmojiconDeleteEvent();


    /**
     * 获取EditText
     * @return
     */
    EditText getEditText();



    /**
     * 设置监听
     * @param listener
     */
    void setChatPrimaryMenuListener(ChatPrimaryMenuListener listener);
}
