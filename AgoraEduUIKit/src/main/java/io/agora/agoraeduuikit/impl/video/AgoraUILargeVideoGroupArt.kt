package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.ViewGroup
import android.widget.LinearLayout
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Video
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Audio
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener

class AgoraUILargeVideoGroupArt(
        context: Context,
        private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        margin: Int,
        mode: io.agora.agoraeducore.core.context.EduContextVideoMode = io.agora.agoraeducore.core.context.EduContextVideoMode.Single,
        userDetailInfo: EduContextUserDetailInfo?) : AbsComponent(), IAgoraUIVideoListener {
    private val tag = "AgoraUIVideoGroup"

    private val videoLayout = LinearLayout(context)
    private var largeVideoArt: AgoraUILargeVideoArt? = null
    private var remoteUserDetailInfo: EduContextUserDetailInfo? = null
    private var remoteUserInfo: AgoraEduContextUserInfo? = null
    private var remoteStreamInfo: AgoraEduContextStreamInfo? = null

//    private val videoGroupHandler = object : VideoHandler() {
//        override fun onUserDetailInfoUpdated(info: io.agora.edu.core.context.EduContextUserDetailInfo) {
//            remoteUserDetailInfo = info
//            largeVideoArt?.upsertUserDetailInfo(info)
//        }
//
//        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
//
//        }
//    }

    private val userHandler = object : UserHandler() {
        override fun onRemoteUserLeft(userInfo: AgoraEduContextUserInfo, operator: AgoraEduContextUserInfo?,
                                      reason: EduContextUserLeftReason) {
            super.onRemoteUserLeft(userInfo, operator, reason)
            if (userInfo.userUuid == remoteUserInfo?.userUuid) {
                remoteUserInfo = null
//            largeVideoArt?.upsertUserDetailInfo(info)
            }
        }

        override fun onUserPropertiesUpdated(userInfo: AgoraEduContextUserInfo, properties: Map<String, Any>,
                                             cause: Map<String, Any>?, operator: AgoraEduContextUserInfo?) {
            super.onUserPropertiesUpdated(userInfo, properties, cause, operator)
            if (userInfo.userUuid == remoteUserInfo?.userUuid) {
                remoteUserInfo = userInfo.copy()
//                largeVideoArt?.upsertUserDetailInfo(info)
            }
        }

        override fun onUserPropertiesDeleted(userInfo: AgoraEduContextUserInfo, keys: List<String>,
                                             cause: Map<String, Any>?, operator: AgoraEduContextUserInfo?) {
            super.onUserPropertiesDeleted(userInfo, keys, cause, operator)
            if (userInfo.userUuid == remoteUserInfo?.userUuid) {
                remoteUserInfo = userInfo.copy()
//                largeVideoArt?.upsertUserDetailInfo(info)
            }
        }
    }

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamJoined(streamInfo, operator)
            if (streamInfo.owner.userUuid == remoteUserInfo?.userUuid) {
                remoteStreamInfo = streamInfo.copy()
            }
        }

        override fun onStreamUpdated(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamUpdated(streamInfo, operator)
            remoteStreamInfo = streamInfo.copy()
        }

        override fun onStreamLeft(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamLeft(streamInfo, operator)
            if (streamInfo.owner.userUuid == remoteUserInfo?.userUuid) {
                remoteStreamInfo = null
            }
        }
    }

    init {
        parent.addView(videoLayout, width, height)//设置宽高
        val videoParams = videoLayout.layoutParams as ViewGroup.MarginLayoutParams
        videoParams.leftMargin = left //设置坐标 x
        videoParams.topMargin = top
        videoLayout.layoutParams = videoParams
        videoLayout.orientation = LinearLayout.VERTICAL

        val remoteLayout = LinearLayout(context)
        remoteLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        videoLayout.addView(remoteLayout)
        largeVideoArt = AgoraUILargeVideoArt(context, remoteLayout, 0.0f, 0.0f, 0f)
        largeVideoArt?.videoListener = this

        eduContext?.userContext()?.addHandler(userHandler)
        eduContext?.streamContext()?.addHandler(streamHandler)
    }

    fun updateUserDetailInfo(info: EduContextUserDetailInfo) {
//        eduContext?.videoContext()?.getHandlers()?.forEach { handler ->
//            handler.onUserDetailInfoUpdated(info)
//        }
        // todo what should i do?
//        remoteUserInfo = info
//        eduContext?.userContext3()?.
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
//        eduContext?.videoContext()?.getHandlers()?.forEach { handler ->
//            handler.onVolumeUpdated(value, streamUuid)
//        }
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        // todo 老师不能把自己的小窗视频拉下来进去大窗进行编辑吗？
        remoteStreamInfo?.let {
            if (enable) {
                //eduContext?.streamContext()?.publishStreams(arrayOf(it.streamUuid).toMutableList(), Video)
            } else {
                //eduContext?.streamContext()?.muteStreams(arrayOf(it.streamUuid).toMutableList(), Video)
            }
        }
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        remoteStreamInfo?.let {
            if (enable) {
                //eduContext?.streamContext()?.publishStreams(arrayOf(it.streamUuid).toMutableList(), Audio)
            } else {
                //eduContext?.streamContext()?.muteStreams(arrayOf(it.streamUuid).toMutableList(), Audio)
            }
        }
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        if (viewGroup != null) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(), viewGroup,
                    streamUuid)
        } else {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
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

    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        largeVideoArt!!.setVisibility(visibility, userDetailInfo)

    }

    fun setLocation(rect: Rect) {
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