package io.agora.uikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.widget.LinearLayout
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserRole
import io.agora.educontext.EduContextVideoMode
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.uikit.educontext.handlers.VideoHandler
import io.agora.uikit.impl.AbsComponent

class AgoraUIVideoGroup(
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
    private var remoteVideo: AgoraUIVideo? = null
    private var localVideo: AgoraUIVideo? = null
    private var remoteUserDetailInfo: EduContextUserDetailInfo? = null
    private var localUserDetailInfo: EduContextUserDetailInfo? = null

    private val videoGroupHandler = object : VideoHandler() {
        override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {
            if (info.user.role == EduContextUserRole.Teacher) {
                remoteUserDetailInfo = info
                remoteVideo?.upsertUserDetailInfo(info)
            } else if (info.user.role == EduContextUserRole.Student) {
                localUserDetailInfo = info
                localVideo?.upsertUserDetailInfo(info)
            }
        }

        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            if (streamUuid == remoteUserDetailInfo?.streamUuid) {
                remoteVideo?.updateAudioVolumeIndication(volume, streamUuid)
            } else if (streamUuid == localUserDetailInfo?.streamUuid) {
                localVideo?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    init {
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
        remoteVideo = AgoraUIVideo(context, remoteLayout, 0.0f, 0.0f, 0f)
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
            localVideo = AgoraUIVideo(context, localLayout, 0.0f, 0.0f, 0f)
            localVideo?.videoListener = this
        }

        eduContext?.videoContext()?.addHandler(videoGroupHandler)
    }

    fun updateUserDetailInfo(info: EduContextUserDetailInfo) {
        eduContext?.videoContext()?.getHandlers()?.forEach { handler ->
            handler.onUserDetailInfoUpdated(info)
        }
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        eduContext?.videoContext()?.getHandlers()?.forEach { handler ->
            handler.onVolumeUpdated(value, streamUuid)
        }
    }

    override fun onUpdateVideo(enable: Boolean) {
        eduContext?.videoContext()?.updateVideo(enable)
    }

    override fun onUpdateAudio(enable: Boolean) {
        eduContext?.videoContext()?.updateAudio(enable)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        eduContext?.videoContext()?.renderVideo(viewGroup, streamUuid)
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