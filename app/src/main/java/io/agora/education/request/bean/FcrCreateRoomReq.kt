package io.agora.education.request.bean

/**
 * author : wf
 * date : 2022/9/13 8:04 下午
 * description : 创建房间时候的请求体
 */
class FcrCreateRoomReq(var roomName: String, var roomType: Int, var startTime: Long, var endTime: Long) {
    var roomProperties: FcrCreateRoomProperties? = null
}