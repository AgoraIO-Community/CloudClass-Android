package io.agora.agoraeduuikit.config.component;

/**
 * author : felix
 * date : 2022/7/13
 * description : 学生视频
 */
public class FcrStudentVideoUIConfig {
    public Camera camera = new Camera();
    public Microphone microphone = new Microphone();
    public BoardAuthorization boardAuthorization = new BoardAuthorization();
    public Reward reward = new Reward();
    public OffStage offStage = new OffStage();

    /**
     * 摄像头开关
     */
    public static class Camera extends FcrBaseUIConfig {

    }

    /**
     * 麦克风开关
     */
    public static class Microphone extends FcrBaseUIConfig {

    }

    /**
     * 白板授权
     */
    public static class BoardAuthorization extends FcrBaseUIConfig {

    }

    /**
     * 奖励
     */
    public static class Reward extends FcrBaseUIConfig {

    }

    /**
     * 下台
     */
    public static class OffStage extends FcrBaseUIConfig {

    }
}
