package io.agora.edu.core.internal.base.network;

public class S3CallbackBody {

    private String appVersion;
    private String deviceName;
    private String deviceVersion;
    private String fileExt;
    private String ossKey;
    private String platform;
    private String serialNumber;
    private Tag tag;
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    public String getAppVersion() {
        return appVersion;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceVersion(String deviceVersion) {
        this.deviceVersion = deviceVersion;
    }
    public String getDeviceVersion() {
        return deviceVersion;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }
    public String getFileExt() {
        return fileExt;
    }

    public void setOssKey(String ossKey) {
        this.ossKey = ossKey;
    }
    public String getOssKey() {
        return ossKey;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
    public String getPlatform() {
        return platform;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
    public Tag getTag() {
        return tag;
    }


    public class Tag {

        private String type;
        public void setType(String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }

    }
}