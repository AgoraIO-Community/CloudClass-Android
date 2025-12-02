package io.agora.agoraeduuikit.config.template;

/**
 * author : wf
 * date : 2022/8/4
 * description :获取Scene的类型（获取教室类型）
 */
public class FcrSceneTypeObject {
    public static FcrInnerSceneType[] getSceneType() {
        return new FcrInnerSceneType[]{FcrInnerSceneType.OneToOne, FcrInnerSceneType.Small, FcrInnerSceneType.Lecture,
                FcrInnerSceneType.Vocational,FcrInnerSceneType.CloudClass};
    }

   public enum FcrInnerSceneType {
        OneToOne, Small, Lecture, Vocational,CloudClass
    }
}