package io.agora.online.component.teachaids.presenter

/**
 * author : felix
 * date : 2022/5/7
 * description : 大窗数据
 */
object FCRLargeWindowManager {
    /**
     * key：roomUuid
     * val：大窗的用户
     */
    val map = mutableMapOf<String, HashSet<String>>()

    fun isLargeWindow(roomUuid: String, streamUuid: String): Boolean {
        map[roomUuid]?.let {
            return it.contains(streamUuid)
        }
        return false
    }

    fun addLargeWindow(roomUuid: String, streamUuid: String) {
        var list = map[roomUuid]
        if (list == null) {
            list = HashSet()
        }
        list.add(streamUuid)
        map[roomUuid] = list
    }

    fun removeLargeWindow(roomUuid: String, streamUuid: String) {
        map[roomUuid]?.remove(streamUuid)
    }

    fun clearByRoom(roomUuid: String) {
        map[roomUuid]?.clear()
    }

    fun clear() {
        map.clear()
    }
}