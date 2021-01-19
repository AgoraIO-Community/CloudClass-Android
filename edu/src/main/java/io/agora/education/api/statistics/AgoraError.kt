package io.agora.education.api.statistics

enum class AgoraError(var value: Int) {
    NONE(0),

    INTERNAL_ERROR(-1),

    SEQUENCE_NOT_EXISTS(20404101),

    ROOM_ALREADY_EXISTS(20409100),
}