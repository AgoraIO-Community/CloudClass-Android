package io.agora.download;


import java.io.Serializable;

import io.agora.download.utils.DebugUtils;

public class FileInfo implements Serializable{
    private String id;   //Unique identifier of the file (url+Storage path)
    private String downloadUrl;
    private String filePath;
    private long size;   //total size
    private long downloadLocation; //The size of the downloaded file relative to the total file size
    private int downloadStatus = DownloadStatus.PAUSE;   //download status

    public FileInfo() {
    }

    public FileInfo(String id, String downloadUrl, String filePath, long size, long downloadLocation, int downloadStatus) {
        this.id = id;
        this.downloadUrl = downloadUrl;
        this.filePath = filePath;
        this.size = size;
        this.downloadLocation = downloadLocation;
        this.downloadStatus = downloadStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDownloadLocation() {
        return downloadLocation;
    }

    public void setDownloadLocation(long downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public FileInfo copy() {
        return new FileInfo(id, downloadUrl, filePath, size, downloadLocation, downloadStatus);
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "id='" + id + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", filePath='" + filePath + '\'' +
                ", size=" + size +
                ", downloadLocation=" + downloadLocation +
                ", downloadStatus=" + DebugUtils.getStatusDesc(downloadStatus) +
                '}';
    }
}
