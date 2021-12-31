package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeducore.core.context.EduContextUserUpdateReason.Reward
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler3
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener

class AgoraUIVideoGroupArt(
        context: Context,
        private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        margin: Int,
        mode: io.agora.agoraeducore.core.context.EduContextVideoMode = io.agora.agoraeducore.core.context.EduContextVideoMode.Single) : AbsComponent(), IAgoraUIVideoListener {
    private val tag = "AgoraUIVideoGroup"

    private val videoLayout = LinearLayout(context)
    private var remoteVideo: AgoraUIVideoArt? = null
    private var localVideo: AgoraUIVideoArt? = null

    private var remoteUserInfo: AgoraEduContextUserInfo? = null
    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var remoteStreamInfo: AgoraEduContextStreamInfo? = null
    private var localStreamInfo: AgoraEduContextStreamInfo? = null
    private var remoteUserDetailInfo: EduContextUserDetailInfo? = null
    private var localUserDetailInfo: EduContextUserDetailInfo? = null

//    private val videoGroupHandler = object : VideoHandler() {
//        override fun onUserDetailInfoUpdated(info: io.agora.agoraeducore.corecontext.EduContextUserDetailInfo) {
//            if (info.user.role == io.agora.agoraeducore.corecontext.AgoraEduContextUserRole.Teacher) {
//                remoteUserDetailInfo = info
//                remoteVideo?.upsertUserDetailInfo(info, eduContext)
//            } else if (info.user.role == io.agora.agoraeducore.corecontext.AgoraEduContextUserRole.Student) {
//                localUserDetailInfo = info
//                localVideo?.upsertUserDetailInfo(info, eduContext)
//            }
//        }
//    }

    private val userHandler = object : UserHandler() {
        override fun onRemoteUserJoined(userInfo: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(userInfo)
            remoteUserInfo = userInfo.copy()
            // todo refresh videoWindow
        }

        override fun onRemoteUserLeft(userInfo: AgoraEduContextUserInfo, operator: AgoraEduContextUserInfo?,
                                      reason: EduContextUserLeftReason) {
            super.onRemoteUserLeft(userInfo, operator, reason)
            remoteUserInfo = null
            // todo refresh videoWindow
        }

        override fun onUserPropertiesUpdated(userInfo: AgoraEduContextUserInfo, properties: Map<String, Any>,
                                             cause: Map<String, Any>?, operator: AgoraEduContextUserInfo?) {
            super.onUserPropertiesUpdated(userInfo, properties, cause, operator)
            if (userInfo.userUuid == localUserInfo?.userUuid) {
                localUserInfo = userInfo
//                localVideo?.upsertUserDetailInfo(info)
            } else {
                remoteUserInfo = userInfo.copy()
                // todo refresh videoWindow
            }
        }

        override fun onUserPropertiesDeleted(userInfo: AgoraEduContextUserInfo, keys: List<String>,
                                             cause: Map<String, Any>?, operator: AgoraEduContextUserInfo?) {
            super.onUserPropertiesDeleted(userInfo, keys, cause, operator)
            if (userInfo.userUuid == localUserInfo?.userUuid) {
                localUserInfo = userInfo
//                localVideo?.upsertUserDetailInfo(info)
            } else {
                remoteUserInfo = userInfo.copy()
                // todo refresh videoWindow
            }
        }

        override fun onUserUpdated(userInfo: AgoraEduContextUserInfo, operator: AgoraEduContextUserInfo?,
                                   reason: EduContextUserUpdateReason?) {
            super.onUserUpdated(userInfo, operator, reason)
            if (reason == Reward) {
                if (userInfo.userUuid == localUserInfo?.userUuid) {
                    localVideo?.updateReward()
                } else {
                    remoteVideo?.updateReward()
                }
                // todo refresh videoWindow
            }
        }
    }

    private val mediaHandler = object : MediaHandler3() {
        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            super.onVolumeUpdated(volume, streamUuid)
            if (streamUuid == localStreamInfo?.streamUuid) {
                localVideo?.updateAudioVolumeIndication(volume, streamUuid)
            } else {
                remoteVideo?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }

        override fun onLocalDeviceStateUpdated(deviceInfo: AgoraEduContextDeviceInfo, state: AgoraEduContextDeviceState2) {
            super.onLocalDeviceStateUpdated(deviceInfo, state)
            if (deviceInfo.isCamera()) {
                localStreamInfo?.videoSourceState = AgoraEduContextMediaSourceState.toMediaSourceState(state)
            } else if (deviceInfo.isMic()) {
                localStreamInfo?.audioSourceState = AgoraEduContextMediaSourceState.toMediaSourceState(state)
            }
            // todo refresh videoWindow
        }
    }

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamJoined(streamInfo, operator)
            if (streamInfo.owner.userUuid == localUserInfo?.userUuid) {
                localStreamInfo = streamInfo.copy()
            } else {
                remoteStreamInfo = streamInfo.copy()
            }
            // todo refresh videoWindow
        }

        override fun onStreamUpdated(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamUpdated(streamInfo, operator)
            if (streamInfo.owner.userUuid == localUserInfo?.userUuid) {
                localStreamInfo = streamInfo.copy()
            } else {
                remoteStreamInfo = streamInfo.copy()
            }
            // todo refresh videoWindow
        }

        override fun onStreamLeft(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamLeft(streamInfo, operator)
            if (streamInfo.owner.userUuid == localUserInfo?.userUuid) {
                localStreamInfo = null
            } else {
                remoteStreamInfo = null
            }
            // todo refresh videoWindow
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
        remoteVideo = AgoraUIVideoArt(context, remoteLayout, eduContext, 0.0f, 0.0f, 0f)
        remoteVideo?.videoListener = this

        if (mode == io.agora.agoraeducore.core.context.EduContextVideoMode.Pair) {
            val localLayout = LinearLayout(context)
            localLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            videoLayout.addView(localLayout)
            val params = localLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = margin
            localLayout.layoutParams = params
            localVideo = AgoraUIVideoArt(context, localLayout, eduContext, 0.0f, 0.0f, 0f)
            localVideo?.videoListener = this
        }

        localUserInfo = eduContext?.userContext()?.getLocalUserInfo()
        eduContext?.userContext()?.addHandler(userHandler)
        eduContext?.mediaContext()?.addHandler(mediaHandler)
        eduContext?.streamContext()?.addHandler(streamHandler)
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
        return if (localStreamInfo?.streamUuid == streamUuid) {
            true
        } else if (remoteStreamInfo?.streamUuid == streamUuid) {
            false
        } else {
            false
        }
    }

    fun show(show: Boolean) {
        videoLayout.post {
            videoLayout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        remoteVideo?.setVisibility(visibility, userDetailInfo)
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