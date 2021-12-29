package io.agora.agoraeduuikit.impl.extapps.countdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.agoraeducore.core.context.EduContextPool;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppBase;
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppLaunchState;
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppUserRole;
import io.agora.agoraeducore.extensions.extapp.TimeUtil;
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId;
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver;
import io.agora.agoraeduuikit.R;
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket;
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal;
import kotlin.jvm.Synchronized;

import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.DEFAULT_DURATION;
import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.MAX_DURATION;
import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.PROPERTIES_KEY_DURATION;
import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.PROPERTIES_KEY_PAUSE_TIME;
import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.PROPERTIES_KEY_START_TIME;
import static io.agora.agoraeduuikit.impl.extapps.countdown.CountdownStatics.PROPERTIES_KEY_STATE;

public class CountDownExtApp extends AgoraExtAppBase {
    private static final String TAG = "CountDownExtApp";

    private View mLayout;
    private CountDownClock mCountDownClock;
    private AppCompatImageView close;
    private RelativeLayout durationLayout;
    private AppCompatEditText durationEditText;
    private AppCompatTextView actionBtn;

    private boolean mCountdownStarted = false;
    private boolean mCountdownPaused = false;

    private boolean mAppLoaded = false;
    private boolean mPendingPropertyUpdate = false;
    private Map<String, Object> mPendingProperties;
    private Map<String, Object> mPendingCause;

    private boolean actionIsRestart = false;
    private AgoraWidgetMessageObserver whiteBoardObserver = (msg, id) -> {
        AgoraBoardInteractionPacket packet = new Gson().fromJson(msg, AgoraBoardInteractionPacket.class);
        if (packet.getSignal().getValue() == AgoraBoardInteractionSignal.BoardGrantDataChanged.getValue()) {
            String userUuid = getEduContextPool().userContext().getLocalUserInfo().getUserUuid();
            boolean granted = ((ArrayList<String>) packet.getBody()).contains(userUuid);
            setDraggable(granted);
        }
    };
    private static class NumberParser {
        static int parseStringIntOrZero(@Nullable Object obj) {
            int value = 0;
            if (obj instanceof String) {
                try {
                    value = Integer.parseInt((String) obj);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            return value;
        }

        static long parseStringLongOrZero(@Nullable Object obj) {
            long value = 0;
            if (obj instanceof String) {
                try {
                    value = Long.parseLong((String) obj);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            return value;
        }
    }

    @Override
    public void onExtAppLoaded(@NotNull Context context, @NonNull RelativeLayout parent, @NonNull View view, @Nullable EduContextPool eduContextPool) {
        super.onExtAppLoaded(context, parent, view, eduContextPool);
        Log.d(TAG, "onExtAppLoaded, appId=" + getIdentifier());
        synchronized (this) {
            setDraggable(true);
            mAppLoaded = true;
            parent.post(() -> {

                if (mPendingPropertyUpdate) {
                    String properties = mPendingProperties != null ? mPendingProperties.toString() : "";
                    String cause = mPendingCause != null ? mPendingCause.toString() : "";
                    Log.d(TAG, "update pending property update, " + properties + ", " + cause);
                    parseProperties(mPendingProperties, mPendingCause);
                    mPendingPropertyUpdate = false;
                }
            });
        }
        getEduContextPool().widgetContext().addWidgetMessageObserver(whiteBoardObserver, AgoraWidgetDefaultId.WhiteBoard.getId());
    }

    @Override
    public void onPropertyUpdated(Map<String, Object> properties, @Nullable Map<String, Object> cause) {
        synchronized (this) {
            super.onPropertyUpdated(properties, cause);
            if (!mAppLoaded) {
                Log.d(TAG, "onPropertyUpdated, " + getIdentifier() +
                        ", request to update property when app is not loaded");
                mPendingPropertyUpdate = true;
                mPendingProperties = properties;
                mPendingCause = cause;
            } else {
                Log.d(TAG, "onPropertyUpdated, " + getIdentifier() +
                        ", request to update property when app is already loaded");
                parseProperties(properties, cause);
            }
        }
    }

    @Synchronized
    private void parseProperties(@NonNull Map<String, Object> properties, @Nullable Map<String, Object> cause) {
        // Countdown state means whether the count down is started, stopped or paused
        int state = NumberParser.parseStringIntOrZero(properties.get(PROPERTIES_KEY_STATE));
        boolean started = state == 1;
        boolean paused = state == 2;

        long startTime = NumberParser.parseStringLongOrZero(properties.get(PROPERTIES_KEY_START_TIME));
        long pauseTime = NumberParser.parseStringLongOrZero(properties.get(PROPERTIES_KEY_PAUSE_TIME));
        int duration = NumberParser.parseStringIntOrZero(properties.get(PROPERTIES_KEY_DURATION));

        Log.i(TAG, "Countdown properties updated, started:" + started +
                ", paused:" + paused + ", start time:" + startTime +
                ", pause time:" + pauseTime + ", duration:" + duration);

        if (!paused) {
            if (started) {
                long currentTime = TimeUtil.currentTimeMillis() / 1000;
                int leftSecond = (int) (startTime + duration - currentTime);
                if (leftSecond > 0 && mCountDownClock != null) {
                    startCountDownInSeconds(leftSecond);
                }
            }
        } else if (!mCountdownPaused) {
            if (mCountdownStarted) {
                // If currently the app is counting down the clock,
                // we should stop the counting where it is
                Log.d(TAG, "Countdown is paused, stop local countdown ticking");
                stopCountdownTicking();
            } else {
                // If it is not counting (e.g., the app is launched
                // just after the counting has already paused), the
                // clock stays at a time calculated according to the
                // timestamp properties.
                long leftInMilliSec = (duration - (pauseTime - startTime)) * 1000;
                Log.d(TAG, "Countdown has been paused");
                setSpecificCountdownTime(leftInMilliSec);
            }
        }

        mCountdownStarted = started;
        mCountdownPaused = paused;
    }

    @Override
    public void onExtAppUnloaded() {
        synchronized (this) {
            mAppLoaded = false;
            mCountDownClock.resetCountdownTimer();
        }
    }

    @NonNull
    @Override
    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    public View onCreateView(@NonNull Context content) {
        mLayout = LayoutInflater.from(content).inflate(R.layout.extapp_countdown, null, false);
        mLayout.setClickable(true);

        // parseProperties(getProperties(), null);
        mCountDownClock = mLayout.findViewById(R.id.countdown_clock);
        mCountDownClock.resetCountdownTimer();

        close = mLayout.findViewById(R.id.close_Image);
        durationLayout = mLayout.findViewById(R.id.duration_Layout);
        durationEditText = mLayout.findViewById(R.id.duration_EditText);
        actionBtn = mLayout.findViewById(R.id.action_Btn);
        readyUIByRole();

        return mLayout;
    }

    private void startCountDownInSeconds(long seconds) {
        Log.d(TAG, "startCountDown " + seconds + " sec");
        long left = seconds;
        if (left < 0) {
            left = 0;
        } else if (left >= MAX_DURATION) {
            left = MAX_DURATION - 1;
        }

        long finalLeft = left;
        if (mCountDownClock.isAttachedToWindow()) {

            mCountDownClock.setCountdownListener(new CountDownClock.CountdownCallBack() {
                @Override
                public void countdownAboutToFinish() {
                    Log.d(TAG, "Countdown is about to finish");
                    mCountDownClock.setDigitTextColor(Color.RED);
                }

                @Override
                public void countdownFinished() {
                    Log.d(TAG, "Countdown finished");
                }
            });

            mCountDownClock.post(() -> mCountDownClock.startCountDown(finalLeft * 1000));
        } else {
            Log.w(TAG, "extension app view has not attached to window, UI operations fail");
        }
    }

    private void setSpecificCountdownTime(long milliSeconds) {
        if (mCountDownClock.isAttachedToWindow()) {
            mCountDownClock.post(() -> mCountDownClock.setCountDownTime(milliSeconds));
        } else {
            Log.w(TAG, "extension app view has not attached to window, UI operations fail");
        }
    }

    private void stopCountdownTicking() {
        if (mCountDownClock.isAttachedToWindow()) {
            mCountDownClock.post(() -> mCountDownClock.pauseCountdown());
        } else {
            Log.w(TAG, "extension app view has not attached to window, UI operations fail");
        }
    }

    public static int getAppIconResource() {
        return R.drawable.agora_tool_icon_countdown;
    }

    private void readyUIByRole() {
        if (getExtAppContext() == null) {
            Constants.AgoraLog.e(TAG + "->getExtAppContext() is null, please check.");
            return;
        }
        boolean isTeacher = getExtAppContext().getLocalUserInfo().getUserRole().equals(AgoraExtAppUserRole.TEACHER);
        if (!isTeacher) {
            durationLayout.setVisibility(View.GONE);
            actionBtn.setVisibility(View.GONE);
            close.setVisibility(View.GONE);
            return;
        }
        durationLayout.setVisibility(View.VISIBLE);
        durationEditText.setText(String.valueOf(DEFAULT_DURATION));
        durationEditText.setEnabled(true);
        durationEditText.setFocusable(true);
        durationEditText.setFocusableInTouchMode(true);
        actionBtn.setVisibility(View.VISIBLE);
        close.setOnClickListener(v -> closeSelf());
        durationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int duration = Integer.parseInt(s.toString());
                    durationEditText.setTextColor(mLayout.getResources().getColor(
                            duration > MAX_DURATION ? R.color.red : R.color.black));
                    actionBtn.setEnabled(duration <= MAX_DURATION);
                }
            }
        });
        actionBtn.setOnClickListener(v -> {
            if (actionIsRestart) {
                restartCountdownByTeacher();
            } else {
                startCountdownByTeacher();
            }
        });
    }

    private void closeSelf() {
        CountdownStatus status = new CountdownStatus(
                String.valueOf(CountdownLaunchStatus.Init.getValue()),
                null, null, null);
        Map<String, Object> common = new HashMap<>(1);
        common.put(PROPERTIES_KEY_STATE, AgoraExtAppLaunchState.Stopped.getValue());
        updateProperties(status.convert(), new HashMap<>(), common, null);
    }

    private void startCountdownByTeacher() {
        Editable duration = durationEditText.getText();
        if (duration != null && duration.length() > 0) {
            actionBtn.setText(mLayout.getResources().getString(R.string.restart));
            actionIsRestart = true;
            durationLayout.setVisibility(View.GONE);
            CountdownStatus status = new CountdownStatus(
                    String.valueOf(CountdownLaunchStatus.Started.getValue()),
                    String.valueOf(TimeUtil.currentTimeMillis() / 1000),
                    null, duration.toString());
            Map<String, Object> common = new HashMap<>(1);
            common.put(PROPERTIES_KEY_STATE, AgoraExtAppLaunchState.Running.getValue());
            updateProperties(status.convert(), new HashMap<>(), common, null);
        } else {
            Constants.AgoraLog.e(TAG + "->duration is empty or null, please check.");
        }
    }

    private void restartCountdownByTeacher() {
        actionBtn.setText(mLayout.getResources().getString(R.string.start));
        actionIsRestart = false;
        durationLayout.setVisibility(View.VISIBLE);
        durationEditText.setText(String.valueOf(DEFAULT_DURATION));
        CountdownStatus status = new CountdownStatus(
                String.valueOf(CountdownLaunchStatus.Paused.getValue()),
                null, null, null);
        Map<String, Object> common = new HashMap<>(1);
        common.put(PROPERTIES_KEY_STATE, AgoraExtAppLaunchState.Running);
        updateProperties(status.convert(), new HashMap<>(), common, null);
    }
}
