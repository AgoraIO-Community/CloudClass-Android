package io.agora.agoraeduuikit.provider

import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import java.util.concurrent.CopyOnWriteArrayList

class UIDataProvider(private val eduContext: EduContextPool?) {
    private val tag = "UIDataProvider"

    private val listeners = CopyOnWriteArrayList<UIDataProviderListener>()

    @Volatile
    private var grantedUsers = CopyOnWriteArrayList<String>()

    @Volatile
    var localVideoState: AgoraEduContextDeviceState2 = AgoraEduContextDeviceState2.Close

    @Volatile
    var localAudioState: AgoraEduContextDeviceState2 = AgoraEduContextDeviceState2.Close

    /**
     * 获取进入教室，设置已经白板授权的人
     */
    fun setGrantedUsers(eduCore: AgoraEduCore?) {
        try {
            val map =
                (((eduCore?.room()?.roomProperties?.get("widgets") as? Map<*, *>)?.get("netlessBoard") as? Map<*, *>)?.get("extra") as? Map<*, *>)?.get(
                    "grantedUsers"
                ) as? Map<String, Boolean>

            map?.forEach {
                if (it.value) {
                    grantedUsers.add(it.key)
                }
            }

            if (grantedUsers.isNotEmpty()) {
                if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS) {
                    // 大班课，重新进入收回权限
                    val userUuid = eduContext.userContext()?.getLocalUserInfo()?.userUuid
                    grantedUsers.remove(userUuid)
                }

                if (grantedUsers.isNotEmpty()) {
                    val data = AgoraBoardGrantData(true, grantedUsers)
                    val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
                    eduContext?.widgetContext()?.sendMessageToWidget(packet, AgoraWidgetDefaultId.WhiteBoard.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addListener(listener: UIDataProviderListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun removeListener(listener: UIDataProviderListener) {
        listeners.remove(listener)
    }

    private fun iterateListeners(runnable: ((UIDataProviderListener) -> Unit)?) {
        listeners.forEach { listener ->
            runnable?.invoke(listener)
        }
    }

    private val widgetObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (id == AgoraWidgetDefaultId.WhiteBoard.id && packet.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                grantedUsers.clear()

                if (packet.body is MutableList<*>) { // 白板开关的格式
                    grantedUsers.addAll(packet.body as MutableList<String>)
                } else { // 白板授权的格式
                    val bodyStr = GsonUtil.gson.toJson(packet.body)
                    val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                    if (agoraBoard.granted) {
                        grantedUsers.addAll(agoraBoard.userUuids)
                    }
                }
                callbackBothKindsOfUserList()
            } else if (id == AgoraWidgetDefaultId.LargeWindow.id) {
//                val packet = Gson().fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
//                if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
//                    //标记largewindow showed
//                    largeWindowShowedUsers.clear()
//                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userInfo ->
//                        largeWindowShowedUsers.add(userInfo.userUuid)
//                    } ?: Runnable {
//                        LogX.e(tag,"${packet.signal}, packet.body convert failed")
//                    }
//                } else if(packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender){
//                    LogX.i(tag,"${packet.signal}, LargeWindowStopRender")
//                }
            }
        }
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            callbackBothKindsOfUserList()
        }
    }

    private val providerUserHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            callbackBothKindsOfUserList()

            val coHosts = getCoHostUuidSet()
            if (!coHosts.contains(user.userUuid)) {
                notifyUserListChanged()
            }
        }

        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {
            notifyUserListChanged()
        }

        override fun onUserUpdated(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserUpdateReason?
        ) {
            callbackBothKindsOfUserList()
        }

        override fun onCoHostUserListAdded(
            userList: List<AgoraEduContextUserInfo>,
            operator: AgoraEduContextUserInfo?
        ) {
            callbackBothKindsOfUserList()
        }

        override fun onUserRewarded(
            user: AgoraEduContextUserInfo,
            rewardCount: Int,
            operator: AgoraEduContextUserInfo?
        ) {
            callbackBothKindsOfUserList()
        }

        override fun onCoHostUserListRemoved(
            userList: List<AgoraEduContextUserInfo>,
            operator: AgoraEduContextUserInfo?
        ) {
            callbackBothKindsOfUserList()
        }

        override fun onHandsWaveEnabled(enabled: Boolean) {
            iterateListeners {
                it.onHandsWaveEnable(enabled)
            }
        }

        override fun onUserHandsWave(userUuid: String, duration: Int, payload: Map<String, Any>?) {
            iterateListeners {
                it.onUserHandsWave(userUuid, duration, payload)
            }
        }

        override fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?) {
            iterateListeners {
                it.onUserHandsDown(userUuid, payload)
            }
        }

        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            iterateListeners { listener ->
                listener.onLocalUserKickedOut()
            }
        }
    }

    private val providerStreamHandler = object : StreamHandler() {
        override fun onStreamJoined(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
                callbackBothKindsOfUserList()
            } else if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
                callbackScreenShare(streamInfo)
            }
        }

        override fun onStreamUpdated(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
                callbackBothKindsOfUserList()
            } else if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
                callbackScreenShare(streamInfo)
            }
        }

        override fun onStreamLeft(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
                callbackBothKindsOfUserList()
            } else if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
                callbackScreenShare(streamInfo, true)
            }
        }
    }

    private val providerMediaHandler = object : MediaHandler() {
        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            iterateListeners { listener ->
                listener.onVolumeChanged(volume, streamUuid)
            }
        }

        override fun onLocalDeviceStateUpdated(
            deviceInfo: AgoraEduContextDeviceInfo,
            state: AgoraEduContextDeviceState2
        ) {
            when (deviceInfo.deviceType) {
                AgoraEduContextDeviceType.Camera -> {
                    callbackBothKindsOfUserList(null, state)
                    localVideoState = state
                }
                AgoraEduContextDeviceType.Mic -> {
                    callbackBothKindsOfUserList(state, null)
                    localAudioState = state
                }

                else -> {}
            }
        }

        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            iterateListeners { listener ->
                listener.onAudioMixingStateChanged(state, errorCode)
            }
        }
    }

    init {
        eduContext?.widgetContext()?.addWidgetMessageObserver(widgetObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(widgetObserver, AgoraWidgetDefaultId.LargeWindow.id)
        eduContext?.roomContext()?.addHandler(roomHandler)
        eduContext?.userContext()?.addHandler(providerUserHandler)
        eduContext?.streamContext()?.addHandler(providerStreamHandler)
        eduContext?.mediaContext()?.addHandler(providerMediaHandler)
    }

    private fun callbackScreenShare(streamInfo: AgoraEduContextStreamInfo, stop: Boolean = false) {
//        eduContext?.userContext()?.getUserList(streamInfo.owner.role)?.find {
//            streamInfo.owner.userUuid == it.userUuid
//        }?.let {
//        }
        val coHostSet = getCoHostUuidSet()
        val userDetailInfo = toAgoraUserDetailInfo(
            streamInfo.owner,
            coHostSet.contains(streamInfo.owner.userUuid), streamInfo
        )
        iterateListeners { listener ->
            if (stop) {
                listener.onScreenShareStop(userDetailInfo)
            } else {
                listener.onScreenShareStart(userDetailInfo)
            }
        }

    }

    private fun callbackBothKindsOfUserList(
        localAudioState: AgoraEduContextDeviceState2? = null,
        localVideoState: AgoraEduContextDeviceState2? = null
    ) {
        callbackCoHostUserList(localAudioState, localVideoState)
        notifyUserListChanged(localAudioState, localVideoState)
    }

    private fun callbackCoHostUserList(
        localAudioState: AgoraEduContextDeviceState2? = null,
        localVideoState: AgoraEduContextDeviceState2? = null
    ) {
        eduContext?.userContext()?.getCoHostList()?.let { list ->
            toAgoraUserDetailInfo(list, localAudioState, localVideoState).let { detailUserList ->
                iterateListeners { listener ->
                    listener.onCoHostListChanged(detailUserList)
                }
            }
        }
    }

    fun notifyUserListChanged(
        localAudioState: AgoraEduContextDeviceState2? = null,
        localVideoState: AgoraEduContextDeviceState2? = null
    ) {
        eduContext?.userContext()?.getAllUserList()?.let { list ->
            toAgoraUserDetailInfo(list, localAudioState, localVideoState).let { detailUserList ->
                iterateListeners { listener ->
                    listener.onUserListChanged(detailUserList)
                }
            }
        }
    }

    fun notifyScreenShareDisplay() {
        eduContext?.streamContext()?.getAllStreamList()?.forEach { streamInfo ->
            if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen &&
                streamInfo.videoSourceState == AgoraEduContextMediaSourceState.Open
            ) {
                callbackScreenShare(streamInfo)
            }
        }
    }

    private fun getCoHostUuidSet(): Set<String> {
        val coHostList = mutableSetOf<String>()
        eduContext?.userContext()?.getCoHostList()?.forEach {
            coHostList.add(it.userUuid)
        }
        return coHostList
    }

    private fun toAgoraUserDetailInfo(
        list: List<AgoraEduContextUserInfo>,
        localAudioState: AgoraEduContextDeviceState2? = null,
        localVideoState: AgoraEduContextDeviceState2? = null
    ): List<AgoraUIUserDetailInfo> {
        val userList = mutableListOf<AgoraUIUserDetailInfo>()
        eduContext?.let { context ->
            val coHostSet = getCoHostUuidSet()
            val localUserUuid = eduContext.userContext()?.getLocalUserInfo()?.userUuid
            list.forEach { userInfo ->
                val streamInfoList = context.streamContext()?.getStreamList(userInfo.userUuid)
                val streamInfo = streamInfoList?.find {
                    it.videoSourceType == AgoraEduContextVideoSourceType.Camera
                }
                userList.add(
                    if (userInfo.userUuid == localUserUuid) {
                        toAgoraUserDetailInfo(
                            userInfo,
                            coHostSet.contains(userInfo.userUuid),
                            streamInfo, localAudioState, localVideoState
                        )
                    } else {
                        toAgoraUserDetailInfo(
                            userInfo,
                            coHostSet.contains(userInfo.userUuid), streamInfo
                        )
                    }
                )
            }
        }

        return userList
    }

    fun toAgoraUserDetailInfo(
        user: AgoraEduContextUserInfo,
        isCoHost: Boolean,
        stream: AgoraEduContextStreamInfo? = null,
        localAudioState: AgoraEduContextDeviceState2? = null,
        localVideoState: AgoraEduContextDeviceState2? = null
    ): AgoraUIUserDetailInfo {
        val audioSourceState = localAudioState?.let {
            toSourceState(it)
        } ?: stream?.audioSourceState ?: AgoraEduContextMediaSourceState.Close

        val videoSourceState = localVideoState?.let {
            toSourceState(it)
        } ?: stream?.videoSourceState ?: AgoraEduContextMediaSourceState.Close

        return AgoraUIUserDetailInfo(
            user.userUuid, user.userName, user.role,
            isCoHost, eduContext?.userContext()?.getUserRewardCount(user.userUuid) ?: 0,
            grantedUsers.contains(user.userUuid),
            eduContext?.userContext()?.getLocalUserInfo()?.userUuid == user.userUuid,
            hasAudio(stream),
            hasVideo(stream),
            stream?.streamUuid ?: "",
            stream?.streamName ?: "",
            stream?.streamType ?: AgoraEduContextMediaStreamType.None,
            stream?.audioSourceType ?: AgoraEduContextAudioSourceType.None,
            stream?.videoSourceType ?: AgoraEduContextVideoSourceType.None,
            audioSourceState, videoSourceState
        )
    }

    private fun hasAudio(stream: AgoraEduContextStreamInfo?): Boolean {
        return stream?.streamType == AgoraEduContextMediaStreamType.Both
            || stream?.streamType == AgoraEduContextMediaStreamType.Audio
    }

    private fun hasVideo(stream: AgoraEduContextStreamInfo?): Boolean {
        return stream?.streamType == AgoraEduContextMediaStreamType.Both
            || stream?.streamType == AgoraEduContextMediaStreamType.Video
    }

    private fun toSourceState(state: AgoraEduContextDeviceState2): AgoraEduContextMediaSourceState {
        return when (state) {
            AgoraEduContextDeviceState2.Open -> AgoraEduContextMediaSourceState.Open
            AgoraEduContextDeviceState2.Close -> AgoraEduContextMediaSourceState.Close
            AgoraEduContextDeviceState2.Error -> AgoraEduContextMediaSourceState.Error
        }
    }

    @Synchronized
    fun release() {
        listeners.clear()
        eduContext?.roomContext()?.removeHandler(roomHandler)
        eduContext?.userContext()?.removeHandler(providerUserHandler)
        eduContext?.streamContext()?.removeHandler(providerStreamHandler)
        eduContext?.mediaContext()?.removeHandler(providerMediaHandler)
    }
}

open class UIDataProviderListenerImpl : UIDataProviderListener {
    override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {

    }

    override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {

    }

    override fun onVolumeChanged(volume: Int, streamUuid: String) {

    }

    override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {

    }

    override fun onLocalUserKickedOut() {

    }

    override fun onScreenShareStart(info: AgoraUIUserDetailInfo) {

    }

    override fun onScreenShareStop(info: AgoraUIUserDetailInfo) {

    }

    override fun onHandsWaveEnable(enable: Boolean) {

    }

    override fun onUserHandsWave(userUuid: String, duration: Int, payload: Map<String, Any>?) {

    }

    override fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?) {

    }
}

data class AgoraUIUserDetailInfo(
    val userUuid: String,
    val userName: String,
    val role: AgoraEduContextUserRole,
    val isCoHost: Boolean,
    val reward: Int,
    var whiteBoardGranted: Boolean,
    val isLocal: Boolean,
    var hasAudio: Boolean,
    var hasVideo: Boolean,
    val streamUuid: String,
    val streamName: String?,
    val streamType: AgoraEduContextMediaStreamType,
    val audioSourceType: AgoraEduContextAudioSourceType,
    val videoSourceType: AgoraEduContextVideoSourceType,
    var audioSourceState: AgoraEduContextMediaSourceState,
    var videoSourceState: AgoraEduContextMediaSourceState
) {

    fun copy(): AgoraUIUserDetailInfo {
        return AgoraUIUserDetailInfo(
            userUuid, userName, role, isCoHost, reward, whiteBoardGranted,
            isLocal, hasAudio, hasVideo, streamUuid, streamName, streamType, audioSourceType,
            videoSourceType, audioSourceState, videoSourceState
        )
    }

    /**
     * whether audio device is enable
     * */
    fun isAudioEnable(): Boolean {
        return this.audioSourceState == AgoraEduContextMediaSourceState.Open
    }

    fun isVideoEnable(): Boolean {
        return this.videoSourceState == AgoraEduContextMediaSourceState.Open
    }

    override fun toString(): String {
        return "AgoraUIUserDetailInfo(userUuid='$userUuid', userName='$userName', role=$role, isCoHost=$isCoHost, reward=$reward, whiteBoardGranted=$whiteBoardGranted, isLocal=$isLocal, hasAudio=$hasAudio, hasVideo=$hasVideo, streamUuid='$streamUuid', streamName=$streamName, streamType=$streamType, audioSourceType=$audioSourceType, videoSourceType=$videoSourceType, audioSourceState=$audioSourceState, videoSourceState=$videoSourceState)"
    }

}