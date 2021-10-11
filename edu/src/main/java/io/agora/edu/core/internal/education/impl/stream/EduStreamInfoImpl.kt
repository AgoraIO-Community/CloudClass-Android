package io.agora.edu.core.internal.education.impl.stream

import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.framework.data.VideoSourceType
import io.agora.edu.core.internal.framework.EduBaseUserInfo

internal class EduStreamInfoImpl(
        streamUuid: String,
        streamName: String?,
        videoSourceType: VideoSourceType,
        hasVideo: Boolean,
        hasAudio: Boolean,
        publisher: EduBaseUserInfo,
        var updateTime: Long?
) : EduStreamInfo(streamUuid, streamName, videoSourceType, hasVideo, hasAudio, publisher) {
}
