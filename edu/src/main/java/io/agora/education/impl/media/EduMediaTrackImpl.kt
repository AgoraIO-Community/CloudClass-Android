package io.agora.education.impl.media

import android.view.SurfaceView
import android.view.ViewGroup
import io.agora.education.api.media.EduCameraVideoTrack
import io.agora.education.api.media.EduMicrophoneAudioTrack
import io.agora.education.api.stream.data.EduRenderConfig
import io.agora.education.api.stream.data.EduRenderMode
import io.agora.education.api.stream.data.EduVideoEncoderConfig
import io.agora.education.impl.util.Convert.convertVideoEncoderConfig
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rte.RteEngineImpl
import io.agora.rte.RteEngineImpl.OK

internal class EduCameraVideoTrackImpl : EduCameraVideoTrack {
    private var renderConfig: EduRenderConfig? = null

    private var previewSurface: SurfaceView? = null

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
            previewSurface = RtcEngine.CreateRendererView(container.context)
            previewSurface!!.setZOrderMediaOverlay(true)
            previewSurface!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            container.addView(previewSurface)
            videoCanvas = VideoCanvas(previewSurface, renderMode, 0)
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
        previewSurface?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            previewSurface = null
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