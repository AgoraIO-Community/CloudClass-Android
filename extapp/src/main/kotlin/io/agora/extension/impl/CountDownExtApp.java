package io.agora.extension.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.agora.extension.AgoraExtAppBase;
import io.agora.extension.R;

public class CountDownExtApp extends AgoraExtAppBase {
    private static final String PROPERTIES_KEY_STARTTIME = "startTime";
    private static final String PROPERTIES_KEY_DURATION = "duration";

    private TextView tvCount;
    private int leftSecond;
    private final Runnable countDownRun = new Runnable() {
        @Override
        public void run() {
            if (leftSecond > 0) {
                leftSecond --;
                postCountDown(1000);
            }
        }
    };

    @Override
    public void onExtAppLoaded(@NotNull Context context) {

    }

    @Override
    public void onPropertyUpdated(@NotNull Map<String, Object> properties, @Nullable Map<String, Object> cause) {
        parseProperties(properties);
    }

    private void parseProperties(@NotNull Map<String, Object> properties) {
        try {
            Object timeObject = properties.get(PROPERTIES_KEY_STARTTIME);
            long startTime = 0;
             if (timeObject instanceof String) {
                startTime = Long.parseLong((String) timeObject);
            }

            Object leftObject = properties.get(PROPERTIES_KEY_DURATION);
            int duration = 0;
            if (leftObject instanceof String) {
                duration = Integer.parseInt((String) leftObject);
            }

            long currentTime = System.currentTimeMillis() / 1000;
            long delay = 0;
            leftSecond = (int) (startTime + duration - currentTime);

            if (leftSecond > 0) {
                postCountDown(delay);
            }
        } catch (Exception e) {
            Log.e("CountDownExtApp", "", e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void postCountDown(long delay) {
        if (tvCount != null) {
            tvCount.post(() -> {
                tvCount.setText(leftSecond + "");
                tvCount.removeCallbacks(countDownRun);
                tvCount.postDelayed(countDownRun, delay);
            });
        }
    }

    @Override
    public void onExtAppUnloaded() {
        tvCount.removeCallbacks(countDownRun);
        tvCount = null;
    }

    @NotNull
    @Override
    public View onCreateView(@NotNull Context content) {
        @SuppressLint("InflateParams")
        View layout = LayoutInflater.from(content).inflate(
                R.layout.extapp_countdown, null, false);
        tvCount = layout.findViewById(R.id.tv_count);

        layout.findViewById(R.id.btn_start).setOnClickListener(
                v -> updateProperties(new HashMap<String, Object>() {{
                    put(PROPERTIES_KEY_STARTTIME, System.currentTimeMillis() / 1000 + "");
                    put(PROPERTIES_KEY_DURATION, 20 + "");
        }}, new HashMap<>(), null));

        layout.findViewById(R.id.btn_close).setOnClickListener(v -> unload());
        parseProperties(getProperties());
        return layout;
    }

    public static String getAppIdentifier() {
        return "io.agora.test";
    }

    public static int getAppIconResource() {
        return R.drawable.agora_tool_icon_countdown;
    }


}
