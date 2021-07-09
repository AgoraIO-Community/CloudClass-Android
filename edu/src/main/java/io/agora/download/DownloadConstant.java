package io.agora.download;


public class DownloadConstant {
    /**
     * The download process will send broadcast to transfer the file download progress and other information.
     * eg : FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD);
     */
    public static final String EXTRA_INTENT_DOWNLOAD = "agora_download_extra";
}
