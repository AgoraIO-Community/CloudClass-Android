package io.agora.online.helper

/**
 * author : felix
 * date : 2022/2/28
 * description :
 */
@Deprecated("废弃")
object AgoraUIConfig {
    const val carouselMaxItem = 7
    const val videoWidthMaxRatio = 0.25487f
    const val videoRatio1 = 9 / 16f
    const val clickInterval = 500L
    var isLargeScreen: Boolean = false
    const val videoPlaceHolderImgSizePercent = 0.6f
    const val videoOptionIconSizePercent = 0.14f
    const val videoOptionIconSizeMax = 54
    const val videoOptionIconSizeMaxWithLargeScreen = 36
    const val audioVolumeIconWidthRatio = 0.72727273f
    const val audioVolumeIconAspect = 0.1875f
    const val chatHeightLargeScreenRatio = 0.7f
    var isGARegion = false
    var keepVideoListItemRatio = false

    const val baseUIHeightSmallScreen = 375f
    const val baseUIHeightLargeScreen = 574f

    object OneToOneClass {
        var teacherVideoWidth = 600
    }

    object SmallClass {
        var videoListVideoWidth = 600
        var videoListVideoHeight = 336
    }

    object LargeClass {
        const val coHostMaxItem = 4

        // whiteBoard and teacherVideo
        const val componentRatio = 9f / 16f
        const val statusBarPercent = 0.026f
        const val teacherVideoWidthMaxRatio = 0.29985f

        var studentVideoWidth = 600
        var studentVideoHeight = 336
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
    }
}