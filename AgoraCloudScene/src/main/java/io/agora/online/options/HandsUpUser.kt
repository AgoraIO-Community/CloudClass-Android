package io.agora.online.options

/**
 * author : wufang
 * date : 2022/3/14
 * description :正在举手的用户
 */
data class HandsUpUser(
    val userUuid: String,
    val userName: String,
    val isCoHost: Boolean
)