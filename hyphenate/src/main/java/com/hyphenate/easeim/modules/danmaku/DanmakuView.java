package com.hyphenate.easeim.modules.danmaku;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.utils.RandomUtil;
import com.hyphenate.easeim.utils.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DanmakuView的基类，继承自RelativeLayout，一个弹幕对应一个DanmakuView。
 * 这里实现了一些通用的功能。
 * <p>
 *
 */
@SuppressWarnings("unused")
public class DanmakuView extends RelativeLayout {

    /**
     * 弹幕内容
     */
    private Danmaku mDanmaku;

    private Context context;

    /**
     * 监听
     */
    private ListenerInfo mListenerInfo;

    private String message;

    private class ListenerInfo {
        private ArrayList<OnEnterListener> mOnEnterListeners;

        private List<OnExitListener> mOnExitListener;
    }

    /**
     * 弹幕进场时的监听
     */
    public interface OnEnterListener {
        void onEnter(DanmakuView view);
    }

    /**
     * 弹幕离场后的监听
     */
    public interface OnExitListener {
        void onExit(DanmakuView view);
    }

    /**
     * 显示时长 ms
     */
    private int mDuration;

    private TextView content;

    private ImageView gift;
    private RelativeLayout layout_root;
    private ImageView avatar;

    public DanmakuView(Context context) {
        super(context);
        initView(context);
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public DanmakuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public TextView getContent() {
        return content;
    }

    public ImageView getGift() {
        return gift;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void hideGift(){
        layout_root.setBackground(null);
        avatar.setImageDrawable(null);
        gift.setImageDrawable(null);
    }

    //初始化UI，可根据业务需求设置默认值。
    private void initView(Context context) {

        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.danmaku_layout, this, true);
        content = (TextView) findViewById(R.id.danma_content);
        gift = (ImageView) findViewById(R.id.danma_gift);
        layout_root = (RelativeLayout) findViewById(R.id.danma_root_layout);
        avatar = findViewById(R.id.avatar);
    }

    /**
     * 设置弹幕内容
     */
    public void setDanmaku(Danmaku danmaku) {
        mDanmaku = danmaku;
        content.setText(danmaku.text);
        switch (danmaku.mode) {
            case top:
            case bottom:
                setGravity(Gravity.CENTER);
                break;
            case scroll:
            default:
                setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                break;
        }
        if(!danmaku.giftUrl.isEmpty()){
            mDanmaku.color = "#ffffffff";
            layout_root.setBackground(context.getResources().getDrawable(randomBackground()));
            avatar.setImageDrawable(content.getResources().getDrawable(R.mipmap.ease_chatting_biaoqing_btn_normal));
            Glide.with(context.getApplicationContext()).load(danmaku.avatarUrl)
                    .apply(RequestOptions.placeholderOf(R.mipmap.ease_chatting_biaoqing_btn_normal))
                    .into(avatar);
            Glide.with(context.getApplicationContext()).load(danmaku.giftUrl)
                    .apply(RequestOptions.placeholderOf(R.mipmap.ee_33))
                    .into(gift);
        }
    }

    private int randomBackground() {
        int i = RandomUtil.nextInt(4);
        if (i == 1) {
            return R.drawable.gift_danmaku_clolor_2;
        } else if (i == 2) {
            return R.drawable.gift_danmaku_clolor_3;
        } else if (i == 3) {
            return R.drawable.gift_danmaku_clolor_4;
        } else {
            return R.drawable.gift_danmaku_clolor_1;
        }
    }

    public Danmaku getDanmaku() {
        return mDanmaku;
    }

    /**
     * 显示弹幕
     */
    public void show(final ViewGroup parent, int duration) {
        mDuration = duration;
        switch (mDanmaku.mode) {
            case top:
            case bottom:
                showFixedDanmaku(parent, duration);
                break;
            case scroll:
            default:
                showScrollDanmaku(parent, duration);
                break;
        }

        if (hasOnEnterListener()) {
            for (OnEnterListener listener : getListenerInfo().mOnEnterListeners) {
                listener.onEnter(this);
            }
        }
        postDelayed(() -> {
            setVisibility(GONE);
            if (hasOnExitListener()) {
                for (OnExitListener listener : getListenerInfo().mOnExitListener) {
                    listener.onExit(DanmakuView.this);
                }
            }
            parent.removeView(DanmakuView.this);
        }, duration);
    }

    private void showScrollDanmaku(ViewGroup parent, int duration) {
        int screenWidth = ScreenUtil.getScreenWidth();
        int textLength = getViewLength();
        scrollTo(-screenWidth, 0);
        parent.addView(this);
        smoothScrollTo(textLength, 0, duration);
    }

    private void showFixedDanmaku(ViewGroup parent, int duration) {
        setGravity(Gravity.CENTER);
        parent.addView(this);
    }

    private ListenerInfo getListenerInfo() {
        if (mListenerInfo == null) {
            mListenerInfo = new ListenerInfo();
        }
        return mListenerInfo;
    }

    public void addOnEnterListener(OnEnterListener l) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnEnterListeners == null) {
            li.mOnEnterListeners = new ArrayList<>();
        }
        if (!li.mOnEnterListeners.contains(l)) {
            li.mOnEnterListeners.add(l);
        }
    }

    public void clearOnEnterListeners() {
        ListenerInfo li = getListenerInfo();
        if (li.mOnEnterListeners == null || li.mOnEnterListeners.size() == 0) {
            return;
        }
        li.mOnEnterListeners.clear();
    }

    public void addOnExitListener(OnExitListener l) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnExitListener == null) {
            li.mOnExitListener = new CopyOnWriteArrayList<>();
        }
        if (!li.mOnExitListener.contains(l)) {
            li.mOnExitListener.add(l);
        }
    }

    public void clearOnExitListeners() {
        ListenerInfo li = getListenerInfo();
        if (li.mOnExitListener == null || li.mOnExitListener.size() == 0) {
            return;
        }
        li.mOnExitListener.clear();
    }

    public boolean hasOnEnterListener() {
        ListenerInfo li = getListenerInfo();
        return li.mOnEnterListeners != null && li.mOnEnterListeners.size() != 0;
    }

    public boolean hasOnExitListener() {
        ListenerInfo li = getListenerInfo();
        return li.mOnExitListener != null && li.mOnExitListener.size() != 0;
    }

    public int getViewLength() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec((1<<30)-1,MeasureSpec.AT_MOST);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec((1<<30)-1,MeasureSpec.AT_MOST);
        layout_root.measure(widthMeasureSpec,heightMeasureSpec);
        return layout_root.getMeasuredWidth();
    }

    public int getDuration() {
        return mDuration;
    }

    /**
     * 恢复初始状态
     */
    public void restore() {
        clearOnEnterListeners();
        clearOnExitListeners();
        setVisibility(VISIBLE);
        setScrollX(0);
        setScrollY(0);
    }

    private Scroller mScroller;

    public void smoothScrollTo(int x, int y, int duration) {
        if (mScroller == null) {
            mScroller = new Scroller(getContext(), new LinearInterpolator());
//            content.setScroller(mScroller);

        }

        int sx = getScrollX();
        int sy = getScrollY();
        mScroller.startScroll(sx, sy, x - sx, y - sy, duration);
    }

    @Override
    public void computeScroll() {
        if (mScroller != null && mScroller.computeScrollOffset()) {
//            EasyL.v(TAG, "computeScroll: " + mScroller.getCurrX());
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    void callExitListener() {
        for (OnExitListener listener : getListenerInfo().mOnExitListener) {
            listener.onExit(this);
        }
    }
}
