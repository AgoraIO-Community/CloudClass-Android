package io.agora.edu.core.internal.education.api.stream.data

enum class EduRenderMode(var value: Int) {
    HIDDEN(1),
    FIT(2)
}

data class EduRenderConfig(
        var eduRenderMode: EduRenderMode = EduRenderMode.HIDDEN,
        var eduMirrorMode: EduMirrorMode = EduMirrorMode.AUTO
)

enum class EduMirrorMode(val value: Int) {
    AUTO(0),
    ENABLED(1),
    DISABLED(2)
}