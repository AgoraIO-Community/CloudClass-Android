package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.widget.LinearLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraUIVideoGroupArt(
    context: Context,
    private val eduContext: EduContextPool?,
    parent: ViewGroup,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    margin: Int,
    mode: EduContextVideoMode = EduContextVideoMode.Single) : AbsComponent(), IAgoraUIVideoListener {
    private val tag = "AgoraUIVideoGroup"

    private val videoLayout = LinearLayout(context)
    private var remoteVideo: AgoraUIVideoArt2? = null
    private var localVideo: AgoraUIVideoArt2? = null

    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var remoteUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var localUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var largeWindowOpened: Boolean = false

    private val largeWindowObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            if (id == AgoraWidgetDefaultId.LargeWindow.id) {
                val packet = Gson().fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
                if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender) {
                    //大窗停止渲染，小窗恢复渲染
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo显示大窗
                        if (userDetailInfo.role == Teacher) {
                            largeWindowOpened = false
                            remoteVideo?.upsertUserDetailInfo(userDetailInfo, eduContext!!, !largeWindowOpened)
                        }
                    } ?: Runnable {
                        AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
                    //大窗开始渲染，小窗显示占位图
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo
                        if (userDetailInfo.role == Teacher) {
                            largeWindowOpened = true
                            remoteVideo?.upsertUserDetailInfo(userDetailInfo, eduContext!!, !largeWindowOpened)
                        }
                    } ?: Runnable {
                        Constants.AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                }
            }
        }
    }

    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            val localIsTeacher = localUserInfo?.role == Teacher
            userList.forEach {
                if ((it.role == Teacher && localIsTeacher) || it.role == Student && !localIsTeacher) {
                    // check duplicate data
                    if (localUserDetailInfo == it) {
                        return@forEach
                    }
                    localUserDetailInfo = it
                    localVideo?.upsertUserDetailInfo(it, eduContext)
                } else if ((it.role == Teacher && !localIsTeacher) || it.role == Student && localIsTeacher) {
                    if (remoteUserDetailInfo == it) {
                        return@forEach
                    }
                    remoteUserDetailInfo = it
                    remoteVideo?.upsertUserDetailInfo(it, eduContext, !largeWindowOpened)
                }
            }
            val a = localIsTeacher && userList.find { it.role == Student } == null
            val b = !localIsTeacher && userList.find { it.role == Teacher } == null
            if (a || b) {
                remoteUserDetailInfo = null
                remoteVideo?.upsertUserDetailInfo(null, eduContext)
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == localUserDetailInfo?.streamUuid || streamUuid == "0") {
                localVideo?.updateAudioVolumeIndication(volume, streamUuid)
            } else if (streamUuid == remoteUserDetailInfo?.streamUuid) {
                remoteVideo?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    init {
        localUserInfo = eduContext?.userContext()?.getLocalUserInfo()

        parent.addView(videoLayout, width, height)
        val videoParams = videoLayout.layoutParams as ViewGroup.MarginLayoutParams
        videoParams.leftMargin = left
        videoParams.topMargin = top
        videoLayout.layoutParams = videoParams
        videoLayout.orientation = LinearLayout.VERTICAL

        val remoteLayout = LinearLayout(context)
        remoteLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        videoLayout.addView(remoteLayout)
        remoteVideo = AgoraUIVideoArt2(context, remoteLayout, eduContext, 0.0f, 0.0f, 1f, localUserInfo)
        remoteVideo?.videoListener = this

        if (mode == EduContextVideoMode.Pair) {
            val localLayout = LinearLayout(context)
            localLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            videoLayout.addView(localLayout)
            val params = localLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = margin
            localLayout.layoutParams = params
            localVideo = AgoraUIVideoArt2(context, localLayout, eduContext, 0.0f, 0.0f, 1f, localUserInfo)
            localVideo?.videoListener = this
        }
        eduContext?.widgetContext()?.addWidgetMessageObserver(
            largeWindowObserver, AgoraWidgetDefaultId.LargeWindow.id)
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        AgoraLog.d(tag, "onAudioUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Audio)
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        AgoraLog.d(tag, "onVideoUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Video)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        val isLocal = isLocalStream(streamUuid)
        if (noneView && isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (noneView && !isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (!noneView && isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid)
        } else if (!noneView && !isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid)
        }
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
        videoLayout.post {
            videoLayout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun setRect(rect: Rect) {
        videoLayout.post {
            val params = videoLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            videoLayout.layoutParams = params
        }
    }
}