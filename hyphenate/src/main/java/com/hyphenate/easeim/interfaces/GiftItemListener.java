package com.hyphenate.easeim.interfaces;

import com.hyphenate.easeim.domain.Gift;

public  interface GiftItemListener {

    /***
     * 点击发送礼物
     * @param gift
     */
    void onGiveGift(Gift gift);
}
