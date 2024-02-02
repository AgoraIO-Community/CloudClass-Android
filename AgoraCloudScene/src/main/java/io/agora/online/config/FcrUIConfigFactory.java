package io.agora.online.config;


import io.agora.agoraeducore.core.internal.framework.proxy.RoomType;
import io.agora.online.config.template.FcrOneToOneUIConfig;
import io.agora.online.config.template.FcrDefUIConfig;
import io.agora.online.config.template.FcrLectureUIConfig;
import io.agora.online.config.template.FcrSmallUIConfig;

/**
 * author : felix
 * date : 2022/7/11
 * description : 模板配置
 */
public class FcrUIConfigFactory {
    /**
     * 获取 UI 配置模板
     *
     * @param roomType
     * @return
     */
    public static FcrUIConfig getConfig(int roomType) {
        FcrUIConfig config = getDefUIConfig();

        if (roomType == RoomType.ONE_ON_ONE.getValue()) {
            config = new FcrOneToOneUIConfig();

        } else if (roomType == RoomType.SMALL_CLASS.getValue()) {
            config = new FcrSmallUIConfig();

        } else if (roomType == RoomType.LARGE_CLASS.getValue()) {
            config = new FcrLectureUIConfig();
        }

        return config;
    }

    public static FcrUIConfig getDefUIConfig() {
        return new FcrDefUIConfig();
    }
}
