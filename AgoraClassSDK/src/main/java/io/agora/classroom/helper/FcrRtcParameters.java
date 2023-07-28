package io.agora.classroom.helper;

import com.google.gson.annotations.SerializedName;

/**
 * author : felix
 * date : 2022/8/9
 * description :
 */
public class FcrRtcParameters {

    @SerializedName("che.video.lowBitRateStreamParameter")
    public FcrRtcParametersItem parametersItem = new FcrRtcParametersItem();

    public static class FcrRtcParametersItem {
        public int width = 320;
        public int height = 240;
        public int frameRate = 15;
        public int bitRate = 200;
    }
}
