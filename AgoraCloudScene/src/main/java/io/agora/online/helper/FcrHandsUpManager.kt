package io.agora.online.helper

import android.os.Handler
import android.os.Looper
import io.agora.online.component.CameraStateCallback
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.MediaProxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * author : felix
 * date : 2023/5/19
 * description :
 *
 * https://confluence.agoralab.co/pages/viewpage.action?pageId=1213957845
 *
 *
{
"cmd":"handsUp",
"data":{
"userUuid": {string} // 被操作人
"state": 1           // 0: 取消举手，1：举手
}
}


{
"cmd":"handsUpAll",
"data":{
"operation": 0  // 0: 取消全体举手
}
}
 */
object FcrHandsUpManager {
    const val CMD = "cmd"
    const val DATA = "data"
    const val HANDSUP_CMD_ALL = "handsUpAll"
    const val HANDSUP_CMD = "handsUp"
    const val DEVICE_SWITCH = "deviceSwitch"
    const val DEVICE_SWITCH_BATCH = "deviceSwitchBatch" // 全体禁言

    private var handsUpMap = ConcurrentHashMap<String, Long>()
    private var handsUpList = CopyOnWriteArrayList<String>()

    var onHandsUpUpdateListener: ((List<String>) -> Unit)? = null
    var handler = Handler(Looper.getMainLooper())
    var heartbeatTime = 3000L
    var refreshListTime = heartbeatTime + 2000L
    var userUuid: String? = null

    init {
        heartbeatTime = ((2..4).random()) * 1000L
        refreshListTime = heartbeatTime + 2000L
    }

    fun init(eduCore: AgoraEduCore?) {
        addHandsUpListener(eduCore)
        startRefresh()
    }

    fun release(eduCore: AgoraEduCore?) {
        stopRefresh()
        removeHandsUpListener(eduCore)
    }

    fun startRefresh() {
        stopRefresh()
        handsUpMap.forEach {
            // 超过 refreshListTime 心跳时间，就认为不再举手了
            //LogX.e(TAG, "check >>>>>>>>是否超超时： ${(System.currentTimeMillis() - it.value) > refreshListTime}")
            if (System.currentTimeMillis() - it.value > refreshListTime) {
                handsUpMap.remove(it.key)
            }
        }
        updateCallback()

        handler.postDelayed({
            startRefresh()
        }, refreshListTime)
    }

    fun stopRefresh() {
        handler.removeCallbacksAndMessages(null)
    }

    fun remove(userUuid: String?) {
        userUuid?.let {
            handsUpMap.remove(userUuid)
            updateCallback()
        }
    }

    fun add(userUuid: String?) {
        userUuid?.let {
            val isExist = handsUpMap.contains(userUuid)
            handsUpMap[userUuid] = System.currentTimeMillis()
            if (!isExist) {
                updateCallback()
            }
        }
    }

    fun clear() {
        handsUpMap.clear()
        updateCallback()
    }

    fun updateCallback() {
        //LogX.e("handsUpList=$handsUpList || getList = ${getList()}")
        if (handsUpList != getList()) {
            //LogX.e("handsUpList change")

            handsUpList.clear()
            handsUpList.addAll(getList())
            onHandsUpUpdateListener?.invoke(handsUpList)
        } else {
            //LogX.e("handsUpList un callback")
        }
    }

    fun getList(): List<String> {
        if (handsUpMap.isEmpty()) {
            return ArrayList<String>()
        }
        return handsUpMap.keys.toList()
    }

    fun addHandsUpListener(eduCore: AgoraEduCore?) {
        userUuid = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid
        eduCore?.eduContextPool()?.roomContext()?.addHandler(roomHandler)
    }

    fun removeHandsUpListener(eduCore: AgoraEduCore?) {
        eduCore?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
    }

    fun getDeviceState(eduCore: AgoraEduCore?, device: AgoraEduContextSystemDevice, listener: (Boolean) -> Unit) {
        getSystemDeviceInfo(device)?.let { deviceInfo ->
            eduCore?.eduContextPool()?.mediaContext()?.getLocalDeviceState(deviceInfo, CameraStateCallback {
                if (AgoraEduContextDeviceState2.isDeviceOpen(it)) {
                    listener.invoke(true)
                } else {
                    listener.invoke(false)
                }
            })
        }
    }

    fun getSystemDeviceInfo(device: AgoraEduContextSystemDevice): AgoraEduContextDeviceInfo? {
        val id = AgoraEduContextSystemDevice.getDeviceId(device)
        val info = MediaProxy.getSystemDeviceMap()[id]
        return info?.let { AgoraEduContextDeviceInfo(it.id, it.name, toEduContextDeviceType(it.type)) }
    }

    fun toEduContextDeviceType(type: MediaProxy.DeviceType): AgoraEduContextDeviceType {
        return when (type) {
            MediaProxy.DeviceType.Camera -> AgoraEduContextDeviceType.Camera
            MediaProxy.DeviceType.Mic -> AgoraEduContextDeviceType.Mic
            MediaProxy.DeviceType.Speaker -> AgoraEduContextDeviceType.Speaker
        }
    }

    var roomHandler = object : RoomHandler() {
        override fun onReceiveCustomChannelMessage(customMessage: FcrCustomMessage) {
            val cmdValue = customMessage.payload.data[CMD]
            if (cmdValue == HANDSUP_CMD) {
                val handsUp = customMessage.payload.data[DATA] as? Map<String, Any>
                handsUp?.let {
                    val userUuid = handsUp.get("userUuid") as? String
                    val state = ("" + handsUp.get("state")).toDouble().toInt()

                    userUuid?.let {
                        if (state == 0) { // 0: 取消举手
                            remove(userUuid)
                        } else if (state == 1) { // 1：举手
                            add(userUuid)
                        }
                    }
                }
            } else if (cmdValue == HANDSUP_CMD_ALL) {
                val handsUpAll = customMessage.payload.data[DATA] as? Map<String, Any>
                handsUpAll?.let {
                    val operation = ("" + handsUpAll.get("operation")).toDouble().toInt()
                    if (operation == 0) { // 取消全体举手
                        clear()
                    }
                }
            }
        }
    }
}