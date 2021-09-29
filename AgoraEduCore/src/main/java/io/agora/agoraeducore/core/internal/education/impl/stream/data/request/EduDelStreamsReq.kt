package io.agora.agoraeducore.core.internal.education.impl.stream.data.request

class EduDelStreamsReq(
        val userUuid: String,
        val streamUuid: String
) {
}

class EduDelStreamsBody(
        val streams: MutableList<EduDelStreamsReq>
)

