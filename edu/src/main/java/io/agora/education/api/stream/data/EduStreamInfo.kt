package io.agora.education.api.stream.data

import io.agora.education.api.user.data.EduBaseUserInfo

enum class VideoSourceType(var value: Int) {
    CAMERA(1),
    SCREEN(2)
}

enum class AudioSourceType(var value: Int) {
    MICROPHONE(1)
}

open class EduStreamInfo(
        /*仅支持数字类型的字符串*/
        val streamUuid: String,
        var streamName: String?,
        var videoSourceType: VideoSourceType,
        var hasVideo: Boolean,
        var hasAudio: Boolean,
        val publisher: EduBaseUserInfo
) {
    init {
        if (streamName == null) {
            streamName = streamUuid.plus("-")
        }
    }

    /**是否是同一个流*/
    fun same(streamInfo: EduStreamInfo): Boolean {
        return this.streamUuid == streamInfo.streamUuid
    }

    /**是否是同一个对象*/
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduStreamInfo) {
            return false
        }
        return (other.streamUuid == this.streamUuid && other.streamName == this.streamName &&
                other.publisher == this.publisher) && other.hasAudio == this.hasAudio &&
                other.hasVideo == this.hasVideo && other.videoSourceType == this.videoSourceType
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun copy(): EduStreamInfo {
        return EduStreamInfo(streamUuid, streamName, videoSourceType, hasVideo, hasAudio, publisher)
    }
}

enum class EduVideoState(var value: Int) {
    Off(0),
    Open(1),
    Disable(2)
}

enum class EduAudioState(var value: Int) {
    Off(0),
    Open(1),
    Disable(2)
}

//enum class EduStreamStateChangeType(var value: Int) {
//    UnKnow(0),
//    Video(1),
//    Audio(2),
//    VideoAudio(3)
//}
