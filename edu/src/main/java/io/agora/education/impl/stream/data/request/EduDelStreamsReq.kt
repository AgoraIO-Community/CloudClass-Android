package io.agora.education.impl.stream.data.request

class EduDelStreamsReq(
        val userUuid: String,
        val streamUuid: String
) {
}

class EduDelStreamsBody(
        val streams: MutableList<EduDelStreamsReq>
)

