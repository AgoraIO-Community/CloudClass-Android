package io.agora.agoraeducore.core.internal.edu.classroom

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import io.agora.agoraeducontext.*
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.FLEX
import io.agora.agoraeducore.core.internal.edu.common.api.RoomPre
import io.agora.agoraeducore.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.agoraeducore.core.internal.edu.common.bean.roompre.DeviceStateBean
import io.agora.agoraeducore.core.internal.edu.common.impl.RoomPreImpl
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.EduRoom
import io.agora.agoraeducore.core.internal.framework.data.EduStreamEvent
import io.agora.agoraeducore.core.internal.framework.data.EduStreamInfo
import io.agora.agoraeducore.core.internal.education.api.stream.data.LocalStreamInitOptions
import io.agora.agoraeducore.core.internal.framework.data.VideoSourceType
import io.agora.agoraeducore.core.internal.framework.EduLocalUser
import io.agora.agoraeducore.core.internal.framework.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserRole
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.rte.data.RteLocalAudioState
import io.agora.agoraeducore.core.internal.rte.data.RteLocalVideoState
import io.agora.agoraeducore.core.internal.rte.data.RteRemoteAudioState
import io.agora.agoraeducore.core.internal.rte.data.RteRemoteVideoState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import io.agora.agoraeducore.R

internal open class BaseManager(
        protected val context: Context,
        protected val config: AgoraEduCoreConfig,
        protected var eduRoom: EduRoom?,
        protected var eduUser: EduLocalUser,
        private val eduContextPool: EduContextPool?
) {
    open var tag = "BaseManager"

    // Reported by remote users;key: userUuid, value: 0:UnAvailable 1:Available 2:Closed
    private val remoteCameraDeviceStateMap: MutableMap<String, Int> = ConcurrentHashMap()
    private var localCameraRteState = RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_STOPPED.value

    @Volatile
    private var localCameraDeviceState: Int? = null

    // Reported by remote users;key: userUuid, value: 0:UnAvailable 1:Available 2:Closed
    private val remoteMicDeviceStateMap: MutableMap<String, Int> = ConcurrentHashMap()
    private var localMicRteState = RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_STOPPED.value

    @Volatile
    private var localMicDeviceState: Int? = null

    protected var localStream: EduStreamInfo? = null
    protected val roomPre: RoomPre

    init {
        roomPre = RoomPreImpl(config.appId, config.roomUuid)
    }

    open fun dispose() {
        eduRoom = null
    }

    protected fun getCurRoomFullUser(callback: EduCallback<MutableList<EduUserInfo>>) {
        eduRoom?.getFullUserList(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(res: MutableList<EduUserInfo>?) {
                if (res != null) {
                    callback.onSuccess(res)
                } else {
                    callback.onFailure(EduError.internalError("current eduRoom`userList is null"))
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun getCurRoomFullStream(callback: EduCallback<MutableList<EduStreamInfo>>) {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(streams: MutableList<EduStreamInfo>?) {
                if (streams != null) {
                    callback.onSuccess(streams)
                } else {
                    callback.onFailure(EduError.internalError("current eduRoom`streamList is null"))
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun getProperty(properties: Map<String, Any>?, key: String): String? {
        if (properties != null) {
            for ((key1, value) in properties) {
                if (key1 == key) {
                    return Gson().toJson(value)
                }
            }
        }
        return null
    }

    fun getUserFlexProps(userUuid: String): MutableMap<String, String>? {
        val lock = CountDownLatch(1)
        var result: MutableMap<String, String>? = null
        getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(res: MutableList<EduUserInfo>?) {
                res?.find { it.userUuid == userUuid }?.let {
                    result = it.userProperties[FLEX] as? MutableMap<String, String>
                }
                lock.countDown()
            }

            override fun onFailure(error: EduError) {
                lock.countDown()
            }
        })
        try {
            lock.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    protected fun userRoleConvert(role: EduUserRole): EduContextUserRole {
        return when (role) {
            EduUserRole.TEACHER -> EduContextUserRole.Teacher
            EduUserRole.STUDENT -> EduContextUserRole.Student
            EduUserRole.ASSISTANT -> EduContextUserRole.Assistant
            else -> EduContextUserRole.Teacher
        }
    }

    protected fun userInfoConvert(info: EduBaseUserInfo): EduContextUserInfo {
        return EduContextUserInfo(info.userUuid, info.userName, userRoleConvert(info.role),
                getUserFlexProps(info.userUuid))
    }

    @Synchronized
    private fun buildLocalStream() {
        if (localStream == null) {
            localStream = EduStreamInfo(
                    eduUser.userInfo.streamUuid,
                    null,
                    VideoSourceType.CAMERA,
                    true,
                    true,
                    eduUser.userInfo)
        }
    }

    fun muteLocalAudio(isMute: Boolean) {
        buildLocalStream()
        localStream?.let {
            switchLocalVideoAudio(it.hasVideo, !isMute)
        }
    }

    fun muteLocalVideo(isMute: Boolean) {
        buildLocalStream()
        localStream?.let {
            switchLocalVideoAudio(!isMute, it.hasAudio)
        }
    }

    private fun switchLocalVideoAudio(openVideo: Boolean, openAudio: Boolean) {
        buildLocalStream()

        localStream?.let {
            // Update local stream state and then sync the states to remote server
            eduUser.initOrUpdateLocalStream(LocalStreamInitOptions(it.streamUuid,
                    openVideo, openAudio, openVideo, openAudio),
                    object : EduCallback<EduStreamInfo> {
                        override fun onSuccess(res: EduStreamInfo?) {
                            eduUser.muteStream(res!!, object : EduCallback<Boolean> {
                                override fun onSuccess(res: Boolean?) {}

                                override fun onFailure(error: EduError) {}
                            })
                        }

                        override fun onFailure(error: EduError) {}
                    })
        }
    }

    private fun localVideoIsMuted(): Boolean {
        return if (localStream == null) {
            false
        } else {
            !localStream!!.hasVideo
        }
    }

    private fun localAudioIsMuted(): Boolean {
        return if (localStream == null) {
            false
        } else {
            !localStream!!.hasAudio
        }
    }

    fun updateLocalCameraSwitchState(closed: Boolean) {
        getLocalCameraDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                val value = if (closed) 2 else 1
                localCameraDeviceState = value
                Log.e(tag, "updateLocalCameraSwitchState->$localCameraDeviceState")
                roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(camera = value))
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun updateLocalCameraAvailableState(state: Int) {
        getLocalCameraDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                if (res == EduContextDeviceState.Closed.value) {
                    return
                }
                Log.e(tag, "updateLocalCameraAvailableState->$localCameraDeviceState")
                localCameraRteState = state
                val cameraUnavailable = state == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.value
                        && !localVideoIsMuted()
                val value = if (cameraUnavailable) 0 else 1
                roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(camera = value))
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun getRemoteVideoRteState(streamUuid: String): Int {
        return eduUser.cachedRemoteVideoStates[streamUuid]
                ?: RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.value
    }

    private fun getRemoteCameraDeviceState(userUuid: String, callback: EduCallback<Int>) {
        val state = remoteCameraDeviceStateMap[userUuid]
        if (state == null) {
            getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.forEach {
                        if (it.userUuid == userUuid) {
                            val deviceJson: String? = getProperty(it.userProperties, DeviceStateBean.DEVICES)
                            if (!TextUtils.isEmpty(deviceJson)) {
                                val deviceStateBean: DeviceStateBean? = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
                                deviceStateBean?.let {
                                    remoteCameraDeviceStateMap[userUuid] = deviceStateBean.camera
                                            ?: EduContextDeviceState.Available.value
                                    callback.onSuccess(deviceStateBean.camera)
                                }
                            } else {
                                callback.onSuccess(EduContextDeviceState.Available.value)
                            }
                            return@forEach
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        } else {
            callback.onSuccess(state)
        }
    }

    @Synchronized
    private fun getLocalCameraDeviceState(callback: EduCallback<Int>) {
        if (localCameraDeviceState == null) {
            getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.forEach {
                        if (it.userUuid == config.userUuid) {
                            val deviceJson: String? = getProperty(it.userProperties, DeviceStateBean.DEVICES)
                            if (!TextUtils.isEmpty(deviceJson)) {
                                val deviceStateBean: DeviceStateBean? = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
                                deviceStateBean?.let {
                                    localCameraDeviceState = deviceStateBean.camera
                                            ?: EduContextDeviceState.Available.value
                                    callback.onSuccess(localCameraDeviceState)
                                }
                            } else {
                                callback.onSuccess(EduContextDeviceState.Available.value)
                            }
                            return@forEach
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        } else {
            callback.onSuccess(localCameraDeviceState)
        }
    }

    fun updateLocalMicSwitchState(closed: Boolean) {
        getLocalMicDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                val value = if (closed) 2 else 1
                localMicDeviceState = value
                Log.e(tag, "updateLocalMicSwitchState->$value")
                roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(mic = value))
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun updateLocalMicAvailableState(state: Int) {
        getLocalMicDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                Log.e(tag, "updateLocalMicAvailableState1->$res")
                if (res == EduContextDeviceState.Closed.value) {
                    return
                }
                Log.e(tag, "updateLocalMicAvailableState2->$state")
                localMicRteState = state
                val micUnavailable = state == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.value
                        && !localAudioIsMuted()
                val value = if (micUnavailable) 0 else 1
                roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(mic = value))
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun getRemoteAudioRteState(streamUuid: String): Int {
        return eduUser.cachedRemoteAudioStates[streamUuid]
                ?: RteRemoteAudioState.REMOTE_AUDIO_STATE_FROZEN.value
    }

    private fun getRemoteMicDeviceState(userUuid: String, callback: EduCallback<Int>) {
        val state = remoteMicDeviceStateMap[userUuid]
        if (state == null) {
            getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.find { it.userUuid == userUuid }?.let {
                        val deviceJson: String? = getProperty(it.userProperties, DeviceStateBean.DEVICES)
                        if (!TextUtils.isEmpty(deviceJson)) {
                            val deviceStateBean: DeviceStateBean? = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
                            remoteMicDeviceStateMap[userUuid] = deviceStateBean?.mic
                                    ?: EduContextDeviceState.Available.value
                            callback.onSuccess(remoteMicDeviceStateMap[userUuid])
                        } else {
                            callback.onSuccess(EduContextDeviceState.Available.value)
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        } else {
            callback.onSuccess(state)
        }
    }

    @Synchronized
    private fun getLocalMicDeviceState(callback: EduCallback<Int>) {
        if (localMicDeviceState == null) {
            getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.find { it.userUuid == config.userUuid }?.let {
                        val deviceJson: String? = getProperty(it.userProperties, DeviceStateBean.DEVICES)
                        if (!TextUtils.isEmpty(deviceJson)) {
                            val deviceStateBean: DeviceStateBean? = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
                            deviceStateBean?.let {
                                localMicDeviceState = deviceStateBean.mic
                                        ?: EduContextDeviceState.Available.value
                                callback.onSuccess(localMicDeviceState)
                            }
                        } else {
                            callback.onSuccess(EduContextDeviceState.Available.value)
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        } else {
            callback.onSuccess(localMicDeviceState)
        }
    }

    fun updateRemoteDeviceState(userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        val curUserProperties = userInfo.userProperties
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyData.CMD].toString().toFloat().toInt()
            if (causeType == PropertyData.DEVICE_STATE) {
                val deviceJson: String? = getProperty(curUserProperties, DeviceStateBean.DEVICES)
                if (!TextUtils.isEmpty(deviceJson)) {
                    val stateBean: DeviceStateBean? = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
                    stateBean?.camera?.let {
                        remoteCameraDeviceStateMap[userInfo.userUuid] = it
                    }
                    stateBean?.mic?.let {
                        remoteMicDeviceStateMap[userInfo.userUuid] = it
                    }
                }
            }
        }
    }

    /** init localDevice State from properties */
    fun initLocalDeviceState(config: EduContextDeviceConfig) {
        getLocalCameraDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                if (!config.cameraEnabled) {
                    localCameraDeviceState = EduContextDeviceState.Closed.value
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
        getLocalMicDeviceState(object : EduCallback<Int> {
            override fun onSuccess(res: Int?) {
                if (!config.micEnabled) {
                    localMicDeviceState = EduContextDeviceState.Closed.value
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    /** https://confluence.agoralab.co/pages/viewpage.action?pageId=719462858 */
    protected fun notifyUserDeviceState(info: EduContextUserDetailInfo, callback: EduCallback<Unit>) {
        if (info.user.userUuid == config.userUuid) {
            notifyLocalUserDeviceState(info, callback)
        } else {
            notifyRemoteUserDeviceState(info, callback)
        }
    }

    private fun notifyLocalUserDeviceState(info: EduContextUserDetailInfo, callback: EduCallback<Unit>) {
        // Local  Step-1
        // Determine if the micDevice is open
        getLocalMicDeviceState(object : EduCallback<Int> {
            override fun onSuccess(micState: Int?) {
                if (micState == EduContextDeviceState.Closed.value) {
                    info.microState = EduContextDeviceState.Closed
                } else {
                    // Determine if the audio is muted
                    if (info.enableAudio) {
                        if (localMicRteState == RteLocalAudioState.LOCAL_AUDIO_STREAM_STATE_FAILED.value) {
                            info.microState = EduContextDeviceState.UnAvailable
                        } else {
                            info.microState = EduContextDeviceState.Available
                        }
                    } else {
                        info.microState = EduContextDeviceState.Available
                    }
                }
                // Step-1
                // Determine if the cameraDevice is open
                getLocalCameraDeviceState(object : EduCallback<Int> {
                    override fun onSuccess(cameraState: Int?) {
                        if (cameraState == EduContextDeviceState.Closed.value) {
                            info.cameraState = EduContextDeviceState.Closed
                        } else {
                            // Determine if the video is muted
                            if (info.enableVideo) {
                                if (localCameraRteState == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.value) {
                                    info.cameraState = EduContextDeviceState.UnAvailable
                                } else {
                                    info.cameraState = EduContextDeviceState.Available
                                }
                            } else {
                                info.cameraState = EduContextDeviceState.Available
                            }
                        }
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(error: EduError) {
                    }
                })
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun notifyRemoteUserDeviceState(info: EduContextUserDetailInfo, callback: EduCallback<Unit>) {
        // Remote Step-1
        // Determine if the micDevice is open
        getRemoteMicDeviceState(info.user.userUuid, object : EduCallback<Int> {
            override fun onSuccess(micState: Int?) {
                if (micState == EduContextDeviceState.Closed.value) {
                    info.microState = EduContextDeviceState.Closed
                    // Step-2
                    notifyRemoteUserCameraDeviceState(info, callback)
                } else {
                    // Determine if the audio is muted
                    if (info.enableAudio) {
                        getRemoteMicDeviceState(info.user.userUuid, object : EduCallback<Int> {
                            override fun onSuccess(micState: Int?) {
                                if (micState == EduContextDeviceState.UnAvailable.value) {
                                    info.microState = EduContextDeviceState.UnAvailable
                                } else {
                                    val state = getRemoteAudioRteState(info.streamUuid)
                                    if (state == RteRemoteAudioState.REMOTE_AUDIO_STATE_FROZEN.value ||
                                            state == RteRemoteAudioState.REMOTE_AUDIO_STATE_STOPPED.value) {
                                        info.microState = EduContextDeviceState.UnAvailable
                                    } else {
                                        info.microState = EduContextDeviceState.Available
                                    }
                                }
                                // Step-2
                                notifyRemoteUserCameraDeviceState(info, callback)
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                    } else {
                        // user mute audio,default mic device available
                        info.microState = EduContextDeviceState.Available
                        // Step-2
                        notifyRemoteUserCameraDeviceState(info, callback)
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun notifyRemoteUserCameraDeviceState(info: EduContextUserDetailInfo, callback: EduCallback<Unit>) {
        // Determine if the cameraDevice is open
        getRemoteCameraDeviceState(info.user.userUuid, object : EduCallback<Int> {
            override fun onSuccess(cameraState: Int?) {
                if (cameraState == EduContextDeviceState.Closed.value) {
                    info.cameraState = EduContextDeviceState.Closed
                    callback.onSuccess(Unit)
                } else {
                    // Determine if the video is muted
                    if (info.enableVideo) {
                        getRemoteCameraDeviceState(info.user.userUuid, object : EduCallback<Int> {
                            override fun onSuccess(cameraState: Int?) {
                                if (cameraState == EduContextDeviceState.UnAvailable.value) {
                                    info.cameraState = EduContextDeviceState.UnAvailable
                                } else {
                                    val state = getRemoteVideoRteState(info.streamUuid)
                                    if (state == RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.value ||
                                            state == RteRemoteVideoState.REMOTE_VIDEO_STATE_STOPPED.value) {
                                        info.cameraState = EduContextDeviceState.UnAvailable
                                    } else {
                                        info.cameraState = EduContextDeviceState.Available
                                    }
                                }
                                callback.onSuccess(Unit)
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                    } else {
                        // user mute video,default camera device available
                        info.cameraState = EduContextDeviceState.Available
                        callback.onSuccess(Unit)
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun modifyLocalStream(stream: EduStreamInfo?, sourceType: VideoSourceType) {
        when (sourceType) {
            VideoSourceType.CAMERA -> {
                localStream = stream
            }
        }
    }

    fun addLocalStream(event: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to add local stream")
        modifyLocalStream(event.modifiedStream, event.modifiedStream.videoSourceType)
        Constants.AgoraLog.e("$tag:Local stream is added：" + localStream?.hasAudio + "," + localStream?.hasVideo)
    }

    fun updateLocalStream(event: EduStreamEvent) {
        event.operatorUser?.let { baseUserInfo ->
            if (baseUserInfo.userUuid != config.userUuid && localStream != null) {
                val info: EduStreamInfo = event.modifiedStream
                if (info.hasVideo != localStream!!.hasVideo) {
                    eduContextPool?.deviceContext()?.getHandlers()?.forEach {
                        it.onDeviceTips(context.getString(
                                if (info.hasVideo) R.string.camera_turned_on
                                else R.string.camera_turned_off))
                    }
                }
                if (info.hasAudio != localStream!!.hasAudio) {
                    eduContextPool?.deviceContext()?.getHandlers()?.forEach {
                        it.onDeviceTips(context.getString(if (info.hasAudio)
                            R.string.microphone_turned_on else R.string.microphone_turned_off))
                    }
                }
            }
        }
        Constants.AgoraLog.e("$tag:Receive callback to update local stream")
        modifyLocalStream(event.modifiedStream, event.modifiedStream.videoSourceType)
        Constants.AgoraLog.e("$tag:Local stream is updated：" + localStream?.hasAudio + "," + localStream?.hasVideo)
    }

    fun removeLocalStream(event: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to remove local stream")
        modifyLocalStream(null, event.modifiedStream.videoSourceType)
        Constants.AgoraLog.e("$tag:Local stream is removed")
    }
}