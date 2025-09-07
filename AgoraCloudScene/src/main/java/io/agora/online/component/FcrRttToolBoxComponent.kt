package io.agora.online.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetRoomPropsUpdateReq
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.databinding.FcrOnlineEduRttComponentBinding
import io.agora.online.options.AgoraEduOptionsComponent
import io.agora.online.options.AgoraEduRttOptionsComponent
import io.agora.online.widget.FcrWidgetManager.WIDGETS_RTT_ID
import io.agora.online.widget.rtt.FcrRttToolBoxWidget

class FcrRttToolBoxComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)


    private var binding: FcrOnlineEduRttComponentBinding = FcrOnlineEduRttComponentBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * widget插件
     */
    private var widget: FcrRttToolBoxWidget? = null

    /**
     * 界面上设置好的布局
     */
    private var conversionStatusView: ViewGroup? = null
    private var subtitleView: AgoraEduRttOptionsComponent? = null
    private var agoraEduOptionsComponent: AgoraEduOptionsComponent? = null

    /**
     * 插件注册监听
     */
    private val widgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            if (widget == null) {
                val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(widgetId)
                widgetConfig?.let { config ->
                    widget = eduContext?.widgetContext()?.create(config) as FcrRttToolBoxWidget?
                    widget?.init(binding.root, agoraUIProvider, agoraEduOptionsComponent!!, conversionStatusView!!, subtitleView!!)
                }
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            ContextCompat.getMainExecutor(binding.root.context).execute { widget?.release() }
        }
    }

    /**
     * 重置工具准提
     */
    fun resetEduRttToolBoxStatus() {
        widget?.resetEduRttToolBoxStatus()
    }

    fun initView(
        agoraUIProvider: IAgoraUIProvider, agoraEduOptionsComponent: AgoraEduOptionsComponent, conversionStatusView: ViewGroup,
        subtitleView: AgoraEduRttOptionsComponent,
    ) {
        super.initView(agoraUIProvider)
        this.agoraEduOptionsComponent = agoraEduOptionsComponent
        this.conversionStatusView = conversionStatusView
        this.subtitleView = subtitleView
        eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, WIDGETS_RTT_ID)
        eduContext?.widgetContext()?.setWidgetActive(WIDGETS_RTT_ID, AgoraWidgetRoomPropsUpdateReq(state = 1))
    }
}
