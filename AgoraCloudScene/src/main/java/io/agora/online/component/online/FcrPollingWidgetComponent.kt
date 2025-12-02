package io.agora.online.component.online

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineWidgetPollingComponentBinding

/**
 * author : felix
 * date : 2023/6/20
 * description : 投票器容器
 */
class FcrPollingWidgetComponent : FcrBaseWidgetComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineWidgetPollingComponentBinding.inflate(LayoutInflater.from(context), this, true)
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.dragLayout.initView(agoraUIProvider)
        binding.dragLayout.setOnDragTouchListener(onDragClickListener)
    }

    override fun getRegisterWidgetIds(): List<String> {
        return listOf(AgoraWidgetDefaultId.Polling.id)
    }

    override fun createWidget(widgetId: String) {
        binding.dragLayout.removeAllViews()
        binding.dragLayout.resetPosition()
        val widget = createWidget(widgetId, binding.dragLayout)
        widget?.onReceiveMessageForWidget = {
            // hidden view
            widgetListener?.onWidgetUpdate(false, widgetId, 1)
        }
        widgetListener?.onWidgetUpdate(true, widgetId, 1)
        widgetListener?.onActiveWidget(widgetId)
    }

    override fun removeWidget(widgetId: String) {
        ToastManager.showShort(context, R.string.fcr_room_tips_end_poll)
        widgetsMap.remove(widgetId)?.release()
        binding.dragLayout.removeAllViews()
        widgetListener?.onWidgetUpdate(false, widgetId, 0)
    }
}