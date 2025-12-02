package com.agora.edu.component.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeduuikit.provider.UIDataProvider

/**
 * author : felix
 * date : 2022/1/20
 * description : 基础组件Component
 */
abstract class AbsAgoraEduLinearComponent : LinearLayoutCompat, AbsAgoraComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    protected lateinit var agoraUIProvider: IAgoraUIProvider
    protected var eduCore: AgoraEduCore? = null
    protected var uiDataProvider: UIDataProvider? = null
    protected var eduContext: EduContextPool? = null

    /**
     * 自定义组件，使用到相关数据，必须实现这个方法
     */
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        this.agoraUIProvider = agoraUIProvider
        this.uiDataProvider = agoraUIProvider.getUIDataProvider()
        this.eduCore = agoraUIProvider.getAgoraEduCore()
        this.eduContext = eduCore?.eduContextPool()
    }

    override fun release() {

    }
}
