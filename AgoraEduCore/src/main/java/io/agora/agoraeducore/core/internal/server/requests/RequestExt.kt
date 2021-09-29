package io.agora.agoraeducore.core.internal.server.requests

import io.agora.agoraeducore.core.internal.server.struct.request.RoomPreCheckReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduJoinClassroomReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduRemoveRoomPropertyReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUpsertRoomPropertyReq
import io.agora.agoraeducore.core.internal.server.struct.request.*

fun initRoomServiceConfigs() {
    Request.addConfig(
            request = Request.RoomConfig,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/configs",
            httpMethod = "GET",
            pathCount = 1)

    Request.addConfig(
            request = Request.RoomPreCheck,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}",
            httpMethod = "PUT",
            pathCount = 3,
            hasBody = true,
            bodyType = RoomPreCheckReq::class)

    Request.addConfig(
            request = Request.RoomJoin,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/entry",
            httpMethod = "POST",
            pathCount = 3,
            hasBody = true,
            bodyType = EduJoinClassroomReq::class)

    Request.addConfig(
            request = Request.RoomSnapshot,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/snapshot",
            httpMethod = "GET",
            pathCount = 2)

    Request.addConfig(
            request = Request.RoomSequence,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/sequences?nextId={nextId}&count={count}",
            httpMethod = "GET",
            pathCount = 2,
            queryCount = 2)

    Request.addConfig(
            request = Request.RoomSetProperty,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/properties",
            httpMethod = "PUT",
            pathCount = 2,
            hasBody = true,
            bodyType = EduUpsertRoomPropertyReq::class)

    Request.addConfig(
            request = Request.RoomRemoveProperty,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/properties",
            httpMethod = "PUT",
            pathCount = 2,
            hasBody = true,
            bodyType = EduRemoveRoomPropertyReq::class)

    Request.addConfig(
            request = Request.RoomSetRoleMuteState,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/roles/mute",
            httpMethod = "PUT",
            pathCount = 2,
            hasBody = true,
            bodyType = EduRoomMuteStateReq::class)

    Request.addConfig(
            request = Request.RoomSetClassState,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUUid}/states/{state}",
            httpMethod = "PUT",
            pathCount = 3)
}

fun initMessageServiceConfigs() {
    Request.addConfig(
            request = Request.SendRoomMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUuid}/from/{userUuid}/chat",
            httpMethod = "POST",
            pathCount = 3,
            hasBody = true,
            bodyType = EduRoomChatMsgReq::class)

    Request.addConfig(
            request = Request.GetRoomMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/chat/messages",
            httpMethod = "GET",
            pathCount = 2,
            queryCount = 3)

    Request.addConfig(
            request = Request.SendRoomCustomMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/message/channel",
            httpMethod = "POST",
            pathCount = 2,
            hasBody = true,
            bodyType = EduRoomMsgReq::class)

    Request.addConfig(
            request = Request.SendPeerMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/chat/peer",
            httpMethod = "POST",
            pathCount = 3,
            hasBody = true,
            bodyType = EduUserChatMsgReq::class)

    Request.addConfig(
            request = Request.SendPeerCustomMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/users/{toUserUuid}/messages/peer",
            httpMethod = "POST",
            pathCount = 3,
            hasBody = true,
            bodyType = EduUserMsgReq::class)

    Request.addConfig(
            request = Request.SendConversationMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUuid}/conversation/students/{studentUuid}/messages",
            httpMethod = "POST",
            pathCount = 3,
            hasBody = true,
            bodyType = EduRoomChatMsgReq::class)

    Request.addConfig(
            request = Request.GetConversationMessage,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/conversation/students/{studentsUuid}/messages",
            httpMethod = "GET",
            pathCount = 3,
            queryCount = 2)

    Request.addConfig(
            request = Request.Translate,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/acadsoc/apps/{appId}/v1/translation",
            httpMethod = "POST",
            pathCount = 1,
            hasBody = true,
            bodyType = ChatTranslateReq::class)

    Request.addConfig(
            request = Request.SetUserChatMuteState,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}",
            httpMethod = "PUT",
            pathCount = 3,
            hasBody = true,
            bodyType = EduUserRoomChatMuteReq::class)
}

fun initMediaServiceConfigs() {
    Request.addConfig(
            request = Request.UpdateDeviceState,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUuid}/users/{userUuid}/device",
            httpMethod = "PUT",
            pathCount = 3,
            hasBody = true,
            bodyType = DeviceStateUpdateReq::class)
}

fun initUserServiceConfigs() {
    Request.addConfig(
            request = Request.HandsUpApply,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress",
            httpMethod = "POST",
            pathCount = 2)

    Request.addConfig(
            request = Request.HandsUpCancel,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/progress",
            httpMethod = "DELETE",
            pathCount = 2)

    Request.addConfig(
            request = Request.HandsUpExit,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/processes/handsUp/acceptance",
            httpMethod = "DELETE",
            pathCount = 2)
}

fun initGeneralServiceConfig() {

}

fun initExtensionServiceConfig() {
    Request.addConfig(
            request = Request.SetFlexibleRoomProperty,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/properties",
            httpMethod = "PUT",
            pathCount = 2,
            hasBody = true,
            bodyType = RoomFlexPropsReq::class)

    Request.addConfig(
            request = Request.SetFlexibleUserProperty,
            priority = RequestChannelPriority.HTTP,
            urlFormat = "edu/apps/{appId}/v2/rooms/{roomUUid}/users/{userUuid}/properties",
            httpMethod = "PUT",
            pathCount = 3,
            hasBody = true,
            bodyType = UserFlexPropsReq::class)
}