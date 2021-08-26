package io.agora.extension

interface IAgoraExtAppAPaaSEntry {
    fun getRoomInfo(): AgoraExtAppRoomInfo

    fun getLocalUserInfo(): AgoraExtAppUserInfo

    fun getProperties(identifier: String): MutableMap<String, Any?>?

    fun updateProperties(identifier: String,
                         properties: MutableMap<String, Any?>?,
                         cause: MutableMap<String, Any?>?,
                         common: MutableMap<String, Any?>?,
                         callback: AgoraExtAppCallback<String>?)

    fun deleteProperties(identifier: String,
                         propertyKeys: MutableList<String>,
                         cause: MutableMap<String, Any?>?,
                         callback: AgoraExtAppCallback<String>?)

    fun syncAppPosition(identifier: String, userId: String, x: Float, y: Float)

    fun getAppPosition(identifier: String): ExtAppPosition
}

class ExtAppPosition(var userId: String = "", var x: Float = 0F, var y: Float = 0F)