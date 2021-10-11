package io.agora.edu.core.internal.framework.data

import io.agora.edu.core.internal.framework.EduBaseUserInfo

enum class VideoSourceType(var value: Int) {
    CAMERA(1),
    SCREEN(2)
}

enum class AudioSourceType(var value: Int) {
    MICROPHONE(1)
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

open class EduStreamInfo(
        val streamUuid: String,
        var streamName: String?,
        var videoSourceType: VideoSourceType,
        var hasVideo: Boolean,
        var hasAudio: Boolean,
        val publisher: EduBaseUserInfo) {
    init {
        if (streamName.isNullOrEmpty()) {
            streamName = streamUuid.plus("-")
        }
    }

    /**
     * If this and the target stream info object refers to a same stream.
     * These two stream info objects may not be the same instance and
     * not the same stream states.
     * Note stream name is not one of the properties that determine
     * same streams
     * @param streamInfo another stream info object to compare
     */
    fun isSameStream(streamInfo: EduStreamInfo): Boolean {
        return this.streamUuid == streamInfo.streamUuid
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduStreamInfo) {
            return false
        }
        return (other.streamUuid == this.streamUuid && other.streamName == this.streamName &&
                other.publisher == this.publisher) && other.hasAudio == this.hasAudio &&
                other.hasVideo == this.hasVideo && other.videoSourceType == this.videoSourceType
    }

    fun copy(): EduStreamInfo {
        return EduStreamInfo(streamUuid, streamName, videoSourceType, hasVideo, hasAudio, publisher)
    }

    override fun hashCode(): Int {
        var result = streamUuid.hashCode()
        result = 31 * result + (streamName?.hashCode() ?: 0)
        result = 31 * result + videoSourceType.hashCode()
        result = 31 * result + hasVideo.hashCode()
        result = 31 * result + hasAudio.hashCode()
        result = 31 * result + publisher.hashCode()
        return result
    }
}

internal class EduStream {
    lateinit var stream: EduStreamInfo
}

data class EduStreamEvent(
        val modifiedStream: EduStreamInfo,
        val operatorUser: EduBaseUserInfo?) {

    fun copy(): EduStreamEvent {
        val eduStreamInfo = modifiedStream.copy()
        val eduStreamEvent = operatorUser?.copy()
        return EduStreamEvent(eduStreamInfo, eduStreamEvent)
    }
}