package com.hyphenate.easeim;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMUserInfo;

import org.jetbrains.annotations.Nullable;

public class EaseIM {
    private static final String TAG = EaseIM.class.getSimpleName();
    private volatile static EaseIM instance;
    private boolean sdkInited = false;

    private EaseIM() {}

    public static EaseIM getInstance() {
        if (instance == null) {
            synchronized (EaseIM.class) {
                if(instance == null) {
                    instance = new EaseIM();
                }
            }
        }
        return instance;
    }

    public synchronized boolean init(Context context, @Nullable String appKey) {
        if (sdkInited) {
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

        EMOptions options = initChatOptions();
        if (appKey != null) {
            options.setAppKey(appKey);
        }

        EMClient.getInstance().init(context, options);
        EMClient.getInstance().setDebugMode(true);
        sdkInited = true;
        return true;
    }

    protected EMOptions initChatOptions() {
        EMOptions options = new EMOptions();
        options.setAppKey("easemob-demo#cloudclass");
        options.setAutoLogin(false);
        return options;
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

    /***
     * 上传用户信息
     * @param emUserInfo
     */
    public void updateOwnInfo(EMUserInfo emUserInfo){
        EMClient.getInstance().userInfoManager().updateOwnInfo(emUserInfo, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {

            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }
}
