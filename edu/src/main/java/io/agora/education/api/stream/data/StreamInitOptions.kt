package io.agora.education.api.stream.data

open class StreamInitOptions(
        var streamUuid: String,
        var streamName: String? = null
)

class LocalStreamInitOptions(
        streamUuid: String,
        var enableCamera: Boolean = true,
        var enableMicrophone: Boolean = true,
        var hasVideo: Boolean = true,
        var hasAudio: Boolean = true
) : StreamInitOptions(streamUuid) {
    constructor(streamUuid: String, streamName: String?, enableCamera: Boolean,
                enableMicrophone: Boolean) : this(streamUuid, enableCamera, enableMicrophone) {
        this.streamName = streamName
    }
}

class ScreenStreamInitOptions(
        streamUuid: String,
        streamName: String
) : StreamInitOptions(streamUuid, streamName)
