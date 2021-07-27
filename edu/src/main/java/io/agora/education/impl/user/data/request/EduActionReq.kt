package io.agora.education.impl.user.data.request

internal class EduActionReq(
        val fromUserUuid: String,
        var payload: ReqPayload
) {
}

internal class ReqPayload(
        val action: Int,
        var fromUser: ReqUser,
        var fromRoom: ReqRoom
){
}

internal class ReqUser(
        val uuid: String,
        val name: String,
        val role: String
){}

internal class ReqRoom(
        val name: String,
        val uuid: String
){}