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
 * date : 2023/7/18
 * description :
 */
class FcrNetworkPopupComponent(var context: Context) : AbsAgoraComponent {
    lateinit var viewRoot: View
    lateinit var popupWindow: PopupWindow
    lateinit var textDelayView: TextView
    lateinit var textUpLossView: TextView
    lateinit var textDownLossView: TextView
    lateinit var networkStatusView: TextView
    var eduCore: AgoraEduCore? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        viewRoot = LayoutInflater.from(context).inflate(R.layout.fcr_edu_network_component, null, false)

        textDelayView = viewRoot.findViewById(R.id.fcr_tv_network_delay)
        textUpLossView = viewRoot.findViewById(R.id.fcr_tv_network_up_loss)
        textDownLossView = viewRoot.findViewById(R.id.fcr_tv_network_down_loss)
        networkStatusView = viewRoot.findViewById(R.id.fcr_tv_network_status)

        popupWindow = FixPopupWindow(
            viewRoot,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        eduCore = agoraUIProvider.getAgoraEduCore()
        eduCore?.eduContextPool()?.monitorContext()?.addHandler(monitorHandler)
    }

    val monitorHandler = object : MonitorHandler() {
        override fun onLocalNetworkQualityUpdated(quality: EduContextNetworkQuality) {
            super.onLocalNetworkQualityUpdated(quality)
            ContextCompat.getMainExecutor(context).execute {
                networkStatusView.setTextColor(FcrNetWorkUtils.getNetWorkStateColor(context, quality))
                networkStatusView.setText(FcrNetWorkUtils.getNetWorkStateDesc(quality))
            }
        }

        override fun onMediaPacketStatsUpdated(roomUuid: String, stats: FcrMediaPacketStats) {
            super.onMediaPacketStatsUpdated(roomUuid, stats)
            ContextCompat.getMainExecutor(context).execute {
                textDelayView.text = "${stats.lastMileDelay}ms"
                textUpLossView.text = "${stats.txPacketLossRate}%"
                textDownLossView.text = "${stats.rxPacketLossRate}%"
            }
        }
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

    override fun release() {
        eduCore?.eduContextPool()?.monitorContext()?.removeHandler(monitorHandler)
    }
}