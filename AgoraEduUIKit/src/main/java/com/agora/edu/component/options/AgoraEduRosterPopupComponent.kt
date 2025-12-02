package com.agora.edu.component.options

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.agora.edu.component.common.AbsAgoraComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.view.FixPopupWindow
import io.agora.agoraeduuikit.R

open class AgoraEduRosterPopupComponent(var context: Context) : AbsAgoraComponent {
    open var popupWindow: PopupWindow? = null
    open var popupContainerView: View? = null
    open var agoraRosterWidget: AgoraEduRosterComponent? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        popupContainerView = LayoutInflater.from(context).inflate(R.layout.agora_edu_roster_popup_component, null, false)
        agoraRosterWidget = popupContainerView?.findViewById(R.id.agora_roster_widget)
        agoraRosterWidget?.initView(agoraUIProvider)
        popupWindow = FixPopupWindow(popupContainerView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow?.isOutsideTouchable = false
        popupWindow?.isFocusable = false
        popupWindow?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun getPopupWindowWidth(): Int {
        popupContainerView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return -(popupContainerView?.measuredWidth ?: 0)
    }

    fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        popupWindow?.showAsDropDown(anchor, xoff, yoff, gravity)
    }

    fun showAsDropDown(anchor: View?) {
        popupWindow?.showAsDropDown(anchor)
    }

    fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        popupWindow?.showAsDropDown(anchor, xoff, yoff)
    }

    open fun dismiss() {
        popupWindow?.dismiss()
    }

    override fun release() {

    }
}