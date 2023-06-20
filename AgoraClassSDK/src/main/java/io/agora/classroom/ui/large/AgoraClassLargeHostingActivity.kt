package io.agora.classroom.ui.large

import android.view.View
import androidx.core.content.ContextCompat
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.EduContextMirrorMode
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextRenderMode
import io.agora.agoraeducore.core.internal.framework.bean.FcrMediaPlayerState
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : felix
 * date : 2022/8/19
 * description : 伪直播大班课
 */
class AgoraClassLargeHostingActivity : AgoraClassLargeRecordActivity() {
    var seekToTime = 0L

    fun updateSeekTime() {
        val currentTime = eduCore()?.eduContextPool()?.monitorContext()?.getSyncTimestamp()!!
        val startTime = eduCore()?.eduContextPool()?.roomContext()?.getClassInfo()?.startTime
        if (startTime != null) {
            seekToTime = currentTime - startTime
            if (seekToTime < 0) {
                seekToTime = 0
            }
        }
    }

    override fun joinRoomSuccess() {
        val state = streamManager.getClassState(eduCore())
        LogX.e(TAG, " record(init) state=$state")

        if (state == AgoraEduContextClassState.During) {
            playVideo(true)
        }
    }

    override fun playVideo(isPlay: Boolean) {
        val videoUrl = streamManager.getHostingRecordUrl(eduCore())

        if (isPlay) {
            videoUrl?.let {
                LogX.e(TAG, " record play : $videoUrl")
                playVideoUrl = videoUrl
                // 渲染
                eduCore()?.eduContextPool()?.mediaContext()?.startRenderVideoFromCdn(
                    EduContextRenderConfig(EduContextRenderMode.FIT, EduContextMirrorMode.AUTO),
                    binding.agoraClassPlayer, videoUrl
                )
                {
                    // 播放
                    updateSeekTime()
                    eduCore()?.eduContextPool()?.mediaContext()
                        ?.startPlayAudioFromCdn(videoUrl, seekToTime) { state, error ->
                            if (state == FcrMediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED) {
                                ContextCompat.getMainExecutor(context).execute {
                                    classManager?.showDestroyRoom()
                                }
                            }
                        }
                    ContextCompat.getMainExecutor(context).execute {
                        binding.agoraClassPlayerPlaceholder.visibility = View.GONE
                    }
                }
            }
        } else {
            LogX.e(TAG, " record stop : $playVideoUrl")
            // 暂停
            playVideoUrl?.let {
                eduCore()?.eduContextPool()?.mediaContext()?.stopRenderVideoFromCdn(it)
                eduCore()?.eduContextPool()?.mediaContext()?.stopPlayAudioFromCdn(it)
            }
            ContextCompat.getMainExecutor(context).execute {
                binding.agoraClassPlayer.removeAllViews()
                binding.agoraClassPlayerPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    override fun onRoomStateUpdated(state: AgoraEduContextClassState) {
        super.onRoomStateUpdated(state)

        if (state == AgoraEduContextClassState.After) {
            classManager?.showDestroyRoom()
        } else if (state == AgoraEduContextClassState.During) {
            playVideo(true)
        } else {
            playVideo(false)
        }
    }

    override fun getLargeVideoArea(): View {
        return binding.agoraClassContentLayout
    }
}