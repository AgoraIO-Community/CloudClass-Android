package io.agora.edu.core.internal.download.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.agora.edu.core.internal.download.DownloadConstant;
import io.agora.edu.core.internal.download.DownloadStatus;
import io.agora.edu.core.internal.download.FileInfo;
import io.agora.edu.core.internal.download.bean.DownloadInfo;
import io.agora.edu.core.internal.download.bean.RequestInfo;
import io.agora.edu.core.internal.download.config.InnerConstant;
import io.agora.edu.core.internal.download.db.DbHolder;
import io.agora.edu.core.internal.download.execute.DownloadExecutor;
import io.agora.edu.core.internal.download.execute.DownloadTask;
import io.agora.edu.core.internal.download.utils.DebugUtils;
import io.agora.edu.core.internal.download.utils.LogUtils;

public class DownloadService extends Service {

    public static final String TAG = "DownloadService";

    public static boolean canRequest = true;

    // some config about ThreadPool
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(3, CPU_COUNT / 2);
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 0L;

    private DownloadExecutor mExecutor = new DownloadExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
            KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingDeque());

    private HashMap<String, DownloadTask> mTasks = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (canRequest) {
            LogUtils.i(TAG, "onStartCommand() -> launch service, intent=" + intent + "\t this=" + this);
            canRequest = false;

            if (null != intent && intent.hasExtra(InnerConstant.Inner.SERVICE_INTENT_EXTRA)) {
                try {
                    ArrayList<RequestInfo> requesetes =
                            (ArrayList<RequestInfo>) intent.getSerializableExtra(InnerConstant.Inner.SERVICE_INTENT_EXTRA);
                    if (null != requesetes && requesetes.size() > 0) {
                        for (RequestInfo request : requesetes) {
                            executeDownload(request);
                        }
                    }
                }
                catch (Exception e) {
                    LogUtils.i(TAG, "onStartCommand()-> Error while reading data from intent");
                    e.printStackTrace();
                }
            }
            canRequest = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    //Todo  Besides the simple synchronized, is there a better way to lock
    private synchronized void executeDownload(RequestInfo requestInfo) {
        DownloadInfo mDownloadInfo = requestInfo.getDownloadInfo();

        //Check whether the task exists in the list
        DownloadTask task = mTasks.get(mDownloadInfo.getUniqueId());
        DbHolder dbHolder = new DbHolder(getBaseContext());
        FileInfo mFileInfo = dbHolder.getFileInfo(mDownloadInfo.getUniqueId());

        LogUtils.i(TAG, "executeDownload() -> task=" + task + "\t mFileInfo=" + mFileInfo);


        /*the task not exists in the list*/
        if (null == task) {
            if (null != mFileInfo) {
                if (mFileInfo.getDownloadStatus() == DownloadStatus.LOADING
                        || mFileInfo.getDownloadStatus() == DownloadStatus.PREPARE) {
                    //modify file state
                    dbHolder.updateState(mFileInfo.getId(), DownloadStatus.PAUSE);
                } else if (mFileInfo.getDownloadStatus() == DownloadStatus.COMPLETE) {
                    if (mDownloadInfo.getFile().exists()) {
                        if (!TextUtils.isEmpty(mDownloadInfo.getAction())) {
                            Intent loadingIntent = new Intent();
                            loadingIntent.setAction(mDownloadInfo.getAction());
                            mFileInfo.setDownloadStatus(DownloadStatus.LOADING);
                            loadingIntent.putExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD, mFileInfo);
                            sendBroadcast(loadingIntent);

                            Intent completeIntent = new Intent();
                            completeIntent.setAction(mDownloadInfo.getAction());
                            mFileInfo.setDownloadStatus(DownloadStatus.COMPLETE);
                            completeIntent.putExtra(DownloadConstant.EXTRA_INTENT_DOWNLOAD, mFileInfo);
                            sendBroadcast(completeIntent);
                        }
                        return;
                    } else {
                        dbHolder.deleteFileInfo(mDownloadInfo.getUniqueId());
                    }
                }
            }//end of "  null != mFileInfo "

            //create download task
            if (requestInfo.getDictate() == InnerConstant.Request.loading) {
                task = new DownloadTask(this, mDownloadInfo, dbHolder);
                mTasks.put(mDownloadInfo.getUniqueId(), task);
            }
        } else {
            if (task.getStatus() == DownloadStatus.COMPLETE || task.getStatus() == DownloadStatus.LOADING
                    || task.getStatus() == DownloadStatus.PAUSE) {
                if (!mDownloadInfo.getFile().exists()) {
                    task.pause();
                    mTasks.remove(mDownloadInfo.getUniqueId());
                    LogUtils.i(TAG, " The file status mark is " + DebugUtils.getStatusDesc(task.getStatus())
                            + ", but the file does not exist. " +
                            "Re download the file  ");
                    executeDownload(requestInfo);
                    return;
                }
            }
        }

        if (null != task) {
            if (requestInfo.getDictate() == InnerConstant.Request.loading) {
                mExecutor.executeTask(task);
            } else {
                task.pause();
            }
        }
    }
}
