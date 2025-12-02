package io.agora.agoraeduuikit.impl.whiteboard.bean;

import com.google.gson.annotations.SerializedName;
import com.herewhite.sdk.domain.WindowAppParam;

/**
 * author : felix
 * date : 2023/7/6
 * description :
 */
public class MyProjectorAttributes extends WindowAppParam.Attributes {
    @SerializedName("taskId")
    private final String taskUuid;
    @SerializedName("url")
    private final String prefixUrl;

    public MyProjectorAttributes(String taskUuid, String prefixUrl) {
        this.taskUuid = taskUuid;
        this.prefixUrl = prefixUrl;
    }
}