package io.agora.edu.core.internal.download;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;

import io.agora.edu.core.internal.download.bean.DownloadInfo;
import io.agora.edu.core.internal.download.bean.RequestInfo;
import io.agora.edu.core.internal.download.config.InnerConstant;
import io.agora.edu.core.internal.download.service.DownloadService;
import io.agora.edu.core.internal.download.utils.LogUtils;

public class DownloadHelper {

    public static final String TAG = "DownloadHelper";

    private volatile static DownloadHelper SINGLETANCE;

    private static ArrayList<RequestInfo> requests = new ArrayList<>();

    private DownloadHelper() {
    }

    public static DownloadHelper getInstance() {
        if (SINGLETANCE == null) {
            synchronized (DownloadHelper.class) {
                if (SINGLETANCE == null) {
                    SINGLETANCE = new DownloadHelper();
                }
            }
        }
        return SINGLETANCE;
    }

    /**
     * Submit (download / pause) task（Submitting means executing）
     *
     * @param context
     */
    public synchronized void submit(Context context) {
        if (requests.isEmpty()) {
            LogUtils.w("There are no download tasks to perform");
            return;
        }
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(InnerConstant.Inner.SERVICE_INTENT_EXTRA, requests);
        context.startService(intent);
        requests.clear();
    }// end of "submit(..."

    /**
     * @param action broadcast`s action
     */
    public DownloadHelper addTask(String url, File file, String action) {
        RequestInfo requestInfo = createRequest(url, file, action, InnerConstant.Request.loading);
        LogUtils.i(TAG, "addTask() requestInfo=" + requestInfo);

        requests.add(requestInfo);
        return this;
    }

    /**
     * @param action broadcast`s action
     */
    public DownloadHelper pauseTask(String url, File file, String action) {
        RequestInfo requestInfo = createRequest(url, file, action, InnerConstant.Request.pause);
        LogUtils.i(TAG, "pauseTask() -> requestInfo=" + requestInfo);
        requests.add(requestInfo);
        return this;
    }

    /**
     * Todo    To reconstruct the log module, our static inner class is not yet effective
     */
    private DownloadHelper setDebug(boolean isDebug) {
        LogUtils.setDebug(isDebug);
        return this;
    }


    private RequestInfo createRequest(String url, File file, String action, int dictate) {
        RequestInfo request = new RequestInfo();
        request.setDictate(dictate);
        request.setDownloadInfo(new DownloadInfo(url, file, action));
        return request;
    }
}
