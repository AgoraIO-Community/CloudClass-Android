package io.agora.online.config.component;

/**
 * author : felix
 * date : 2022/7/18
 * description :
 */
public class FcrNetlessBoardUIConfig {
    public Mouse mouse = new Mouse();
    public Selector selector = new Selector();
    public Pencil pencil = new Pencil();
    public Text text = new Text();
    public Eraser eraser = new Eraser();
    public Clear clear = new Clear();
    public Save save = new Save();
    public Switch Switch = new Switch();

    /**
     * 鼠标
     */
    public static class Mouse extends FcrBaseUIConfig {
    }

    /**
     * 选择器
     */
    public static class Selector extends FcrBaseUIConfig {
    }

    /**
     * 铅笔
     */
    public static class Pencil extends FcrBaseUIConfig {
    }

    /**
     * 文本框
     */
    public static class Text extends FcrBaseUIConfig {
    }

    /**
     * 橡皮擦
     */
    public static class Eraser extends FcrBaseUIConfig {
    }

    /**
     * 清除
     */
    public static class Clear extends FcrBaseUIConfig {
    }

    /**
     * 保存板书
     */
    public static class Save extends FcrBaseUIConfig {
    }

    /**
     * 保存板书
     */
    public static class Switch extends FcrBaseUIConfig {
    }
}
