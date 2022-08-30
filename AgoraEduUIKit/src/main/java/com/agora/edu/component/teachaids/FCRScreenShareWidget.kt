package com.agora.edu.component.teachaids

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextRenderMode
import io.agora.agoraeducore.core.context.EduContextScreenShareState
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.agoraeduuikit.databinding.AgoraEduScreenShareComponetBinding
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : wufang
 * date : 2022/3/24
 * description :
 */
class FCRScreenShareWidget : AgoraBaseWidget() {
    override val tag = "AgoraCountDownWidget"

    private var screenShareContent: AgoraEduScreenShareComponent? = null

    init {
    }

//    fun getWidgetMsgObserver(): AgoraWidgetMessageObserver? {
//        return screenShareContent?.screenShareObserver
//    }

    override fun init(container: ViewGroup) {
        super.init(container)
        container.post {
            widgetInfo?.localUserInfo?.let {
                screenShareContent = AgoraEduScreenShareComponent(container, it,container.context)
            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>, operator: EduBaseUserInfo?) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator = null)
    }

    override fun onWidgetRoomPropertiesDeleted(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
    }

    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)

        val packet: AgoraLargeWindowInteractionPacket? = Gson().fromJson(message, AgoraLargeWindowInteractionPacket::class.java)
        packet?.let {
            if (packet.signal == AgoraLargeWindowInteractionSignal.ScreenShareOpened) {
                (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                    //拿到userDetailInfo


                    screenShareContent?.updateScreenShareState(EduContextScreenShareState.Start,userDetailInfo.streamUuid)
                } ?: Runnable {
                    AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                }
            }
        }

    }


    override fun release() {
        screenShareContent?.dispose()
        super.release()
    }

    private inner class ScreenShareWidgetContent(val container: ViewGroup, val localUserInfo: AgoraWidgetUserInfo) {
        private val tag = "ScreenShareWidgetContent"

        private var binding: AgoraEduScreenShareComponetBinding = AgoraEduScreenShareComponetBinding.inflate(LayoutInflater.from(container.context),
            container, true)
        private val cardView: CardView = binding.cardView

        private var mCountdownStarted = false
        private var mCountdownInitialState = false

        //        private var actionIsRestart = false
        val screenShareObserver = object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val (signal, body) = Gson().fromJson(msg, AgoraBoardInteractionPacket::class.java)
                if (signal.value == AgoraBoardInteractionSignal.BoardGrantDataChanged.value) {
                    // TODO 这里数据格式变了，请修改
//                    val granted = (body as ArrayList<*>).contains(localUserInfo.userUuid)
//                setDraggable(granted)
                }
            }
        }


        fun dispose() {
            ContextCompat.getMainExecutor(container.context).execute {
                container.removeView(binding.root)
            }
        }

    }


     inner class AgoraEduScreenShareComponent : AbsAgoraEduComponent, View.OnTouchListener {
        constructor(context: Context) : super(context)
        constructor(context: Context, attr: AttributeSet) : super(context, attr)
        constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)
        constructor( container: ViewGroup, localUserInfo: AgoraWidgetUserInfo,context: Context) : super(context)


        private var binding: AgoraEduScreenShareComponetBinding = AgoraEduScreenShareComponetBinding.inflate(LayoutInflater.from(container?.context),
            container, true)
        private val cardView: CardView = binding.cardView

        override fun initView(agoraUIProvider: IAgoraUIProvider) {
            super.initView(agoraUIProvider)
//        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_screen_share_view_corner)
//        binding.cardView.radius = radius.toFloat()
        }


        @SuppressLint("ClickableViewAccessibility")
        fun updateScreenShareState(state: EduContextScreenShareState, streamUuid: String) {
            uiHandler.post {
                val sharing = state == EduContextScreenShareState.Start
                binding.cardView.visibility = if (sharing) VISIBLE else GONE

                if (sharing) {
                    cardView.setOnTouchListener(this)
                    eduContext?.mediaContext()?.startRenderVideo(
                        EduContextRenderConfig(renderMode = EduContextRenderMode.FIT),  binding.screenShareContainerLayout, streamUuid
                    )
                } else {
                    cardView.setOnTouchListener(null)
                    eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
                }
            }
        }

        fun dispose() {
            container?.handler?.post {
                container?.removeView(binding.root)
            }
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return true
        }
    }
}


























