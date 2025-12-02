package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.util.AttributeSet
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraConfigComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeduuikit.config.FcrUIConfig
import io.agora.agoraeduuikit.config.FcrUIConfigFactory

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
