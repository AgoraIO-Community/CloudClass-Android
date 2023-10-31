package io.agora.online.options

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import io.agora.online.component.common.AbsAgoraComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.view.FixPopupWindow
import io.agora.online.R

/**
 * author : wufang
 * date : 2022/1/19
 * description : 正在举手的学生列表的包装类
 */
class AgoraEduHandsUpListPopupComponent(var context: Context) : AbsAgoraComponent {

    open var popupWindow: PopupWindow? = null
    open var popupContainerView: View? = null
    open var agoraHandsupListPopup: AgoraEduHandsUpListComponent? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        popupContainerView = LayoutInflater.from(context).inflate(R.layout.fcr_online_edu_handsup_list_component, null, false)
        agoraHandsupListPopup = popupContainerView?.findViewById(R.id.agora_handsup_list_widget)
        agoraHandsupListPopup?.initView(agoraUIProvider)
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
        agoraHandsupListPopup?.visibility = FrameLayout.GONE
        popupContainerView?.visibility = FrameLayout.GONE
        popupWindow?.dismiss()
    }

    override fun release() {

    }

}