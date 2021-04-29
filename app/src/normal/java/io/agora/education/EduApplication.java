package io.agora.education;

import android.app.Application;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import io.agora.edu.launch.AgoraEduSDK;
import io.agora.extension.AgoraExtAppConfiguration;
import io.agora.extension.impl.CountDownExtApp;

//import com.tencent.bugly.crashreport.CrashReport;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;

    private static String appId;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PreferenceManager.init(this);
        appId = getString(R.string.agora_app_id);
//        CrashReport.initCrashReport(getApplicationContext(), "04948355be", true);
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

    @Nullable
    public static String getAppId() {
        if (TextUtils.isEmpty(appId)) {
            return null;
        }
        return appId;
    }
}
