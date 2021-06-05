package com.hyphenate.easeim.interfaces;


public interface ChatPrimaryMenuListener {
    /**
     * 发送按钮被点击
     * @param content
     */
    void onSendBtnClicked(String content);

    /**
     * 表情图标被点击
     * @param state
     */
    void onFaceViewClicked(boolean state);

    /**
     * 文本输入模式
     */
    void showTextStatus();

}
