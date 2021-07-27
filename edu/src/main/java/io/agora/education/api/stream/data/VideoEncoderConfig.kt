package io.agora.education.api.stream.data

enum class OrientationMode {
    ADAPTIVE,
    FIXED_LANDSCAPE,
    FIXED_PORTRAIT
}

enum class DegradationPreference {
    MAINTAIN_QUALITY,
    MAINTAIN_FRAME_RATE,
    MAINTAIN_BALANCED
}

object VideoDimensions {
    val VideoDimensions_640X480 = arrayOf(640, 480)
    val VideoDimensions_320X240 = arrayOf(320, 240)
    val VideoDimensions_360X240 = arrayOf(360, 240)
}

data class VideoEncoderConfig(
        var videoDimensionWidth: Int = 360,
        var videoDimensionHeight: Int = 360,
        var frameRate: Int = 15,
        var bitrate: Int = 0,
        var orientationMode: OrientationMode = OrientationMode.ADAPTIVE,
        var degradationPreference: DegradationPreference = DegradationPreference.MAINTAIN_QUALITY
)
