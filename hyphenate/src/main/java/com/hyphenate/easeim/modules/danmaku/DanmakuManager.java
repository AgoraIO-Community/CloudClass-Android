package com.hyphenate.easeim.modules.danmaku;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.hyphenate.easeim.interfaces.Pool;
import com.hyphenate.easeim.utils.ScreenUtil;
import com.hyphenate.util.EMLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 用法示例：
 * DanmakuManager dm = DanmakuManager.getInstance();
 * dm.init(getContext());
 * dm.show(new Danmaku("test"));
 * <p>
 */
@SuppressWarnings("unused")
public class DanmakuManager {
    private static final String TAG = DanmakuManager.class.getSimpleName();

    private static DanmakuManager sInstance;

    /**
     * 弹幕容器
     */
    WeakReference<FrameLayout> mDanmakuContainer;
    /**
     * 弹幕池
     */
    private Pool<DanmakuView> mDanmakuViewPool;

    private Config mConfig;

    private DanmakuPositionCalculator mPositionCal;

    private final Handler handler;

    private final List<DanmakuView> viewList = Collections.synchronizedList(new ArrayList<>());

    private final ScheduledExecutorService mChecker = Executors.newSingleThreadScheduledExecutor();

    private final List<Danmaku> danmakuList = Collections.synchronizedList(new LinkedList<>());

    private DanmakuManager() {
        handler = new Handler(Looper.getMainLooper());
        mConfig = new Config();
        mPositionCal = new DanmakuPositionCalculator(this);
        scheduleCheck();
    }

    public static DanmakuManager getInstance() {
        if (sInstance == null) {
            sInstance = new DanmakuManager();
        }
        return sInstance;
    }

    /**
     * 初始化。在使用之前必须调用该方法。
     */
    public void init(Context context, FrameLayout container) {
        if (mDanmakuViewPool == null) {
            mDanmakuViewPool = new CachedDanmakuViewPool(
                    10000, // 缓存存活时间：10秒
                    () -> DanmakuViewFactory.createDanmakuView(context, container));
        }
        setDanmakuContainer(container);
        ScreenUtil.init(context);
    }

    public void reset() {
        viewList.clear();
        danmakuList.clear();
        getPositionCalculator().getLastDanmakus().clear();
    }

    public Config getConfig() {
        if (mConfig == null) {
            mConfig = new Config();
        }
        return mConfig;
    }

    private DanmakuPositionCalculator getPositionCalculator() {
        if (mPositionCal == null) {
            mPositionCal = new DanmakuPositionCalculator(this);
        }
        return mPositionCal;
    }

    public void setDanmakuViewPool(Pool<DanmakuView> pool) {
        if (mDanmakuViewPool != null) {
            mDanmakuViewPool.release();
        }
        mDanmakuViewPool = pool;
    }

    /**
     * 设置弹幕的容器，所有的弹幕都在这里面显示
     */
    public void setDanmakuContainer(final FrameLayout root) {
        if (root == null) {
            throw new NullPointerException("Danmaku container cannot be null!");
        }
        mDanmakuContainer = new WeakReference<>(root);
    }

    /**
     * 弹幕加入缓存
     */
    public void send(Danmaku danmaku) {
        if (mDanmakuViewPool == null) {
            throw new NullPointerException("Danmaku view pool is null. Did you call init() first?");
        }

        danmakuList.add(danmaku);
    }

    /**
     * 发送一条弹幕
     */
    private synchronized void sendDanmamu(Danmaku danmaku) {
        DanmakuView view = getView(danmaku);

        // 计算弹幕距离顶部的位置
        DanmakuPositionCalculator dpc = getPositionCalculator();
        int marginTop = dpc.getMarginTop(view);

        if (marginTop == -1) {
            danmakuList.add(danmaku);
            // 屏幕放不下了
            return;
        }

        // 字体大小
        int textSize = danmaku.size;
        view.getContent().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        // 字体颜色
        try {
            int color = Color.parseColor(danmaku.color);
            view.getContent().setTextColor(color);
        } catch (Exception e) {
            e.printStackTrace();
            view.getContent().setTextColor(Color.WHITE);
        }

        view.getContent().setTextSize(getConfig().textSize);

        int viewWidth = view.getViewLength();
        if (viewWidth < ScreenUtil.getScreenWidth()) {
            viewWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(viewWidth, getConfig().lineHeight);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(params);

        marginParams.setMargins(0, marginTop, 0, 0);
        view.setLayoutParams(marginParams);

        view.show(mDanmakuContainer.get(), getDisplayDuration(danmaku));
        addViewToList(view);
    }

    // 弹幕上屏，从缓存中移除
    private void sendCacheDanmaku() {
        if (danmakuList.size() > 0) {
            Log.e(TAG, "size:" + danmakuList.size());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    sendDanmamu(danmakuList.get(0));
                    danmakuList.remove(0);
                    checkDanmaku();
                }
            });
        }
    }

    /**
     * @return 返回这个弹幕显示时长
     */
    int getDisplayDuration(Danmaku danmaku) {
        Config config = getConfig();
        int duration;
        switch (danmaku.mode) {
            case top:
                duration = config.getDurationTop();
                break;
            case bottom:
                duration = config.getDurationBottom();
                break;
            case scroll:
            default:
                duration = config.getDurationScroll();
                break;
        }
        return duration;
    }

    public DanmakuView getDanmakuView(String msgId) {
        mDanmakuViewPool.removeView(msgId);
        for (DanmakuView view : viewList
        ) {
            if (view.getMessage().equals(msgId))
                return view;
        }
        return null;
    }

    private void addViewToList(DanmakuView view) {
        int max = 100;
        if (viewList.size() > max) {
            viewList.remove(0);
        }
        viewList.add(view);
    }

    /**
     * 定时检查弹幕是否可以上屏
     */
    private void scheduleCheck() {
        mChecker.scheduleWithFixedDelay(() -> {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        checkDanmaku();
                    }});
            }catch (Throwable t){
                Log.e(TAG, "error: " + t);
            }
        }, 1000, 200, TimeUnit.MILLISECONDS);
    }

    private boolean enableSend = false;

    // 判断弹幕是否可以上屏
    private synchronized void checkDanmaku() {
        if (danmakuList.size() > 0) {
            List<DanmakuView> list = getPositionCalculator().getLastDanmakus();
            if (list.size() == 0) {
                sendCacheDanmaku();
                return;
            }
            int i;
            DanmakuView cacheView = getView(danmakuList.get(0));
            int timeArrive = mPositionCal.calTimeArrive(cacheView);// 这条弹幕需要多久到达屏幕边缘
            for (i = 0; i < list.size(); i++) {
                    DanmakuView lastView = list.get(i);
                    int timeDisappear = mPositionCal.calTimeDisappear(lastView); // 最后一条弹幕还需多久消失
                    boolean isFullyShown = mPositionCal.isFullyShown(lastView);
                    if (timeDisappear <= timeArrive && isFullyShown) {
                        enableSend = true;
                        break;
                    }
            }
            if (enableSend) {
                enableSend = false;
                sendCacheDanmaku();
            } else {
                if (i < getConfig().maxScrollLine) {
                    sendCacheDanmaku();
                }
            }
        }
    }

    private DanmakuView getView(Danmaku danmaku) {
        DanmakuView view = mDanmakuViewPool.get();
        if (view == null) {
            EMLog.e(TAG, "DanmakuView is null");
            return null;
        }

        if (mDanmakuContainer == null || mDanmakuContainer.get() == null) {
            return null;
        }
        view.setMessage(danmaku.message);
        view.hideGift();
        view.setDanmaku(danmaku);
        return view;
    }

    /**
     * 一些配置
     */
    public static class Config {

        /**
         * 行高，单位px
         */
        private int lineHeight;

        /**
         * 滚动弹幕显示时长
         */
        private int durationScroll = 0;
        /**
         * 顶部弹幕显示时长
         */
        private int durationTop;
        /**
         * 底部弹幕的显示时长
         */
        private int durationBottom;

        /**
         * 滚动弹幕的最大行数
         */
        private int maxScrollLine = 0;

        /**
         * 弹幕间距，px
         */
        private int marginTop = 0;

        /**
         * 弹幕文字大小，sp
         */
        private int textSize = 16;

        public int getLineHeight() {
            return lineHeight;
        }

        public void setLineHeight(int lineHeight) {
            this.lineHeight = lineHeight;
        }

        public int getMaxScrollLine() {
            return maxScrollLine;
        }

        public void setDurationScroll(int durationScroll) {
            this.durationScroll = durationScroll;
        }

        public int getDurationScroll() {
            if (durationScroll == 0) {
                durationScroll = 5000;
            }
            return durationScroll;
        }

        public int getDurationTop() {
            if (durationTop == 0) {
                durationTop = 5000;
            }
            return durationTop;
        }

        public void setDurationTop(int durationTop) {
            this.durationTop = durationTop;
        }

        public int getDurationBottom() {
            if (durationBottom == 0) {
                durationBottom = 5000;
            }
            return durationBottom;
        }

        public void setDurationBottom(int durationBottom) {
            this.durationBottom = durationBottom;
        }

        public int getMaxDanmakuLine() {
            if (maxScrollLine == 0) {
                maxScrollLine = 10;
            }
            return maxScrollLine;
        }

        public void setMaxScrollLine(int maxScrollLine) {
            this.maxScrollLine = maxScrollLine;
        }

        public void setMarginTop(int marginTop) {
            this.marginTop = marginTop;
        }

        public int getMarginTop() {
            return marginTop;
        }

        public int getTextSize() {
            return textSize;
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
        }
    }

}
