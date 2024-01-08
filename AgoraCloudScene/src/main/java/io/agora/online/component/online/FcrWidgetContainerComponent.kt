package io.agora.online.component.online

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.widget.FcrWidgetInfoListener
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager
import io.agora.agoraeducore.core.internal.transport.OnAgoraTransportListener
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineWidgetContainerComponentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal

/**
 * author : felix
 * date : 2023/6/14
 * description : widget container component
 */
class FcrWidgetContainerComponent : AbsAgoraEduComponent, FcrWidgetInfoListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineWidgetContainerComponentBinding.inflate(LayoutInflater.from(context), this, true)
    private var zIndex = 1f
    private var TAG = "WidgetContainerComponent"

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.fcrWebview.initView(agoraUIProvider)
        binding.fcrPolling.initView(agoraUIProvider)
        binding.fcrCountdown.initView(agoraUIProvider)
        binding.fcrQuiz.initView(agoraUIProvider)

        binding.fcrWebview.setWidgetInfoListener(this)
        binding.fcrPolling.setWidgetInfoListener(this)
        binding.fcrCountdown.setWidgetInfoListener(this)
        binding.fcrQuiz.setWidgetInfoListener(this)

        AgoraTransportManager.addListener(
            AgoraTransportEventId.EVENT_ID_WHITEBOARD_UI,
            object : OnAgoraTransportListener {
                override fun onTransport(event: AgoraTransportEvent) {
                    val params = binding.fcrWhiteboardControl.layoutParams
                    if (event.arg2 == true) {
                        params.width = context.resources.getDimensionPixelOffset(R.dimen.fcr_quick_start_2)
                    } else {
                        params.width = context.resources.getDimensionPixelOffset(R.dimen.fcr_quick_start_1)
                    }
                    binding.fcrWhiteboardControl.layoutParams = params
                }
            })

        eduCore?.eduContextPool()?.widgetContext()
            ?.addWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)

        binding.fcrWebview.setDragOnClickListener {
            setZIndex(binding.fcrWebview)
        }

        binding.fcrPolling.setDragOnClickListener {
            setZIndex(binding.fcrPolling)
        }

        binding.fcrCountdown.setDragOnClickListener {
            setZIndex(binding.fcrCountdown)
        }

        binding.fcrCountdown.setDragOnClickListener {
            setZIndex(binding.fcrCountdown)
        }

        binding.fcrQuiz.setDragOnClickListener {
            setZIndex(binding.fcrQuiz)
        }

        binding.fcrWebviewBtn.setOnClickListener {
            it.visibility = View.GONE
            if (binding.fcrWebview.visibility == View.VISIBLE) {
                binding.fcrWebview.visibility = View.GONE
            } else {
                binding.fcrWebview.visibility = View.VISIBLE
            }
        }

        binding.fcrPollingBtn.setOnClickListener {
            it.visibility = View.GONE
            if (binding.fcrPolling.visibility == View.VISIBLE) {
                binding.fcrPolling.visibility = View.GONE
            } else {
                binding.fcrPolling.visibility = View.VISIBLE
            }
        }
    }

    fun setZIndex(view: View) {
        if (view.z < zIndex) {
            zIndex++
            view.z = zIndex
        }
    }

    override fun release() {
        super.release()
        binding.fcrPolling.release()
        binding.fcrCountdown.release()
        binding.fcrQuiz.release()
        binding.fcrWebview.release()
        eduCore?.eduContextPool()?.widgetContext()
            ?.removeWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        AgoraTransportManager.removeListener(AgoraTransportEventId.EVENT_ID_WHITEBOARD_UI)
    }

    override fun onWidgetUpdate(isShow: Boolean, widgetId: String, count: Int) {
        when (widgetId) {
            AgoraWidgetDefaultId.Polling.id -> {
                switchUI(binding.fcrPolling, binding.fcrPollingBtn, isShow, count)
            }

            AgoraWidgetDefaultId.FcrWebView.id, AgoraWidgetDefaultId.FcrMediaPlayer.id -> {
                binding.fcrWebviewBtn.text = "File($count)"
                switchUI(binding.fcrWebview, binding.fcrWebviewBtn, isShow, count)
            }
            else->{}
        }
    }

    override fun onActiveWidget(widgetId: String) {
        when (widgetId) {
            AgoraWidgetDefaultId.Polling.id -> {
                setViewZIndex(binding.fcrPolling)
            }
            AgoraWidgetDefaultId.Selector.id -> {
                setViewZIndex(binding.fcrQuiz)
            }
            AgoraWidgetDefaultId.CountDown.id -> {
                setViewZIndex(binding.fcrCountdown)
            }
            AgoraWidgetDefaultId.FcrWebView.id, AgoraWidgetDefaultId.FcrMediaPlayer.id -> {
                setViewZIndex(binding.fcrWebview)
            }
            else->{}
        }
    }

    fun setViewZIndex(viewContent: View) {
        if (viewContent.z <= 0) {
            setZIndex(viewContent)
        }
    }

    fun switchUI(viewContent: View, viewQuick: View, isShow: Boolean, count: Int) {
        if (isShow) {
            viewContent.visibility = View.VISIBLE
            viewQuick.visibility = View.GONE
        } else {
            viewContent.visibility = View.GONE

            if (count > 0) {
                viewQuick.visibility = View.VISIBLE
            } else {
                viewQuick.visibility = View.GONE
            }
        }
    }

    private val whiteBoardWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet2 = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet2.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                    if (localUser.role == AgoraEduContextUserRole.Student) {
                        var granted = false
                        if (packet2.body is MutableList<*>) { // 白板开关的格式
                            granted = (packet2.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                        } else { // 白板授权的格式
                            val bodyStr = GsonUtil.gson.toJson(packet2.body)
                            val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                            if (agoraBoard.granted) {
                                granted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                            }
                        }

                        ContextCompat.getMainExecutor(context).execute {
                            if (granted) {
                                binding.fcrWhiteboardControl.visibility = View.VISIBLE
                            } else {
                                binding.fcrWhiteboardControl.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }
}