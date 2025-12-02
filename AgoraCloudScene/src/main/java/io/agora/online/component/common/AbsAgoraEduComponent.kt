package io.agora.online.component.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.online.provider.UIDataProvider

/**
 * author : felix
 * date : 2022/1/20
 * description : 基础组件Component
 */
abstract class AbsAgoraEduComponent : FrameLayout, AbsAgoraComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    protected lateinit var agoraUIProvider: IAgoraUIProvider
    protected var eduCore: AgoraEduCore? = null
    protected var uiDataProvider: UIDataProvider? = null
    protected var eduContext: EduContextPool? = null
    protected var uiHandler = Handler(Looper.getMainLooper())
    protected var roomType: RoomType? = null
    var curMaxZIndex = 0f
    val widgetsMap = LinkedHashMap<String, AgoraBaseWidget>()

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

    /**
     * 判断并切换主线程
     */
    fun runOnUIThread(runnable: Runnable) {
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            runnable.run()
        } else {
            uiHandler.post(runnable)
        }
    }
}
