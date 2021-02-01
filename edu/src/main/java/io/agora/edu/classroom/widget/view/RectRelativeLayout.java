package io.agora.edu.classroom.widget.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class RectRelativeLayout extends RelativeLayout {
    public RectRelativeLayout(Context context) {
        super(context);
    }

    public RectRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
        int heightSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightSpec);
    }
}