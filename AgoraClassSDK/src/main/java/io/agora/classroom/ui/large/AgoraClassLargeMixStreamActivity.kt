package io.agora.classroom.ui.large

import android.view.View
import androidx.core.content.ContextCompat
import io.agora.agoraeducore.core.context.EduContextMirrorMode
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextRenderMode
import io.agora.agoraeducore.core.context.FcrRecordingState
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : felix
 * date : 2022/8/19
 * description : 合流转推 大班课（5000）
 */
class AgoraClassLargeMixStreamActivity : AgoraClassLargeRecordActivity() {

    override fun joinRoomSuccess() {
        val state = streamManager.getRecordState(eduCore())
        LogX.e(TAG, " record(init) state=$state")
        if (state == FcrRecordingState.STARTED) {
            playVideo(true)
        }
    }

    override fun onRoomRecordUpdated(state: FcrRecordingState) {
        super.onRoomRecordUpdated(state)
        if (state == FcrRecordingState.STARTED) {
            playVideo(true)
        } else {
            playVideo(false)
        }
    }

    override fun getLargeVideoArea(): View {
        return binding.agoraClassContentLayout
    }

    override fun playVideo(isPlay: Boolean) {
        val videoUrl = streamManager.getRecordUrl(eduCore())

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
                    eduCore()?.eduContextPool()?.mediaContext()?.startPlayAudioFromCdn(videoUrl)
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


}