package io.agora.education.api.stream.data

enum class EduRenderMode(var value: Int) {
    HIDDEN(1),
    FIT(2)
}

data class EduRenderConfig(
        var eduRenderMode: EduRenderMode = EduRenderMode.HIDDEN
)
