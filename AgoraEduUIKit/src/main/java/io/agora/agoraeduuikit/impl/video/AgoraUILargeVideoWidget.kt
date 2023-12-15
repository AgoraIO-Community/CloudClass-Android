package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduVideoComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraRenderUtils
import com.agora.edu.component.view.FcrDragTouchGroupView
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextRenderMode
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraUILargeVideoWidget : AgoraBaseWidget(), IAgoraUIVideoListener, FcrDragTouchGroupView.OnDoubleClickListener {
    private var context: Context? = null
    private var videoLayout: ViewGroup? = null//用来放大窗view或者屏幕共享view
    var largeVideoView: AgoraEduVideoComponent? = null
    private var lastShowedUserInfo: AgoraUIUserDetailInfo? = null
    var largeVideoListener: IAgoraUILargeVideoListener? = null
    var uiDataProvider: IAgoraUIProvider? = null

    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            userList.find { it.userUuid == lastShowedUserInfo?.userUuid }?.let { userInfo ->
                largeVideoView?.upsertUserDetailInfo(userInfo)
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == lastShowedUserInfo?.streamUuid) {
                largeVideoView?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }

        override fun onUserHandsWave(userUuid: String, duration: Int, payload: Map<String, Any>?) {
            updateUserWaveState(userUuid, true)
        }

        override fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?) {
            updateUserWaveState(userUuid, false)
        }
    }

    private fun updateUserWaveState(userUuid: String, waving: Boolean) {
        largeVideoView?.updateWaveState(userUuid, waving)
    }

    override fun init(parent: ViewGroup) {//parent: largeWindowContainer
        super.init(parent)
        context = parent.context
        videoLayout = LinearLayout(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        parent.addView(videoLayout, layoutParams)
    }

    override fun release() {
        this.context?.let {
            ContextCompat.getMainExecutor(it).execute {
                videoLayout?.removeAllViews()
            }
        }
        super.release()
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        // 大窗渲染
        AgoraRenderUtils.renderView(EduContextRenderConfig(EduContextRenderMode.FIT), eduCore, viewGroup, info)
    }

    fun show(show: Boolean) {
        ContextCompat.getMainExecutor(context).execute {
            videoLayout?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    //销毁大窗widget的时候，这里 properties 不置为空，因为destroy大窗发送给小窗消息的时候，需要用到properties里的值
    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
    }

    //通过接收消息更新widget的view
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraLargeWindowInteractionPacket? = Gson().fromJson(message, AgoraLargeWindowInteractionPacket::class.java)
        packet?.let {
            val bodyStr = GsonUtil.toJson(packet.body)

            when (packet.signal) {
                //打开大窗
                AgoraLargeWindowInteractionSignal.LargeWindowShowed -> {
                    bodyStr?.let {
                        (GsonUtil.jsonToObject<AgoraUIUserDetailInfo>(bodyStr))?.let { userDetailInfo ->
                            //拿到userDetailInfo显示大窗
                            if (lastShowedUserInfo != null && lastShowedUserInfo?.streamUuid != userDetailInfo.streamUuid) {
                                //当前大窗已经打开的情况，切换其他用户打开大窗，多实例大窗可以删除这里的代码
                                largeVideoView?.upsertUserDetailInfo(null)
                                videoLayout?.post {
                                    videoLayout!!.removeAllViews()
                                }
                                val packet = lastShowedUserInfo?.let { it1 ->
                                    AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowStopRender, it1)
                                }
                                if (packet != null) {
                                    sendMessage(GsonUtil.gson.toJson(packet))//大窗StopRender的消息，通知给小窗
                                }
                                lastShowedUserInfo = userDetailInfo
                            } else if (lastShowedUserInfo != null && lastShowedUserInfo?.streamUuid == userDetailInfo.streamUuid) {
                                return
                            }
                            val packet = AgoraLargeWindowInteractionPacket(//通知小窗关闭的msg
                                AgoraLargeWindowInteractionSignal.LargeWindowStartRender, userDetailInfo
                            )
                            sendMessage(GsonUtil.gson.toJson(packet))//通知给小窗
                            videoLayout?.post {
                                largeVideoView = AgoraEduVideoComponent(context!!)
                                uiDataProvider?.let { it1 -> largeVideoView!!.initView(it1) }
                                largeVideoView?.videoListener = this
                                largeVideoView?.upsertUserDetailInfo(userDetailInfo)
                                videoLayout?.addView(
                                    largeVideoView, ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                lastShowedUserInfo = userDetailInfo
                                largeVideoListener?.onLargeVideoShow(userDetailInfo.streamUuid)
                            }
                        } ?: Runnable {
                            LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                        }
                    }
                }
                //关闭大窗
                AgoraLargeWindowInteractionSignal.LargeWindowClosed -> {
                    bodyStr?.let {
                        (GsonUtil.jsonToObject<AgoraUIUserDetailInfo>(bodyStr))?.let { userDetailInfo ->
                            largeVideoView?.upsertUserDetailInfo(null)
                            videoLayout?.post { videoLayout!!.removeAllViews() }
                            val packet = AgoraLargeWindowInteractionPacket(
                                AgoraLargeWindowInteractionSignal.LargeWindowStopRender, userDetailInfo
                            )
                            sendMessage(GsonUtil.gson.toJson(packet))//发送消息给小窗，让小窗渲染
                            lastShowedUserInfo = null
                            largeVideoListener?.onLargeVideoDismiss(userDetailInfo.streamUuid)
                            largeVideoView?.videoListener = null
                            largeVideoView = null
                        } ?: Runnable {
                            LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
    }

    interface IAgoraUILargeVideoListener {
        fun onLargeVideoShow(streamUuid: String)
        fun onLargeVideoDismiss(streamUuid: String)
    }

    override fun onDoubleClick() {
        largeVideoView?.onDoubleClick()
    }

    override fun onClick() {
        largeVideoView?.onClick()
    }
}