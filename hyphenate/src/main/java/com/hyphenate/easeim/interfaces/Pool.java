package com.hyphenate.easeim.interfaces;

public interface Pool<T> {
    /**
     * 从缓存中获取一个T的实例
     */
    T get();

    /**
     * 释放缓存
     */
    void release();

    /**
     * @return 缓存中T实例的数量
     */
    int count();

    void removeView(String msgId);
}
