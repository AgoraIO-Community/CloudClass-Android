package com.agora.edu.component.options

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.agora.edu.component.common.AbsAgoraComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.view.FixPopupWindow
import io.agora.agoraeduuikit.R

/**
 * author : wf
 * date : 2022/2/8 20:09 上午
 * description :
 */
open class AgoraEduHandUpToastPopupComponent(var context: Context) : AbsAgoraComponent {
    open var popupWindow: PopupWindow? = null
    open var popupContainerView: View? = null
    open var agoraHandsUpToastComponent: AgoraEduHandUpToastComponent? = null
    private val timerLimit = 3000L
    private val timerInterval = 1000L
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        popupContainerView = LayoutInflater.from(context).inflate(R.layout.agora_edu_handsup_toast_popup_component, null, false)
        agoraHandsUpToastComponent = popupContainerView?.findViewById(R.id.agora_handsup_toast_component)
        agoraHandsUpToastComponent?.initView(agoraUIProvider)
        popupWindow = FixPopupWindow(popupContainerView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        cancelCountDownTimer.start()
    }

    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            popupWindow?.dismiss()
        }

        override fun onTick(millisUntilFinished: Long) {

        }
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