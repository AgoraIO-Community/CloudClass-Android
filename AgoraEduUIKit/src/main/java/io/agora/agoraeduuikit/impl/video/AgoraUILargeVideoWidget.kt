package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.agora.edu.component.AgoraEduScreenShareComponent
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.EduContextMirrorMode.DISABLED
import io.agora.agoraeducore.core.context.EduContextMirrorMode.ENABLED
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextScreenShareState
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraUILargeVideoWidget : AgoraBaseWidget(), IAgoraUIVideoListener {
    private var context: Context? = null
    private var videoLayout: ViewGroup? = null//用来放大窗view或者屏幕共享view
    var largeVideoArt: AgoraUILargeVideoArt? = null
    var screenShareWindow: AgoraEduScreenShareComponent? = null
    private var lastShowedUserInfo: AgoraUIUserDetailInfo? = null
    var largeVideoListener: IAgoraUILargeVideoListener? = null
    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            userList.find { it.userUuid == lastShowedUserInfo?.userUuid }?.let { userInfo ->
                largeVideoArt?.upsertUserDetailInfo(userInfo)
            }
//            val localIsTeacher = localUserInfo?.role == Teacher
//            userList.forEach {
//                if ((it.role == Teacher && localIsTeacher) || it.role == Student && !localIsTeacher) {
//                    // check duplicate data
//                    if (localUserDetailInfo == it) {
//                        return@forEach
//                    }
//                    localUserDetailInfo = it
//                } else if ((it.role == Teacher && !localIsTeacher) || it.role == Student && localIsTeacher) {
//                    if (remoteUserDetailInfo == it) {
//                        return@forEach
//                    }
//                    remoteUserDetailInfo = it
//                    largeVideoArt?.upsertUserDetailInfo(it)
//                }
//            }
//            val a = localIsTeacher && userList.find { it.role == Student } == null
//            val b = !localIsTeacher && userList.find { it.role == Teacher } == null
//            if (a || b) {
//                remoteUserDetailInfo = null
//                largeVideoArt?.upsertUserDetailInfo(null)
//            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == lastShowedUserInfo?.streamUuid) {
                largeVideoArt?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    override fun init(parent: ViewGroup) {//parent: largeWindowContainer
        super.init(parent)
        context = parent.context
        videoLayout = LinearLayout(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        parent.addView(videoLayout, layoutParams)

    }

    override fun release() {
        videoLayout?.handler?.post {
            videoLayout?.removeAllViews()
        }
        super.release()
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        largeVideoListener?.onRendererContainer(
            EduContextRenderConfig(mirrorMode = DISABLED),
            viewGroup, streamUuid
        )
    }

    fun show(show: Boolean) {
        videoLayout?.post {
            videoLayout?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    //通过接收消息更新widget的view
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraLargeWindowInteractionPacket? = Gson().fromJson(message, AgoraLargeWindowInteractionPacket::class.java)
        packet?.let {
            //打开大窗
            when (packet.signal) {
                AgoraLargeWindowInteractionSignal.LargeWindowShowed -> {
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo显示大窗
                        if (lastShowedUserInfo != null && lastShowedUserInfo?.streamUuid != userDetailInfo.streamUuid) {
                            //当前大窗已经打开的情况，切换其他用户打开大窗//todo 多实例大窗可以删除这里的代码
                            largeVideoArt?.upsertUserDetailInfo(null)
                            videoLayout?.post {
                                videoLayout!!.removeAllViews()
                            }
                            val packet = lastShowedUserInfo?.let { it1 ->
                                AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowStopRender, it1)
                            }
                            if (packet != null) {
                                sendMessage(Gson().toJson(packet))//大窗StopRender的消息，通知给小窗
                            }
                            lastShowedUserInfo = userDetailInfo
                        } else if (lastShowedUserInfo != null && lastShowedUserInfo?.streamUuid == userDetailInfo.streamUuid) {
                            return
                        }
                        val packet = AgoraLargeWindowInteractionPacket(//通知小窗关闭的msg
                            AgoraLargeWindowInteractionSignal.LargeWindowStartRender, userDetailInfo
                        )
                        sendMessage(Gson().toJson(packet))//通知给小窗
                        videoLayout?.post {
                            largeVideoArt = AgoraUILargeVideoArt(context!!, videoLayout!!, 1f, null)
                            largeVideoArt?.videoListener = this
                            largeVideoArt?.upsertUserDetailInfo(userDetailInfo)
                            lastShowedUserInfo = userDetailInfo
                            largeVideoListener?.onLargeVideoShow(userDetailInfo.streamUuid)
                        }
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                    //关闭大窗
                }
                AgoraLargeWindowInteractionSignal.LargeWindowClosed -> {
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        largeVideoArt?.upsertUserDetailInfo(null)

                        videoLayout?.post {
                            videoLayout!!.removeAllViews()
                        }
                        val packet = AgoraLargeWindowInteractionPacket(
                            AgoraLargeWindowInteractionSignal.LargeWindowStopRender, userDetailInfo
                        )
                        sendMessage(Gson().toJson(packet))//发送消息给小窗，让小窗渲染
                        lastShowedUserInfo = null
                        largeVideoListener?.onLargeVideoDismiss(userDetailInfo.streamUuid)
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                    //打开屏幕共享
                }
                AgoraLargeWindowInteractionSignal.ScreenShareOpened -> {//todo 这里update就不行？？
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        videoLayout?.post {
                            screenShareWindow = AgoraEduScreenShareComponent(videoLayout!!.context)
                            videoLayout!!.removeAllViews()
                            videoLayout!!.addView(screenShareWindow)
                            screenShareWindow?.updateScreenShareState(EduContextScreenShareState.Start, userDetailInfo.streamUuid)

                        }
                    }
                    //关闭屏幕共享
                }
                AgoraLargeWindowInteractionSignal.ScreenShareClosed -> {
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        videoLayout?.post {
                            if (screenShareWindow != null) {
                                videoLayout!!.removeAllViews()
                                screenShareWindow?.updateScreenShareState(EduContextScreenShareState.Stop, userDetailInfo.streamUuid)
                            }
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
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
    }

    interface IAgoraUILargeVideoListener {
        fun onLargeVideoShow(streamUuid: String)
        fun onLargeVideoDismiss(streamUuid: String)
        fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String)
    }
}