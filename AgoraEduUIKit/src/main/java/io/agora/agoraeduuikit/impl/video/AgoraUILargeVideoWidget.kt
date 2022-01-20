package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.EduContextMirrorMode.ENABLED
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraUILargeVideoWidget : AgoraBaseWidget(), IAgoraUIVideoListener {
    private var context: Context? = null
    private var videoLayout: ViewGroup? = null
    var largeVideoArt: AgoraUILargeVideoArt? = null
    private var remoteUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var localUserDetailInfo: AgoraUIUserDetailInfo? = null
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

    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {//init
        super.init(parent, width, height, top, left)
        context = parent.context
        videoLayout = LinearLayout(parent.context)
        parent.addView(videoLayout, width, height)
        val videoParams = videoLayout?.layoutParams as ViewGroup.MarginLayoutParams
        videoParams.leftMargin = left
        videoParams.topMargin = top
        videoLayout?.layoutParams = videoParams
        (videoLayout as LinearLayout)?.orientation = LinearLayout.VERTICAL
        setRect(Rect(left, top, width, height))
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        largeVideoListener?.onRendererContainer(EduContextRenderConfig(mirrorMode = ENABLED),
            viewGroup, streamUuid)
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        return when {
            localUserDetailInfo?.streamUuid == streamUuid -> {
                true
            }
            remoteUserDetailInfo?.streamUuid == streamUuid -> {
                false
            }
            else -> {
                false
            }
        }
    }

    fun show(show: Boolean) {
        videoLayout?.post {
            videoLayout?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraLargeWindowInteractionPacket? = Gson().fromJson(message, AgoraLargeWindowInteractionPacket::class.java)
        packet?.let {
            if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowShowed) {
                (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                    //拿到userDetailInfo显示大窗
                    if (lastShowedUserInfo != null && lastShowedUserInfo?.userUuid != userDetailInfo.userUuid) {
                        //当前大窗已经打开的情况，切换其他用户打开大窗
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
                    } else if (lastShowedUserInfo != null && lastShowedUserInfo?.userUuid == userDetailInfo.userUuid) {
                        return
                    }
                    val packet = AgoraLargeWindowInteractionPacket(
                        AgoraLargeWindowInteractionSignal.LargeWindowStartRender, userDetailInfo)
                    sendMessage(Gson().toJson(packet))//通知小窗关闭
                    videoLayout?.post {
                        largeVideoArt = AgoraUILargeVideoArt(context!!, videoLayout!!, 0.0f, 0.0f, 1f, null)
                        largeVideoArt?.videoListener = this
                        largeVideoArt?.upsertUserDetailInfo(userDetailInfo)
                        lastShowedUserInfo = userDetailInfo
                        largeVideoListener?.onLargeVideoShow(userDetailInfo.streamUuid)
                    }
                } ?: Runnable {
                    AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                }
            } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowClosed) {
                (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                    largeVideoArt?.upsertUserDetailInfo(null)
                    videoLayout?.post {
                        videoLayout!!.removeAllViews()
                    }
                    var userInfo = userDetailInfo
                    val packet = AgoraLargeWindowInteractionPacket(
                        AgoraLargeWindowInteractionSignal.LargeWindowStopRender, userInfo)
                    sendMessage(Gson().toJson(packet))
                    lastShowedUserInfo = null
                    largeVideoListener?.onLargeVideoDismiss(userDetailInfo.streamUuid)
                } ?: Runnable {
                    AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                }

            } else {

            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
    }

    fun setRect(rect: Rect) {
        videoLayout?.post {
            val params = videoLayout?.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            videoLayout?.layoutParams = params
        }
    }

    interface IAgoraUILargeVideoListener {
        fun onLargeVideoShow(streamUuid: String)
        fun onLargeVideoDismiss(streamUuid: String)
        fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String)
    }
}