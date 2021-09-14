package io.agora.edu.core.internal.download.bean;


import java.io.File;
import java.io.Serializable;

public class DownloadInfo implements Serializable{
    private String url;
    private File file;
    /**It is used to receive all kinds of information in download through broadcast*/
    private String action;


    public DownloadInfo(String url, File file, String action) {
        this.url = url;
        this.file = file;
        this.action = action;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }


    @Override
    public String toString() {
        return "DownloadInfo{" +
                "url='" + url + '\'' +
                ", file=" + file +
                ", action='" + action + '\'' +
                '}';
    }

    public String getUniqueId(){
        return url + file.getAbsolutePath();
    }


}
