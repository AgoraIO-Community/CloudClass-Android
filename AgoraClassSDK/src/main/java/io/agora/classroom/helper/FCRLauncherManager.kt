package io.agora.classroom.helper

import java.util.concurrent.ConcurrentHashMap

/**
 * author : felix
 * date : 2022/5/6
 * description : 退出教室
 */
object FCRLauncherManager {
    val map = ConcurrentHashMap<String, FCRLauncherListener>()

    fun addLauncherListener(roomUuid: String, listener: FCRLauncherListener) {
        removeLauncherListener(roomUuid)
        map[roomUuid] = listener
    }

    fun removeLauncherListener(listener: FCRLauncherListener) {
        map.forEach {
            if (it.value == listener) {
                map.remove(it.key)
            }
        }
    }

    fun removeLauncherListener(roomUuid: String) {
        map.remove(roomUuid)
    }

    fun notifyLauncher(roomUuid: String) {
        map.forEach {
            if (it.key == roomUuid) {
                it.value.onExit()
            }
        }
    }

    fun notifyAllLauncher() {
        map.forEach {
            it.value.onExit()
        }
    }

    fun clear() {
        map.clear()
    }
}