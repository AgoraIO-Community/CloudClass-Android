package io.agora.agoraeduuikit.impl.whiteboard

import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.SimpleBoardEventListener

/**
 * author : felix
 * date : 2022/2/22
 * description :
 */
class AgoraWhiteBoardManager {
    companion object {
        /**
         * 为了解决多个教室，通过uuid将组件和白板关联起来
         */
        val whiteBoardListenerMap = mutableMapOf<String, MutableList<SimpleBoardEventListener>>()


        fun getWhiteBoardList(uuid: String): List<SimpleBoardEventListener>? {
            return whiteBoardListenerMap[uuid]
        }

        fun addWhiteBoardListener(uuid: String, listener: SimpleBoardEventListener) {
            val list = whiteBoardListenerMap[uuid]
            if (list == null) {
                val listTemp = mutableListOf<SimpleBoardEventListener>()
                listTemp.add(listener)
                whiteBoardListenerMap[uuid] = listTemp
            } else {
                if (!list.contains(listener)) {
                    list.add(listener)
                }
                whiteBoardListenerMap[uuid] = list
            }
        }

        fun removeWhiteBoardListener(uuid: String, listener: SimpleBoardEventListener) {
            val list = whiteBoardListenerMap[uuid]
            if (list != null) {
                if (!list.contains(listener)) {
                    list.remove(listener)
                }
            }
        }

        fun clearWhiteBoardList() {
            whiteBoardListenerMap.clear()
        }
    }
}