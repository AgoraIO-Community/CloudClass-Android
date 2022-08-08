package io.agora.education;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 为了方便测试切换环境
        Boolean isTestMode = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false);
        if (isTestMode != null && isTestMode) {
            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
            strategy.setAppVersion(AgoraEduSDK.version());
            CrashReport.initCrashReport(getApplicationContext(), "750ba2ddee", false, strategy);
        }

        setDevHost();
    }

    public void setDevHost(){
        AgoraEduEnv env = PreferenceManager.getObject(Constants.KEY_SP_ENV, AgoraEduEnv.class);
        if (env != null) {
            AgoraEduSDK.INSTANCE.setAgoraEduEnv(env);
        }
    }
}
