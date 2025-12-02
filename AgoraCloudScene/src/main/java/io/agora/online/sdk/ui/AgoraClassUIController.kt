package io.agora.online.sdk.ui

import io.agora.online.helper.AgoraUIDeviceSetting
import com.google.gson.Gson
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.online.provider.UIDataProvider
import io.agora.online.provider.UIDataProviderListenerImpl

/**
 * author : felix
 * date : 2022/1/26
 * description :
 */
class AgoraClassUIController {
    var uiDataProvider: UIDataProvider? = null
    var eduCore: AgoraEduCore? = null
    var eduContext: EduContextPool? = null

    init {
        AgoraUIDeviceSetting.setFrontCamera(true)
    }

    fun init(eduCore: AgoraEduCore?) {
        this.eduCore = eduCore
        eduContext = eduCore?.eduContextPool()
        uiDataProvider = UIDataProvider(eduCore, eduContext)
        uiDataProvider?.addListener(baseUIDataProviderListener)
    }

    fun setGrantedUsers(){
        uiDataProvider?.setGrantedUsers(eduCore)
    }

    private val baseUIDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            val pair = Pair(state, errorCode)
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.RTCAudioMixingStateChanged, pair)
            eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
        }
    }
}