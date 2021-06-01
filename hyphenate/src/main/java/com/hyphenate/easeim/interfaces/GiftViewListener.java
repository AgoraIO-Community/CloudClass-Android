package com.hyphenate.easeim.interfaces;

import com.hyphenate.easeim.domain.Gift;

public interface GiftViewListener {
    /***
     * 发送礼物
     */
    void onGiftSend(Gift gift);

    /***
     * 关闭按钮被点击
     */
    void onCloseGiftView();
}
