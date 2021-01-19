package io.agora.education;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.bugly.crashreport.CrashReport;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;

    private static String appId;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        PreferenceManager.init(this);

        CrashReport.initCrashReport(getApplicationContext(), "04948355be", true);

        appId = getString(R.string.agora_app_id);
    }

    @Nullable
    public static String getAppId() {
        if (TextUtils.isEmpty(appId)) {
            return null;
        }
        return appId;
    }

}
