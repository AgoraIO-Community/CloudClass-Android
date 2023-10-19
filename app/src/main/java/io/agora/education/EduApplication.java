package io.agora.education;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.base.ToastManager;
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.core.internal.education.impl.util.UnCatchExceptionHandler;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK;
import io.agora.agoraeducore.core.internal.launch.AgoraSDKInitUtils;
import io.agora.agoraeducore.core.utils.SkinUtils;
import io.agora.agoraeduuikit.util.MultiLanguageUtil;
import io.agora.agoraeduuikit.util.SpUtil;
import io.agora.education.config.AppConstants;
import io.agora.education.utils.AppUtil;

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
        AgoraSDKInitUtils.INSTANCE.initSDK(this);
        //注册Activity生命周期监听回调，此部分一定加上，因为有些版本不加的话多语言切换不回来
        registerActivityLifecycleCallbacks(callbacks);
        if (PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false)) {
            UnCatchExceptionHandler.Companion.getExceptionHandler().init(this, "io.agora");
        }
        // 为了方便测试切换环境
//        Boolean isTestMode = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false);
//        if (isTestMode != null && isTestMode) {
//            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
//            strategy.setAppVersion(AgoraEduSDK.version());
//            CrashReport.initCrashReport(getApplicationContext(), "750ba2ddee", false, strategy);
//        }

        // TODO test XXX
        // setTestDev();

        setDevHost();
        setDarkMode();
        AppUtil.setDefLanguageForSys(this);
    }

    void setTestDev() {
        PreferenceManager.put(Constants.KEY_SP_USE_OPEN_TEST_MODE, true);
        PreferenceManager.put(Constants.KEY_SP_ENV, AgoraEduEnv.ENV);
    }

    void setDarkMode() {
        Boolean isDarkMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false);
        if (isDarkMode != null && isDarkMode) {
            SkinUtils.INSTANCE.setNightMode(true);
        }
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

//    @Override
//    protected void attachBaseContext(Context base) {
//        // 会导致视频方向不对
//        super.attachBaseContext(MultiLanguageUtil.attachBaseContext(base));
//    }
}
