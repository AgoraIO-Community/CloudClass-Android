package io.agora.edu.common.bean.response;

public class EduRemoteConfigRes {
    private int vid;
    private Object theme;
    private NetLessConfig netless;

    public EduRemoteConfigRes(Object theme, NetLessConfig netless) {
        this.theme = theme;
        this.netless = netless;
    }

    public int getVid() {
        return vid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    public Object getTheme() {
        return theme;
    }

    public void setTheme(Object theme) {
        this.theme = theme;
    }

    public NetLessConfig getNetless() {
        return netless;
    }

    public void setNetless(NetLessConfig netless) {
        this.netless = netless;
    }


    public class NetLessConfig {
        private String appId;
        private Object oss;

        public NetLessConfig(String appId, Object oss) {
            this.appId = appId;
            this.oss = oss;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public Object getOss() {
            return oss;
        }

        public void setOss(Object oss) {
            this.oss = oss;
        }
    }
}
