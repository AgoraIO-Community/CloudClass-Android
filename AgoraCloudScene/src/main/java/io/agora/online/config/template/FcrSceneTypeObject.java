package io.agora.online.config.template;

/**
 * author : wf
 * date : 2022/8/4
 * description :获取Scene的类型（获取教室类型）
 */
public class FcrSceneTypeObject {
    public static FcrSceneType[] getSceneType() {
        return new FcrSceneType[]{FcrSceneType.OneToOne, FcrSceneType.Small, FcrSceneType.Lecture, FcrSceneType.Vocational};
    }

   public enum FcrSceneType {
        OneToOne, Small, Lecture, Vocational
    }
}