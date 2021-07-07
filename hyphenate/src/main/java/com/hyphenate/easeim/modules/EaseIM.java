package com.hyphenate.easeim.modules;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMUserInfo;

public class EaseIM {
    private static final String TAG = EaseIM.class.getSimpleName();
    private static EaseIM instance;
    private Context context;
    private boolean sdkInited = false;

    private EaseIM() {}

    public static EaseIM getInstance() {
        if(instance == null) {
            synchronized (EaseIM.class) {
                if(instance == null) {
                    instance = new EaseIM();
                }
            }
        }
        return instance;
    }

    public synchronized boolean init(Context context, String appkey) {
        if(sdkInited) {
            return true;
        }
        context = context.getApplicationContext();
        // if there is application has remote service, application:onCreate() maybe called twice
        // this check is to make sure SDK will initialized only once
        // return if process name is not application's name since the package name is the default process name
        if (!isMainProcess(context)) {
            Log.e(TAG, "enter the service process!");
            return false;
        }

        EMOptions options = new EMOptions();
        options.setAutoLogin(false);
        options.setAppKey(appkey);
        EMClient.getInstance().init(context, options);
        EMClient.getInstance().setDebugMode(true);
        sdkInited = true;
        return true;
    }

    /**
     * 判断是否在主进程
     * @param context
     * @return
     */
    public boolean isMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return context.getApplicationInfo().packageName.equals(appProcess.processName);
            }
        }
        return false;
    }
}
