package io.agora.download.execute;


import android.content.Intent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.download.DownloadConstant;
import io.agora.download.DownloadStatus;
import io.agora.download.FileInfo;
import io.agora.download.utils.LogUtils;

public class DownloadExecutor extends ThreadPoolExecutor {

    public static final String TAG = "DownloadExecutor";

    public DownloadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                            TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public void executeTask(DownloadTask task) {
        int status = task.getStatus();
        if (status == DownloadStatus.PAUSE || status == DownloadStatus.FAIL) {
            task.setFileStatus(DownloadStatus.WAIT);

            Intent intent = new Intent();
            intent.setAction(task.getDownLoadInfo().getAction());
            intent.putExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD, task.getFileInfo());
            task.sendBroadcast(intent);

            execute(task);
        } else if (status == DownloadStatus.COMPLETE) {
            task.setFileStatus(DownloadStatus.LOADING);
            Intent progressIntent = new Intent();
            progressIntent.setAction(task.getDownLoadInfo().getAction());
            progressIntent.putExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD, task.getFileInfo());
            task.sendBroadcast(progressIntent);

            task.setFileStatus(DownloadStatus.COMPLETE);
            Intent completeIntent = new Intent();
            completeIntent.setAction(task.getDownLoadInfo().getAction());
            completeIntent.putExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD, task.getFileInfo());
            task.sendBroadcast(completeIntent);
        } else {
            LogUtils.w(TAG, "file state is incorrect, not download! FileInfo=" + task.getFileInfo());
        }
    }
}
