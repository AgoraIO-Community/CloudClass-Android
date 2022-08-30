package io.agora.education;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.tencent.bugly.crashreport.CrashReport;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;

    private static String appId;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        strategy.setAppVersion(AgoraEduSDK.version());
        CrashReport.initCrashReport(getApplicationContext(), "750ba2ddee", false, strategy);

        // 为了方便测试切换环境
        Boolean isTestMode = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false);
        if (isTestMode != null && !isTestMode) {
            appId = getString(R.string.agora_app_id);

            String apiUrl = getString(R.string.agora_api_host);
            if (!TextUtils.isEmpty(apiUrl) && !apiUrl.equals("Agora API Host")) {
                String json = String.format("{\"edu.apiUrl\":\"%s\"}", apiUrl);
                AgoraEduSDK.setParameters(json);
            }
            String reportUrl = getString(R.string.agora_report_host);
            if (!TextUtils.isEmpty(reportUrl) && !reportUrl.equals("Report API Host")) {
                String json = String.format("{\"edu.reportUrl\":\"%s\"}", reportUrl);
                AgoraEduSDK.setParameters(json);
            }
        }
        setDevHost();
    }

    public void setDevHost(){
        AgoraEduEnv env = PreferenceManager.getObject(Constants.KEY_SP_ENV, AgoraEduEnv.class);
        if (env != null) {
            AgoraEduSDK.INSTANCE.setAgoraEduEnv(env);
        }
    }

    @NonNull
    public static String getAppId() {
        if (TextUtils.isEmpty(appId)) {
            return "";
        }
        return appId;
    }

    public static void setAppId(String appId) {
        EduApplication.appId = appId;
    }
}
