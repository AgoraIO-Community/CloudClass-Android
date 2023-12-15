package io.agora.online.component.common

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.online.provider.UIDataProvider

/**
 * author : felix
 * date : 2022/2/24
 * description : 基础组件Dialog
 */
abstract class AbsAgoraEduDialog(context: Context) : Dialog(context), AbsAgoraComponent {
    protected lateinit var agoraUIProvider: IAgoraUIProvider
    protected var eduCore: AgoraEduCore? = null
    protected var uiDataProvider: UIDataProvider? = null
    protected var eduContext: EduContextPool? = null
    protected var uiHandler = Handler(Looper.getMainLooper())
    protected var roomType: RoomType? = null

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
