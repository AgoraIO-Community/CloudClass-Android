package io.agora.online.component.online

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.databinding.FcrOnlineWidgetPollingComponentBinding

/**
 * author : felix
 * date : 2023/6/20
 * description : 答题器容器
 */
class FcrCountDownWidgetComponent : FcrBaseWidgetComponent {
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
        return listOf(AgoraWidgetDefaultId.CountDown.id)
    }

    override fun createWidget(widgetId: String) {
        binding.dragLayout.removeAllViews()
        binding.dragLayout.resetPosition()
        createWidget(widgetId, binding.dragLayout)
        widgetListener?.onActiveWidget(widgetId)
    }

    override fun removeWidget(widgetId: String) {
        widgetsMap.remove(widgetId)?.release()
        binding.dragLayout.removeAllViews()
    }
}