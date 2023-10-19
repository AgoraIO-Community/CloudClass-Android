package com.agora.edu.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.FcrNetWorkUtils
import com.agora.edu.component.view.FixPopupWindow
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextNetworkQuality
import io.agora.agoraeducore.core.context.FcrMediaPacketStats
import io.agora.agoraeducore.core.context.FcrPerformanceInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.R

/**
 * author : felix
 * date : 2023/7/28
 * description :
 */
class FcrPrivatePopupComponent(var context: Context) {
    lateinit var viewRoot: View
    lateinit var popupWindow: PopupWindow
    lateinit var textContentView: TextView

    fun initView() {
        viewRoot = LayoutInflater.from(context).inflate(R.layout.fcr_edu_private_component, null, false)

        textContentView = viewRoot.findViewById(R.id.fcr_text)

        popupWindow = FixPopupWindow(
            viewRoot,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = false
        popupWindow.isFocusable = false
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setText(content: String) {
        textContentView.text = content
    }

    fun getPopupWindowWidth(): Int {
        viewRoot.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return -(viewRoot.measuredWidth ?: 0)
    }

    fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        popupWindow.showAsDropDown(anchor, xoff, yoff, gravity)
    }

    fun showAsDropDown(anchor: View?) {
        popupWindow.showAsDropDown(anchor)
    }

    fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        popupWindow.showAsDropDown(anchor, xoff, yoff)
    }

    fun isShow(): Boolean {
        return popupWindow.isShowing
    }

    fun dismiss() {
        popupWindow.dismiss()
    }

}