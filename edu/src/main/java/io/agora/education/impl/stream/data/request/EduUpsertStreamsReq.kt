package io.agora.education.impl.stream.data.request

import io.agora.education.api.stream.data.AudioSourceType
import io.agora.education.api.stream.data.VideoSourceType


/**@param streamUuid 同一appId下流的唯一id，rtc中uid*/
class EduUpsertStreamsReq(var userUuid: String,
                          var streamUuid: String,
                          var streamName: String?,
                          var videoSourceType: Int,
                          var videoState: Int, var audioState: Int) {
    var audioSourceType: Int = AudioSourceType.MICROPHONE.value

    /*是否针对当前流生成rtcToken;默认为1:生成*/
    var generateToken: Int = 1

    constructor(userUuid: String, streamUuid: String, streamName: String?,
                videoSourceType: Int, videoState: Int, audioState: Int, generateToken: Int) :
            this(userUuid, streamUuid, streamName, videoSourceType, videoState, audioState) {
        this.generateToken = generateToken
    }

}

class EduUpsertStreamsBody(
        val streams: MutableList<EduUpsertStreamsReq>
)