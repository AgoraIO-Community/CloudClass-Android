package io.agora.agoraeducore.core.internal.launch

enum class AgoraEduEncryptMode(val value: Int) {
    NONE(0),
    AES_128_XTS(1),
    AES_128_ECB(2),
    AES_256_XTS(3),
    SM4_128_ECB(4),
    AES_128_GCM(5),
    AES_256_GCM(6);
}