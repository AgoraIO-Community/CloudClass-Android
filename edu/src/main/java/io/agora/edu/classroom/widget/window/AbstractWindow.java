package io.agora.edu.classroom.widget.window;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.edu.util.ThreadUtil;

public class AbstractWindow extends RelativeLayout implements IMinimizable {
    private static final int ANIM_DURATION = 300;

    private String mTag;
    private boolean mIsMinimized;
    private boolean mIsAnimating;
    private IMinimizable.Direction mMinimizeDirection = IMinimizable.Direction.bottomRight;
    private IMinimizable.Direction mRestoreDirection = IMinimizable.Direction.topLeft;

    // Save the original sizes of the window before
    // this window being minimized
    private int mRestoreWidth;
    private int mRestoreHeight;

    private View mOriginalView;
    private View mMinimizedView;

    private ViewPropertyAnimator mMinimizeAnimator;
    private ViewPropertyAnimator mRestoreAnimator;

    public AbstractWindow(Context context) {
        super(context);
        init();
    }

    public AbstractWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AbstractWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTag = getClass().getSimpleName();
    }

    @Override
    public void startMinimize(@Nullable IWindowAnimateListener listener) {
        if (!ThreadUtil.isMainThread()) {
            Log.w(mTag, "Minimization canceled because not in UI thread");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        if (mIsMinimized || mIsAnimating) {
            Log.w(mTag, "Minimization canceled because " +
                    "being animating or already minimized");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        if (mOriginalView == null) {
            Log.w(mTag, "Minimization canceled because original view is null");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        synchronized (this) {
            mIsAnimating = true;
        }

        startMinimizationAnimate(listener);
    }

    private void startMinimizationAnimate(@Nullable IWindowAnimateListener listener) {
        mRestoreWidth = mOriginalView.getWidth();
        mRestoreHeight = mOriginalView.getHeight();
        if (listener != null) listener.onAnimateStart();
        mMinimizeAnimator = mOriginalView.animate()
                .setDuration(ANIM_DURATION)
                .xBy(getTranslateX(mMinimizeDirection))
                .yBy(getTranslateY(mMinimizeDirection))
                .scaleX(getScaleFactorX(mMinimizeDirection, false))
                .scaleY(getScaleFactorY(mMinimizeDirection, false))
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (mMinimizedView != null) {
                        mMinimizedView.setVisibility(VISIBLE);
                    }

                    synchronized (this) {
                        mIsAnimating = false;
                        mIsMinimized = true;
                        mRestoreAnimator = null;
                    }

                    if (listener != null) listener.onAnimateEnd();
                });
    }

    @Override
    public void restoreMinimize(@Nullable IWindowAnimateListener listener) {
        if (!ThreadUtil.isMainThread()) {
            Log.w(mTag, "Minimization restore " +
                    "canceled because not in UI thread");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        if (!mIsMinimized || mIsAnimating) {
            Log.w(mTag, "Minimization restore canceled because " +
                    "being animating or not minimized");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        if (mOriginalView == null) {
            Log.w(mTag, "Minimization canceled because original view is null");
            if (listener != null) listener.onAnimateCancel();
            return;
        }

        synchronized (this) {
            mIsAnimating = true;
        }

        startRestoreAnimate(listener);
    }

    private void startRestoreAnimate(@Nullable IWindowAnimateListener listener) {
        if (listener != null) listener.onAnimateStart();
        mRestoreAnimator = mOriginalView.animate()
                .setDuration(ANIM_DURATION)
                .xBy(getTranslateX(mRestoreDirection))
                .yBy(getTranslateY(mRestoreDirection))
                .scaleX(getScaleFactorX(mRestoreDirection, true))
                .scaleY(getScaleFactorY(mRestoreDirection, true))
                .setInterpolator(new DecelerateInterpolator())
                .withStartAction(() -> {
                    if (mMinimizedView != null) {
                        mMinimizedView.setVisibility(GONE);
                    }
                })
                .withEndAction(() -> {
                    synchronized (this) {
                        mIsAnimating = false;
                        mIsMinimized = false;
                        mRestoreAnimator = null;
                    }

                    if (listener != null) listener.onAnimateEnd();
                });
    }

    protected void setMinimizeDirection(IMinimizable.Direction direction) {
        mMinimizeDirection = direction;
        mRestoreDirection = toReverseDirection(direction);
    }

    private IMinimizable.Direction toReverseDirection(IMinimizable.Direction direction) {
        switch (direction) {
            case top: return IMinimizable.Direction.bottom;
            case right: return IMinimizable.Direction.left;
            case bottom: return IMinimizable.Direction.top;
            case left: return IMinimizable.Direction.right;
            case topLeft: return IMinimizable.Direction.bottomRight;
            case topRight: return IMinimizable.Direction.bottomLeft;
            case bottomLeft: return IMinimizable.Direction.topRight;
            default: return IMinimizable.Direction.topLeft;
        }
    }

    private int getTranslateX(IMinimizable.Direction direction) {
        switch (direction) {
            case top:
            case bottom:
                return 0;
            case left:
            case topLeft:
            case bottomLeft:
                return -mRestoreWidth;
            default: return mRestoreWidth;
        }
    }

    private int getTranslateY(IMinimizable.Direction direction) {
        switch (direction) {
            case left:
            case right:
                return 0;
            case top:
            case topLeft:
            case topRight:
                return -mRestoreHeight;
            default: return mRestoreHeight;
        }
    }

    private float getScaleFactorX(IMinimizable.Direction direction, boolean restore) {
        switch (direction) {
            case top:
            case bottom:
                return 1f;
            default:
                return restore ? 1f : 0f;
        }
    }

    private float getScaleFactorY(IMinimizable.Direction direction, boolean restore) {
        switch (direction) {
            case left:
            case right:
                return 1f;
            default:
                return restore ? 1f : 0f;
        }
    }

    @Override
    public boolean isMinimized() {
        return mIsMinimized;
    }

    @Override
    public void setLayouts(@NonNull View original, View minimize) {
        if (mIsAnimating) {
            Log.w(mTag, "setLayout canceled because minimization is in progress");
            return;
        }

        mOriginalView = original;
        mMinimizedView = minimize;
    }

    @Override
    public void cancelAnimate() {
        if (mRestoreAnimator != null) mRestoreAnimator.cancel();
        if (mMinimizeAnimator != null) mMinimizeAnimator.cancel();
    }
}
