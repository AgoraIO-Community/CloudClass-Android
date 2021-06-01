package com.hyphenate.easeim.modules.danmaku;


import com.hyphenate.chat.EMMessage;

public class Danmaku {
    public static final String COLOR_GRAY = "#c0c0c0";
    public static final String COLOR_WHITE = "#ffffffff";
    public static final String COLOR_RED = "#ffff0000";
    public static final String COLOR_GREEN = "#ff00ff00";
    public static final String COLOR_BLUE = "#ff0000ff";
    public static final String COLOR_YELLOW = "#ffffff00";
    public static final String COLOR_PURPLE = "#ffff00ff";

    public static final int DEFAULT_TEXT_SIZE = 24;

    public String text;// 文字
    public int size = DEFAULT_TEXT_SIZE;// 字号
    public Mode mode = Mode.scroll;// 模式：滚动、顶部、底部
    public String color = COLOR_RED;
    public String giftUrl = "";
    public String avatarUrl = "";

    public String message;

    public enum Mode {
        scroll, top, bottom
    }

    public Danmaku(String message) {
        this.message = message;
    }

    public Danmaku(String text, int textSize, Mode mode, String color) {
        this.text = text;
        this.size = textSize;
        this.mode = mode;
        this.color = color;
    }

    @Override
    public String toString() {
        return "Danmaku{" +
                "text='" + text + '\'' +
                ", textSize=" + size +
                ", mode=" + mode +
                ", color='" + color + '\'' +
                '}';
    }
}
