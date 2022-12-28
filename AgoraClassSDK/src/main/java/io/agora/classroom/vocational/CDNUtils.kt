package io.agora.classroom.vocational

import android.view.ViewGroup
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextStreamInfo
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.log.LogX

object CDNUtils {
    private const val tag = "Vocational-CDNUtils"
    private val cdnRenderingSet = mutableSetOf<String>()

    fun haveCdnStream(streamInfo: AgoraEduContextStreamInfo): Boolean {
        return streamInfo.streamFlvUrl != null ||
            streamInfo.streamRtmpUrl != null ||
            streamInfo.streamHlsUrl != null
    }

    private fun findBestCdnStream(streamInfo: AgoraEduContextStreamInfo): String? {
        streamInfo.streamFlvUrl?.let {
            return it
        }

        streamInfo.streamHlsUrl?.let {
            return it
        }

        streamInfo.streamRtmpUrl?.let {
            return it
        }

        return null
    }

    @Synchronized
    fun renderCdnStream(eduCore: AgoraEduCore?, streamInfo: AgoraEduContextStreamInfo?, viewGroup: ViewGroup?) {
        streamInfo?.let { st ->
            if (cdnRenderingSet.contains(st.streamUuid)) {
                LogX.d(tag, " cdn stream is rendering")
                return
            }
            val url = findBestCdnStream(st)
            url?.let {
                viewGroup?.let {
                    LogX.d(tag, " stop rtc stream")
                    eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamInfo.streamUuid)
                    eduCore?.eduContextPool()?.let { context ->
                        // we believe that the cdn stream have both audio/video -- 2022.05.30
                        LogX.d(tag, " render cdn video $url end")
                        cdnRenderingSet.add(st.streamUuid)
                        context.mediaContext()?.startRenderVideoFromCdn(
                            EduContextRenderConfig(),
                            it,
                            url
                        ) {
                            LogX.d(tag, " render cdn audio")
                            context.mediaContext()?.startPlayAudioFromCdn(url)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun stopCdnStream(eduCore: AgoraEduCore?, streamInfo: AgoraEduContextStreamInfo?) {
        streamInfo?.let {
            eduCore?.eduContextPool()?.let { context ->
                cdnRenderingSet.remove(streamInfo.streamUuid)
                context.mediaContext()?.stopRenderVideoFromCdn(streamInfo.streamUuid)
                findBestCdnStream(streamInfo)?.let {
                    context.mediaContext()?.stopPlayAudioFromCdn(it)
                }
            }
        }
    }
}