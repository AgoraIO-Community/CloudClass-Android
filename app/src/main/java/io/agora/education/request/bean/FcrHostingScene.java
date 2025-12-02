package io.agora.education.request.bean;

/**
 * author : wf
 * date : 2022/9/15 8:45 下午
 * description :伪直播场景需要的字段
 */
public class FcrHostingScene {
    /**
     * 演示数据
     * https://solutions-apaas.agora.io/cowatch/video/avatar-fte1_h1080p.mov
     * https://solutions-apaas.agora.io/cowatch/video/avatar-fte1_h720p.mov
     * https://solutions-apaas.agora.io/cowatch/video/Avengers_2_trailer_3_51-1080p-HDTN.mp4
     * https://solutions-apaas.agora.io/cowatch/video/Avengers_2_trailer_3_51-720p-HDTN.mp4
     */
    public String videoURL;
    public String reserveVideoURL = "";
    public int finishType = 0;

    public FcrHostingScene(String videoURL) {
        this.videoURL = videoURL;
    }
}
