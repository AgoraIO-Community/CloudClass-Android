package io.agora.online.impl.video

import android.content.Context
import android.util.AttributeSet
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraConfigComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.config.FcrUIConfig
import io.agora.online.config.FcrUIConfigFactory

/**
 * author : felix
 * date : 2022/1/20
 * description : 基础组件Component
 */
abstract class AbsAgoraEduConfigDialog<T> : AbsAgoraEduComponent, IAgoraConfigComponent<T> {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        updateUIForConfig(getUIConfig())
    }

    abstract fun updateUIForConfig(config: T?)

    fun getModuleUIConfig(): FcrUIConfig? {
        eduCore?.config?.roomType?.let {
            return FcrUIConfigFactory.getConfig(it)
        }
        return null
    }
}
