package io.agora.online.sdk.helper;

/**
 * author : felix
 * date : 2022/10/29
 * description :
 */
public class FcrStreamParameters {
    /**
     * 小流
     */
    public static class LowStream {
        public static int width = 320;
        public static int height = 240;
        public static int frameRate = 15;
        public static int bitRate = 200;
    }

    /**
     * 大流
     */
    public static class HeightStream {
        public static int width = 640;
        public static int height = 480;
        public static int frameRate = 15;
        public static int bitRate = 600;
    }
}
