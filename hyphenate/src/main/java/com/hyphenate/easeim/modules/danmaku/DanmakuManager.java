package com.hyphenate.easeim.modules.danmaku;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hyphenate.easeim.interfaces.Pool;
import com.hyphenate.easeim.utils.RandomUtil;
import com.hyphenate.easeim.utils.ScreenUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
    private static final int RESULT_OK = 0;
    private static final int RESULT_NULL_ROOT_VIEW = 1;
    private static final int RESULT_FULL_POOL = 2;
    private static final int TOO_MANY_DANMAKU = 2;

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

    private int max = 100;

    private final List<DanmakuView> viewList = Collections.synchronizedList(new ArrayList<>());

    private DanmakuManager() {
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
                    60000, // 缓存存活时间：60秒
                    100, // 最大弹幕数：100
                    () -> DanmakuViewFactory.createDanmakuView(context, container));
        }
        setDanmakuContainer(container);
        ScreenUtil.init(context);

        mConfig = new Config();
        mPositionCal = new DanmakuPositionCalculator(this);
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
     * 设置允许同时出现最多的弹幕数，如果屏幕上显示的弹幕数超过该数量，那么新出现的弹幕将被丢弃，
     * 直到有旧的弹幕消失。
     *
     * @param max 同时出现的最多弹幕数
     */
    public void setMaxDanmakuSize(int max) {
        if (mDanmakuViewPool == null) {
            return;
        }
        this.max = max;
        mDanmakuViewPool.setMaxSize(max);
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
     * 发送一条弹幕
     */
    public int send(Danmaku danmaku) {
        if (mDanmakuViewPool == null) {
            throw new NullPointerException("Danmaku view pool is null. Did you call init() first?");
        }

        DanmakuView view = mDanmakuViewPool.get();
        if (view == null) {
            return RESULT_FULL_POOL;
        }
        if (mDanmakuContainer == null || mDanmakuContainer.get() == null) {
            return RESULT_NULL_ROOT_VIEW;
        }
        view.setMessage(danmaku.message);
        view.hideGift();
        view.setDanmaku(danmaku);

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

        // 计算弹幕距离顶部的位置
        DanmakuPositionCalculator dpc = getPositionCalculator();
        int marginTop = dpc.getMarginTop(view);

        if (marginTop == -1) {
            // 屏幕放不下了
            return TOO_MANY_DANMAKU;
        }
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getConfig().lineHeight);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(params);

        marginParams.setMargins(0, marginTop, 0, 0);
        view.setLayoutParams(marginParams);
        view.getContent().setTextSize(getConfig().textSize);
        view.show(mDanmakuContainer.get(), getDisplayDuration(danmaku));
        addViewToList(view);
        return RESULT_OK;
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
        if (viewList.size() > max) {
            viewList.remove(0);
        }
        viewList.add(view);
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
        private int durationScroll;
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

        public int getDurationScroll() {
            return RandomUtil.nextInt(5, 10) * 1000;
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
