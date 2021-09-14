package io.agora.edu.uikit.impl.video

enum class VideoState(val value: Int) {
    Init(-1),
    Normal(0),
    OffLine(1),
    VideoOff(2),
    NoCamera(3),
    Loading(4)
}

enum class AudioState(val value: Int) {
    Init(-1),
    Normal(0),
    Off(1),
    NoDevice(2)
}

enum class StageState(val value: Int) {
    Init(-1),
    NoStage(0),
    Staging(1)
}