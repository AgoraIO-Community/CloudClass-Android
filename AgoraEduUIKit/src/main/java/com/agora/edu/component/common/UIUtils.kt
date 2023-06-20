package com.agora.edu.component.common

import android.view.View

/**
 * author : felix
 * date : 2022/7/11
 * description :
 */
class UIUtils {
    companion object {
        fun setViewVisible(view: View, isShow: Boolean) {
            view.visibility = if (isShow) {
                View.VISIBLE
            } else View.GONE
        }

        fun setViewInVisible(view: View, isShow: Boolean) {
            view.visibility = if (isShow) {
                View.VISIBLE
            } else View.INVISIBLE
        }
    }
}