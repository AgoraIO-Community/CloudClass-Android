package io.agora.edu.classroom

import android.content.Context
import android.view.ViewGroup
import io.agora.edu.R
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduUser
import io.agora.educontext.EduContextPool

class ScreenShareManager(
        val context: Context,
        private val eduContext: EduContextPool?,
        var launchConfig: AgoraEduLaunchConfig,
        var eduRoom: EduRoom?,
        var eduUser: EduUser
) {
    private val tag = "ScreenShareManager"

    private var localScreenStream: EduStreamInfo? = null

    @Volatile private var isScreenShare: Boolean = false

    var screenShareStateChangedListener = object : (Boolean) -> Unit {
        override fun invoke(p1: Boolean) {

        }
    }

    fun dispose() {
        eduRoom = null
    }

    fun isScreenSharing(): Boolean {
        return isScreenShare
    }

    fun setScreenShareState(sharing: Boolean) {
        if (sharing != isScreenShare) {
            isScreenShare = sharing
            screenShareStateChangedListener(sharing)
        }
    }

    fun renderScreenShare(container: ViewGroup?, streamUuid: String) {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.streamUuid == streamUuid }?.let {
                    isScreenShare = container != null
                    eduUser.setStreamView(it, launchConfig.roomUuid, container, isScreenShare)
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    fun checkAndNotifyScreenShareStarted(streamEvents: MutableList<EduStreamEvent>) {
        val screenShareStream = streamEvents.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream

        screenShareStream?.let {
            localScreenStream = screenShareStream
            eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                h.onScreenShareStateUpdated(true, localScreenStream!!.streamUuid)
                h.onScreenShareTip(String.format(
                        context.getString(R.string.screen_share_start_message_format),
                        it.publisher.userName))
            }
        }
    }

    /**
     * Especially check for local screen share stream when
     * joining room successfully that whether local user is
     * sharing his screen when he last time joined
     */
    fun checkAndNotifyScreenShareRestored() {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
                    if (localScreenStream != null && localScreenStream == it) {
                        return
                    } else {
                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                            h.onScreenShareStateUpdated(true, it.streamUuid)
                            h.onScreenShareTip(String.format(
                                    context.getString(R.string.screen_share_start_message_format),
                                    it.publisher.userName))
                        }
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun checkAndNotifyScreenShareRemoved(streamEvents: MutableList<EduStreamEvent>) {
        localScreenStream = streamEvents?.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        localScreenStream?.let {
            eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                h.onScreenShareStateUpdated(false, it.streamUuid)
                h.onScreenShareTip(String.format(
                        context.getString(R.string.screen_share_end_message_format),
                        it.publisher.userName))
            }
        }
    }
}