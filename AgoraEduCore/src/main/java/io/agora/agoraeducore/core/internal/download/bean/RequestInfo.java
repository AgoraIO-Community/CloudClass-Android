package io.agora.agoraeducore.core.internal.download.bean;


import java.io.Serializable;

import io.agora.agoraeducore.core.internal.download.utils.DebugUtils;

public class RequestInfo implements Serializable{

    private int dictate;

    private DownloadInfo downloadInfo;

    public RequestInfo() {
    }

    public int getDictate() {
        return dictate;
    }

    public void setDictate(int dictate) {
        this.dictate = dictate;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }


    @Override
    public String toString() {
        return "RequestInfo{" +
                "dictate=" + DebugUtils.getRequestDictateDesc(dictate) +
                ", downloadInfo=" + downloadInfo +
                '}';
    }
}
