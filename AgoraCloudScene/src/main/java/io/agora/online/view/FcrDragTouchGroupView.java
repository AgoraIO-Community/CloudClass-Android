package io.agora.online.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.online.component.common.AbsAgoraEduComponent;

import io.agora.agoraeducore.core.internal.log.LogX;

/**
 * author : felix
 * date : 2022/10/9
 * description : view - 拖动，单击，双击
 */
public class FcrDragTouchGroupView extends AbsAgoraEduComponent {
    private float downX;
    private float downY;

    public int limitWidth = 1000;
    public int limitHeight = 2000;

    private Handler handler = new Handler();

    private int count = 0;//点击次数
    private long firstClick = 0;//第一次点击时间
    private long secondClick = 0;//第二次点击时间
    private long lastDoubleClick = 0;//上次响应成功时间戳
    public boolean isDoubleClicked = false;// 当前view是否被双击
    /**
     * 两次点击时间间隔，单位毫秒
     */
    private final int totalTimeOne = 200;
    private final int totalTimeTwo = 500;
    private final float touchLimit = 0.1f;


    /**
     * 双击 响应 间隔
     **/
    private final int timeComplete = 1000;

    private OnDoubleClickListener onDoubleClickListener;

    private OnTouchListener onTouchListener;

    /**
     * 记录按下位置
     */
    private int interceptX = 0;
    private int interceptY = 0;
    /**
     * 拖动最小偏移量
     */
    private static final int MINIMUM_OFFSET = 5;

    private int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    public void setOnDoubleClickListener(OnDoubleClickListener onDoubleClickListener) {
        this.onDoubleClickListener = onDoubleClickListener;
    }

    public void setOnDragTouchListener(OnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    private boolean isEnableDrag = true;

    public void setEnableDrag(boolean isEnableDrag) {
        this.isEnableDrag = isEnableDrag;
    }

    /**
     * 组件拖动的范围
     *
     * @param width
     * @param height
     */
    public void setDragRange(int width, int height) {
        this.limitWidth = width;
        this.limitHeight = height;
    }

    public FcrDragTouchGroupView(@NonNull Context context) {
        super(context);
    }

    public FcrDragTouchGroupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FcrDragTouchGroupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return true;
//    }


    /**
     * 解决点击与拖动冲突的关键代码
     *
     * @param ev
     * @return
     */
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        //此回调如果返回true则表示拦截TouchEvent由自己处理，false表示不拦截TouchEvent分发出去由子view处理
//        //解决方案：如果是拖动父View则返回true调用自己的onTouch改变位置，是点击则返回false去响应子view的点击事件
//        boolean isIntercept = false;
//        switch (ev.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                interceptX = (int) ev.getX();
//                interceptY = (int) ev.getY();
//                isIntercept = false;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                //在一些dpi较高的设备上点击view很容易触发 ACTION_MOVE，所以此处做一个过滤
//                isIntercept = Math.abs(ev.getX() - interceptX) > MINIMUM_OFFSET && Math.abs(ev.getY() - interceptY) > MINIMUM_OFFSET;
//                break;
//            case MotionEvent.ACTION_UP:
//                break;
//            default:
//                break;
//        }
//        return isIntercept;
//    }
    public void resetPosition() {
        FrameLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
        params.leftMargin = 0;
        params.topMargin = 0;
        setLayoutParams(params);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handleTouchEvent = super.onTouchEvent(event);
        if (onTouchListener != null) {
            onTouchListener.onTouchEvent(event);
        }
        if (this.isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    downForClick();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!isEnableDrag) {
                        break;
                    }
                    float xDistance = event.getX() - downX;
                    float yDistance = event.getY() - downY;

                    if (xDistance != 0 && yDistance != 0) {
//                        int left = (int) (getLeft() + xDistance);
//                        int right = (int) (getRight() + xDistance);
//                        int top = (int) (getTop() + yDistance);
//                        int bottom = (int) (getBottom() + yDistance);
//
//                        // 设置边界
//                        if (left < 0) {
//                            left = 0;
//                            right = left + getWidth();
//                        }
//                        if (right > limitWidth) {
//                            right = limitWidth;
//                            left = right - getWidth();
//                        }
//                        if (top < 0) {
//                            top = 0;
//                            bottom = top + getHeight();
//                        }
//                        if (bottom > limitHeight) {
//                            bottom = limitHeight;
//                            top = bottom - getHeight();
//                        }

                        // 移动
                        //其他view刷新时，会导致view回到原点，可以用设置LayoutParams的方式代替
                        //this.layout(left, top, right, bottom);

                        FrameLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
                        params.leftMargin += xDistance;
                        params.topMargin += yDistance;

                        int leftLimit = (limitWidth - getWidth()) / 2;
                        int rightLimit = limitWidth - leftLimit - getWidth();

                        if (params.leftMargin < -leftLimit) {
                            params.leftMargin = -leftLimit;
                        } else if (params.leftMargin > rightLimit) {
                            params.leftMargin = rightLimit;
                        }

                        int topLimit = (limitHeight - getHeight()) / 2;
                        int bottomLimit = limitHeight - topLimit - getHeight();

                        if (params.topMargin < -topLimit) {
                            params.topMargin = -topLimit;
                        } else if (params.topMargin > bottomLimit) {
                            params.topMargin = bottomLimit;
                        }

                        setLayoutParams(params);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    xDistance = event.getX() - downX;
                    yDistance = event.getY() - downY;

                    //LogX.e("FcrDragTouchGroupView", "x=" + Math.abs(xDistance) + "||y=" + Math.abs(yDistance) + "|| touchSlop=" + touchSlop);

                    if (Math.abs(xDistance) < touchLimit && Math.abs(yDistance) < touchLimit) {
                        // 避免拖动的时候，触发单击事件
                        upForClick();
                    } else {
                        // 避免拖动的时候，触发双击事件
                        handler.removeCallbacksAndMessages(null);
                        count = 0;
                        firstClick = 0;
                        secondClick = 0;
                    }

                    setPressed(false);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    setPressed(false);
                    break;
                default:
                    break;
            }
            return true;
        }
        return handleTouchEvent;
    }

    void downForClick() {
        count++;
        if (1 == count) {
            firstClick = System.currentTimeMillis();//记录第一次点击时间
            isDoubleClicked = false;
        } else if (2 == count) {
            handler.removeCallbacksAndMessages(null);
            secondClick = System.currentTimeMillis();//记录第二次点击时间
            if (secondClick - firstClick < totalTimeTwo) {//判断二次点击时间间隔是否在设定的间隔时间之内
                /*判断 上次按钮生效间隔 */
                if (secondClick - lastDoubleClick > timeComplete) {
                    lastDoubleClick = secondClick;
                    if (onDoubleClickListener != null) {
                        onDoubleClickListener.onDoubleClick();
                        isDoubleClicked = true;
                    }
                }
                count = 0;
                firstClick = 0;
                secondClick = 0;
            } else {
                firstClick = secondClick;
                count = 1;
            }
        }

    }

    void upForClick() {
        if (1 == count) {
            long firstUpClick = System.currentTimeMillis(); // 记录第一次点击时间
            long deltaClick = firstUpClick - firstClick;
            if (deltaClick < totalTimeOne / 2) {
                handler.postDelayed(() -> {
                    if (onDoubleClickListener != null) {
                        onDoubleClickListener.onClick();
                    }
                    count = 0;
                    firstClick = 0;
                    secondClick = 0;
                }, totalTimeOne);
            }
        }
    }


    public interface OnDoubleClickListener {
        void onDoubleClick();

        void onClick();
    }

    public interface OnTouchListener {
        boolean onTouchEvent(MotionEvent event);
    }
}
