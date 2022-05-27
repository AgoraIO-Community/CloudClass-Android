package io.agora.classroom.ui

import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.google.gson.Gson
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

/**
 * author : hefeng
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
        eduContext = eduCore?.eduContextPool()
        uiDataProvider = UIDataProvider(eduContext)
        uiDataProvider?.addListener(baseUIDataProviderListener)
    }

    private val baseUIDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            val pair = Pair(state, errorCode)
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.RTCAudioMixingStateChanged, pair)
            eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
        }

//        override fun onLocalUserKickedOut() {
//            super.onLocalUserKickedOut()
//            // TODO hefeng
//            //kickOut()
//        }

//        override fun onScreenShareStart(info: AgoraUIUserDetailInfo) {
//            // TODO hefeng
////            screenShareWindow?.updateScreenShareState(EduContextScreenShareState.Start, info.streamUuid)
//        }
//
//        override fun onScreenShareStop(info: AgoraUIUserDetailInfo) {
//            // TODO hefeng
////            screenShareWindow?.updateScreenShareState(EduContextScreenShareState.Stop, info.streamUuid)
//        }
    }
}