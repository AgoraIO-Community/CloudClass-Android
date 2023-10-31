package com.agora.edu.component.view

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.widget.PopupWindow

/**
 * author : felix
 * date : 2022/2/7
 * description : 兼容高版本 PopupWindow bug
 */
class FixPopupWindow(contentView: View?, width: Int, height: Int, focusable: Boolean) :
    PopupWindow(contentView, width, height, focusable) {

    override fun showAsDropDown(anchor: View?) {
        if (Build.VERSION.SDK_INT == 24 && anchor != null) {
            val rect = Rect()
            anchor.getGlobalVisibleRect(rect)
            val h = anchor.resources.displayMetrics.heightPixels - rect.bottom
            height = h
        }
        super.showAsDropDown(anchor)
    }
}