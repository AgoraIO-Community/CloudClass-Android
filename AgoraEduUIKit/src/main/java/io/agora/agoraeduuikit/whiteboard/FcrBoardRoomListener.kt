package io.agora.agoraeduuikit.whiteboard

/**
 * author : felix
 * date : 2022/6/9
 * description :
 */
interface FcrBoardRoomListener {
    fun onConnectionStateUpdated(state: FcrBoardRoomConnectionState)
    fun onBoardLog(log: String, extra: String? = null, type: FcrBoardLogType)
    fun onNetlessLog(log: String, extra: String? = null, type: FcrBoardLogType)
}