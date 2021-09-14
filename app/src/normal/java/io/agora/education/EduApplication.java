package io.agora.education;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.util.ArrayList;
import java.util.Locale;

import io.agora.edu.core.internal.launch.AgoraEduSDK;
import io.agora.edu.extensions.extapp.AgoraExtAppConfiguration;
import io.agora.edu.extensions.extapp.AgoraExtAppLayoutParam;
import io.agora.edu.extensions.extapp.countdown.CountDownExtApp;

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

        ArrayList<AgoraExtAppConfiguration> list = new ArrayList<>();
        list.add(new AgoraExtAppConfiguration(
                "io.agora.countdown",
                new AgoraExtAppLayoutParam(
                        AgoraExtAppLayoutParam.wrap,
                        AgoraExtAppLayoutParam.wrap),
                CountDownExtApp.class,
                Locale.getDefault().getLanguage(),
                null));
        AgoraEduSDK.registerExtApps(list);
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
