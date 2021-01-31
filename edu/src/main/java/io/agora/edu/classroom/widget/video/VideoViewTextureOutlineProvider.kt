package io.agora.edu.classroom.widget.video

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider
import io.agora.edu.R

class VideoViewTextureOutlineProvider(private val mRadius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        val leftMargin = 0
        val topMargin = 0
        val selfRect = Rect(leftMargin, topMargin,
                rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin)
        outline.setRoundRect(selfRect, mRadius)
    }
}