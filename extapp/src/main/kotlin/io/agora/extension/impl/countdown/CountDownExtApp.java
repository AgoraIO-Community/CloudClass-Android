package io.agora.extension.impl.countdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import io.agora.extension.AgoraExtAppBase;
import io.agora.extension.R;
import io.agora.extension.TimeUtil;
import kotlin.jvm.Synchronized;

public class CountDownExtApp extends AgoraExtAppBase {
    private static final String TAG = "CountDownExtApp";
    private static final String PROPERTIES_KEY_START_TIME = "startTime";
    private static final String PROPERTIES_KEY_DURATION = "duration";
    private static final String PROPERTIES_KEY_PAUSE_TIME = "pauseTime";
    private static final String PROPERTIES_KEY_STATE = "state";
    private static final int MAX_SECONDS = 3600;

    private static final int MIN_MOVE_DISTANCE_X = 10;
    private static final int MIN_MOVE_DISTANCE_Y = 8;

    private RelativeLayout mContainer;
    private View mLayout;
    private CountDownClock mCountDownClock;
    private int mLastPointerId = -1;
    private int mLastTouchX = -1;
    private int mLastTouchY = -2;
    private boolean mTouched = false;

    private int leftSecond;
    private boolean mCountdownStarted = false;
    private boolean mCountdownPaused = false;

    private boolean mAppLoaded = false;
    private boolean mPendingPropertyUpdate = false;
    private Map<String, Object> mPendingProperties;
    private Map<String, Object> mPendingCause;

    private static class NumberParser {
        static int parseStringIntOrZero(@Nullable Object obj) {
            int value = 0;
            if (obj instanceof String) {
                try {
                    value = Integer.parseInt((String) obj);
                } catch (NumberFormatException e) {
                    value = 0;
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
                    value = 0;
                    e.printStackTrace();
                }
            }

            return value;
        }
    }

    @Override
    public void onExtAppLoaded(@NotNull Context context, @NonNull RelativeLayout container) {
        Log.d(TAG, "onExtAppLoaded, appId=" + getIdentifier());
        synchronized (this) {
            mAppLoaded = true;

            mContainer = container;
            mContainer.post(() -> {
                if (mLayout != null) {
                    mLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    Log.d(TAG, "onExtAppLoaded, layout, ${this}");
                                    if (mLayout.getWidth() > 0 && mLayout.getHeight() > 0) {
                                        mLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                        // The content layout will be default in the center of parent container view
                                        int width = mContainer.getWidth();
                                        int height = mContainer.getHeight();
                                        int w = mLayout.getWidth();
                                        int h = mLayout.getHeight();
                                        RelativeLayout.LayoutParams params =
                                                (RelativeLayout.LayoutParams) mLayout.getLayoutParams();
                                        params.width = w;
                                        params.height = h;
                                        params.leftMargin = (width - params.width) / 2;
                                        params.topMargin = (height - params.height) / 2;
                                        mLayout.setLayoutParams(params);
                                    }
                                }
                            });
                }

                if (mPendingPropertyUpdate) {
                    String properties = mPendingProperties != null ? mPendingProperties.toString() : "";
                    String cause = mPendingCause != null ? mPendingCause.toString() : "";
                    Log.d(TAG, "update pending property update, " + properties + ", " + cause );
                    parseProperties(mPendingProperties, mPendingCause);
                    mPendingPropertyUpdate = false;
                }
            });
        }
    }

    @Override
    public void onPropertyUpdated(Map<String, Object> properties, @Nullable Map<String, Object> cause) {
        synchronized (this) {
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

    @Synchronized private void parseProperties(@NotNull Map<String, Object> properties, @Nullable Map<String, Object> cause) {
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
                leftSecond = (int) (startTime + duration - currentTime);
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

    @NotNull
    @Override
    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    public View onCreateView(@NotNull Context content) {
        mLayout = LayoutInflater.from(content).inflate(R.layout.extapp_countdown, null, false);
        mLayout.setClickable(true);
        mLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Only detect the touch events of the first pointer
                    if (mLastPointerId != -1) {
                        if (mLastPointerId != event.getPointerId(0)) {
                            // Current touching pointer is not the pointer of current touch event,
                            // this event will be ignored.
                            break;
                        }
                    } else {
                        mLastPointerId = event.getPointerId(0);
                    }

                    mLastTouchX = (int) event.getRawX();
                    mLastTouchY = (int) event.getRawY();
                    mTouched = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mTouched || event.getPointerId(0) != mLastPointerId) {
                        break;
                    }

                    if (!coordinateInRange((int) event.getRawX(), (int) event.getRawY())) {
                        break;
                    }

                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();
                    reLayout(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Log.d(TAG, "on layout touch up or canceled");
                    mLastPointerId = -1;
                    mLastTouchX = -1;
                    mLastTouchY = -1;
                    mTouched = false;
                    break;
            }
            return false;
        });

        // parseProperties(getProperties(), null);
        mCountDownClock = mLayout.findViewById(R.id.countdown_clock);
        mCountDownClock.resetCountdownTimer();

        return mLayout;
    }

    private boolean coordinateInRange(int x, int y) {
        int[] location = new int[2];
        mLayout.getLocationOnScreen(location);
        int layoutX = location[0];
        int layoutY = location[1];
        int layoutW = mLayout.getWidth();
        int layoutH = mLayout.getHeight();
        return (layoutX <= x && x <= layoutX + layoutW) &&
                (layoutY <= y && y <= layoutY + layoutH);
    }

    private void reLayout(int x, int y) {
        if (mLayout == null || mContainer == null) {
            return;
        }

        if (mLayout.getParent() != mContainer) {
            return;
        }

        int diffX = x - mLastTouchX;
        int diffY = y - mLastTouchY;

        if (Math.abs(diffX) < MIN_MOVE_DISTANCE_X) {
            diffX = 0;
        }

        if (Math.abs(diffY) < MIN_MOVE_DISTANCE_Y) {
            diffY = 0;
        }

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) mLayout.getLayoutParams();
        int width = params.width;
        int height = params.height;
        int top = params.topMargin;
        int left = params.leftMargin;
        int parentWidth = mContainer.getWidth();
        int parentHeight = mContainer.getHeight();

        if (diffX < 0) {
            if (left + diffX < 0) {
                left = 0;
            } else {
                left += diffX;
            }
        } else {
            if (left + width + diffX > parentWidth) {
                left = parentWidth - width;
            } else {
                left += diffX;
            }
        }

        if (diffY < 0) {
            if (top + diffY < 0) {
                top = 0;
            } else {
                top += diffY;
            }
        } else {
            if (top + height + diffY > parentHeight) {
                top = parentHeight - height;
            } else {
                top += diffY;
            }
        }

        params.leftMargin = left;
        params.topMargin = top;
        mLayout.setLayoutParams(params);

        mLastTouchX += diffX;
        mLastTouchY += diffY;
    }

    private void startCountDownInSeconds(long seconds) {
        Log.d(TAG, "startCountDown " + seconds + " sec");
        long left = seconds;
        if (left < 0) {
            left = 0;
        } else if (left >= MAX_SECONDS) {
            left = MAX_SECONDS - 1;
        }

        long finalLeft = left;
        if (mCountDownClock.isAttachedToWindow()) {

            mCountDownClock.setCountdownListener(new CountDownClock.CountdownCallBack() {
                @Override
                public void countdownAboutToFinish() {
                    Log.d(TAG,"Countdown is about to finish");
                    mCountDownClock.setDigitTextColor(Color.RED);
                }

                @Override
                public void countdownFinished() {
                    Log.d(TAG,"Countdown finished");
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
}
