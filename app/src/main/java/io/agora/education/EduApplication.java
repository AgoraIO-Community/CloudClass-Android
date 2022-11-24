package io.agora.education;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.bugly.crashreport.CrashReport;

import java.util.Locale;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK;
import io.agora.agoraeduuikit.util.MultiLanguageUtil;
import io.agora.agoraeduuikit.util.SpUtil;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;
    private static Context mAppContext;

    public static Context getContext() {
        return mAppContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mAppContext = getApplicationContext();
        //注册Activity生命周期监听回调，此部分一定加上，因为有些版本不加的话多语言切换不回来
        registerActivityLifecycleCallbacks(callbacks);
        // 为了方便测试切换环境
        Boolean isTestMode = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false);
        if (isTestMode != null && isTestMode) {
            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
            strategy.setAppVersion(AgoraEduSDK.version());
            CrashReport.initCrashReport(getApplicationContext(), "750ba2ddee", false, strategy);
        }
        setDevHost();
    }

    public void setDevHost() {
        AgoraEduEnv env = PreferenceManager.getObject(Constants.KEY_SP_ENV, AgoraEduEnv.class);
        if (env != null) {
            AgoraEduSDK.INSTANCE.setAgoraEduEnv(env);
        }
    }

    ActivityLifecycleCallbacks callbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            String language = SpUtil.getString(getApplicationContext(), io.agora.education.config.AppConstants.LOCALE_LANGUAGE);
            String country = SpUtil.getString(getApplicationContext(), io.agora.education.config.AppConstants.LOCALE_AREA);
            if (!TextUtils.isEmpty(language) && !TextUtils.isEmpty(country)) {
                //强制修改应用语言
                if (!MultiLanguageUtil.isSameWithSetting(activity)) {
                    Locale locale = new Locale(language, country);
                    MultiLanguageUtil.changeAppLanguage(activity, locale, false);
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
        //Activity 其它生命周期的回调
    };

    @Override
    protected void attachBaseContext(Context base) {
        //系统语言等设置发生改变时会调用此方法，需要要重置app语言
        super.attachBaseContext(MultiLanguageUtil.attachBaseContext(base));
    }
}
