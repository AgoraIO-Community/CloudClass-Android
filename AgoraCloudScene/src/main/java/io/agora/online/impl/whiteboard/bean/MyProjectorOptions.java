package io.agora.online.impl.whiteboard.bean;

import com.herewhite.sdk.domain.WindowAppParam;

/**
 * author : felix
 * date : 2023/7/6
 * description :
 */
public class MyProjectorOptions extends WindowAppParam.Options {
    private final String scenePath;
    
    public MyProjectorOptions(String scenePath, String title) {
        super(title);
        this.scenePath = scenePath;
    }
}