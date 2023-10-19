package io.agora.agoraeduuikit.config.component;

/**
 * author : felix
 * date : 2022/7/13
 * description : IM 控件
 */
public class FcrAgoraChatUIConfig extends FcrBaseUIConfig {
    public MuteAll muteAll = new MuteAll();
    public Emoji emoji = new Emoji();
    public Picture picture = new Picture();

    /**
     * IM-全体禁言
     */
    public static class MuteAll extends FcrBaseUIConfig {
    }

    /**
     * IM-emoji表情
     */
    public static class Emoji extends FcrBaseUIConfig {
    }

    /**
     * IM-图片
     */
    public static class Picture extends FcrBaseUIConfig {
    }
}
