package com.agora.edu.component.common

import android.content.Context
import android.util.AttributeSet
import io.agora.agoraeduuikit.config.FcrUIConfig
import io.agora.agoraeduuikit.config.FcrUIConfigFactory
import io.agora.agoraeduuikit.config.template.FcrDefUIConfig

/**
 * author : felix
 * date : 2022/1/20
 * description : 基础组件Component
 */
abstract class AbsAgoraEduConfigComponent<T> : AbsAgoraEduComponent, IAgoraConfigComponent<T> {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        updateUIForConfig(getUIConfig())
    }

    abstract fun updateUIForConfig(config: T)

    fun getTemplateUIConfig(): FcrUIConfig {
        eduCore?.config?.roomType?.let {
            return FcrUIConfigFactory.getConfig(it)
        }
        return FcrDefUIConfig()
    }
}
