package io.agora.edu.classroom.widget.video;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

public class VideoViewTextureOutlineProvider extends ViewOutlineProvider {
    private float mRadius;

    public VideoViewTextureOutlineProvider(float radius) {
        this.mRadius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        int leftMargin = 0;
        int topMargin = 0;
        Rect selfRect = new Rect(leftMargin, topMargin,
                rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
        outline.setRoundRect(selfRect, mRadius);
    }
}
