package io.agora.download.utils;


import io.agora.download.DownloadStatus;
import io.agora.download.config.InnerConstant;

public class DebugUtils {
    public static String getStatusDesc( int status){
        switch (status){
            case DownloadStatus.WAIT:
                return " wait ";
            case DownloadStatus.PREPARE:
                return " prepare ";
            case DownloadStatus.LOADING:
                return " loading ";
            case DownloadStatus.PAUSE:
                return " pause ";
            case DownloadStatus.COMPLETE:
                return " complete ";
            case DownloadStatus.FAIL:
                return " fail ";
            default:
                return "  UnKnow Status ";
        }
    }

    public static String getRequestDictateDesc( int dictate){
        switch (dictate){
            case InnerConstant.Request.loading:
                return " loading ";
            case InnerConstant.Request.pause:
                return " pause ";
            default:
                return " dictate description error  ";
        }
    }
}
