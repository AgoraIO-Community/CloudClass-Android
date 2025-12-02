package io.agora.education.request.bean

/**
 * author : wf
 * date : 2022/9/13 8:04 下午
 * description : 创建房间时候的请求体
 */
class FcrCreateRoomReq(var roomName: String, var roomType: Int, var startTime: Long, var endTime: Long) {
    var roomProperties: FcrCreateRoomProperties? = null
    var userName: String? = null

    /**
     * 场景类型
     * 0：one_on_one     roomType = 0
     * 2：large_class    roomType = 2
     * 4：edu_medium_v1  roomType = 4
     * 6：proctoring     roomType = 6
     * 10: cloud_class   roomType = 4
     */
    var sceneType: Int = 4
}