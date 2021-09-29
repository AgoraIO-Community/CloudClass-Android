package io.agora.agoraeducore.core.internal.education.impl.media

import android.view.TextureView
import android.view.ViewGroup
import io.agora.agoraeducore.core.internal.education.api.media.EduCameraVideoTrack
import io.agora.agoraeducore.core.internal.education.api.media.EduMicrophoneAudioTrack
import io.agora.agoraeducore.core.internal.education.api.stream.data.EduRenderConfig
import io.agora.agoraeducore.core.internal.education.api.stream.data.EduRenderMode
import io.agora.agoraeducore.core.internal.education.api.stream.data.EduVideoEncoderConfig
import io.agora.agoraeducore.core.internal.education.impl.util.Convert.convertVideoEncoderConfig
import io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.agoraeducore.core.internal.rte.RteEngineImpl
import io.agora.agoraeducore.core.internal.rte.RteEngineImpl.OK

internal class EduCameraVideoTrackImpl : EduCameraVideoTrack {
    private var renderConfig: EduRenderConfig? = null

    private var previewTexture: TextureView? = null

    override fun start(): Int {
        return RteEngineImpl.enableLocalVideo(true)
    }

    override fun stop(): Int {
        return RteEngineImpl.enableLocalVideo(false)
    }

    override fun switchCamera(): Int {
        return RteEngineImpl.switchCamera()
    }

    override fun setView(container: ViewGroup?): Int {
        val videoCanvas: VideoCanvas
        var renderMode: Int = renderConfig?.eduRenderMode?.value ?: EduRenderMode.FIT.value
        removePreviewSurface()
        if (container == null) {
            videoCanvas = VideoCanvas(null, renderMode, 0)
        } else {
            previewTexture = RtcEngine.CreateTextureView(container.context)
//            previewTexture!!.setZOrderMediaOverlay(true)
            previewTexture!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            container.addView(previewTexture)
            videoCanvas = VideoCanvas(previewTexture, renderMode, 0)
        }
        val a = RteEngineImpl.setupLocalVideo(videoCanvas)
        if (a < OK()) {
            return a
        }
        container?.let {
            val b = RteEngineImpl.setClientRole(CLIENT_ROLE_BROADCASTER)
            if (b < OK()) {
                return b
            }
        }
        val c = if (container == null) {
            RteEngineImpl.stopPreview()
        } else {
            RteEngineImpl.startPreview()
        }
        return c
    }

    private fun removePreviewSurface() {
        previewTexture?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            previewTexture = null
        }
    }

    override fun setRenderConfig(config: EduRenderConfig): Int {
        this.renderConfig = config
        return RteEngineImpl.setLocalRenderMode(config.eduRenderMode.value)
    }

    override fun setVideoEncoderConfig(config: EduVideoEncoderConfig): Int {
        return RteEngineImpl.setVideoEncoderConfiguration(convertVideoEncoderConfig(config))
    }
}

internal class EduMicrophoneAudioTrackImpl : EduMicrophoneAudioTrack {
    override fun start(): Int {
        return RteEngineImpl.enableLocalAudio(true)
    }

    override fun stop(): Int {
        return RteEngineImpl.enableLocalAudio(false)
    }
}