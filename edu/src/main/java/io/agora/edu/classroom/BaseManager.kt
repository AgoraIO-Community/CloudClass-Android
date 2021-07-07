package io.agora.edu.classroom

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import io.agora.edu.R
import io.agora.edu.classroom.bean.PropertyCauseType
import io.agora.edu.common.api.RoomPre
import io.agora.edu.common.bean.roompre.LocalDeviceState
import io.agora.edu.common.impl.RoomPreImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.LocalStreamInitOptions
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.Constants
import io.agora.educontext.DeviceState
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.EduContextUserRole
import io.agora.rte.data.RteLocalVideoState
import io.agora.rte.data.RteRemoteVideoState
import java.util.concurrent.ConcurrentHashMap

open class BaseManager(
        protected val context: Context,
        protected val launchConfig: AgoraEduLaunchConfig,
        protected var eduRoom: EduRoom?,
        protected var eduUser: EduUser
) {
    open var tag = "BaseManager"

    // Reported by remote users;key: userUuid, value: whether of the camera available
    private val remoteCameraStateMap: MutableMap<String, Int> = ConcurrentHashMap()
    private var localCameraState = RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_STOPPED.value
    protected var localStream: EduStreamInfo? = null
    private val roomPre: RoomPre

    protected var baseManagerEventListener: BaseManagerEventListener? = null

    init {
        roomPre = RoomPreImpl(launchConfig.appId, launchConfig.roomUuid)
    }

    fun dispose() {
        eduRoom = null
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

    protected fun userRoleConvert(role: EduUserRole): EduContextUserRole {
        return when (role) {
            EduUserRole.TEACHER -> EduContextUserRole.Teacher
            EduUserRole.STUDENT -> EduContextUserRole.Student
            EduUserRole.ASSISTANT -> EduContextUserRole.Assistant
            else -> EduContextUserRole.Teacher
        }
    }

    protected fun userInfoConvert(info: EduBaseUserInfo): EduContextUserInfo {
        return EduContextUserInfo(info.userUuid, info.userName, userRoleConvert(info.role))
    }

    public fun muteLocalAudio(isMute: Boolean) {
        if (localStream != null) {
            switchLocalVideoAudio(localStream!!.hasVideo, !isMute)
        }
    }

    public fun muteLocalVideo(isMute: Boolean) {
        if (localStream != null) {
            switchLocalVideoAudio(!isMute, localStream!!.hasAudio)
        }
    }

    protected fun switchLocalVideoAudio(openVideo: Boolean, openAudio: Boolean) {
        if (localStream == null) {
            Log.w(tag, "switch local video/audio fail because cannot find local camera stream")
            return
        }

        // Update local stream state and then sync the states to remote server
        eduUser.initOrUpdateLocalStream(LocalStreamInitOptions(localStream!!.streamUuid,
                openVideo, openAudio), object : EduCallback<EduStreamInfo> {
            override fun onSuccess(res: EduStreamInfo?) {
                eduUser.muteStream(res!!, object : EduCallback<Boolean> {
                    override fun onSuccess(res: Boolean?) {}
                    override fun onFailure(error: EduError) {}
                })
            }

            override fun onFailure(error: EduError) {}
        })
    }

    private fun localVideoIsMuted(): Boolean {
        return if (localStream == null) {
            false
        } else {
            !localStream!!.hasVideo
        }
    }

    fun updateLocalCameraState(state: Int) {
        this.localCameraState = state
        val cameraUnavailable = state == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.value
                && !localVideoIsMuted()
        roomPre.updateCameraState(launchConfig.userUuid, !cameraUnavailable)
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

    protected fun getRemoteVideoState(streamUuid: String): Int {
        return eduUser.cachedRemoteVideoStates[streamUuid]
                ?: RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.value
    }

    fun updateRemoteCameraState(userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        val curUserProperties: Map<String, Any> = userInfo.userProperties
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyCauseType.CMD].toString().toFloat().toInt()
            if (causeType == PropertyCauseType.CAMERA_STATE) {
                val deviceJson: String? = getProperty(curUserProperties, LocalDeviceState.DEVICES)
                if (!TextUtils.isEmpty(deviceJson)) {
                    val state: LocalDeviceState = Gson().fromJson(deviceJson, LocalDeviceState::class.java)
                    state?.let {
                        remoteCameraStateMap[userInfo.userUuid] = it.camera
                    }
                }
            }
        }
    }

    protected fun getRemoteCameraState(userUuid: String, callback: EduCallback<Int>) {
        val state = remoteCameraStateMap[userUuid]
        if (state == null) {
            getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.forEach {
                        if (it.userUuid == userUuid) {
                            val deviceJson: String? = getProperty(it.userProperties, LocalDeviceState.DEVICES)
                            if (!TextUtils.isEmpty(deviceJson)) {
                                val deviceState: LocalDeviceState? = Gson().fromJson(deviceJson, LocalDeviceState::class.java)
                                deviceState?.let {
                                    remoteCameraStateMap[userUuid] = deviceState.camera
                                    callback.onSuccess(deviceState.camera)
                                }
                            } else {
                                callback.onSuccess(LocalDeviceState.AVAILABLE)
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

    protected fun notifyUserDeviceState(info: EduContextUserDetailInfo, callback: EduCallback<Unit>) {
        if (info.user.userUuid == launchConfig.userUuid) {
            // Local
            // Temporarily default microphone available
            info.microState = DeviceState.Available
            // Determine if the camera is available
            if (info.enableVideo) {
                if (localCameraState == RteLocalVideoState.LOCAL_VIDEO_STREAM_STATE_FAILED.value) {
                    info.cameraState = DeviceState.UnAvailable
                } else {
                    info.cameraState = DeviceState.Available
                }
            } else {
                info.cameraState = DeviceState.Available
            }
            callback.onSuccess(Unit)
        } else {
            // Remote
            // Temporarily default microphone available
            info.microState = DeviceState.Available
            // Determine if the camera is available
            if (info.enableVideo) {
                val state = getRemoteVideoState(info.streamUuid)
                if (state == RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.value ||
                        state == RteRemoteVideoState.REMOTE_VIDEO_STATE_STOPPED.value) {
                    getRemoteCameraState(info.user.userUuid, object : EduCallback<Int> {
                        override fun onSuccess(cameraState: Int?) {
                            if (cameraState == LocalDeviceState.UNAVAILABLE) {
                                info.cameraState = DeviceState.UnAvailable
                            } else {
                                info.cameraState = DeviceState.Available
                            }
                            callback.onSuccess(Unit)
                        }

                        override fun onFailure(error: EduError) {
                        }
                    })
                } else {
                    info.cameraState = DeviceState.Available
                    callback.onSuccess(Unit)
                }
            } else {
                info.cameraState = DeviceState.Available
                callback.onSuccess(Unit)
            }
        }
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
            if (baseUserInfo.userUuid != launchConfig.userUuid && localStream != null) {
                val info: EduStreamInfo = event.modifiedStream
                if (info.hasVideo != localStream!!.hasVideo) {
                    baseManagerEventListener?.onMediaMsgUpdate(context.getString(if (info.hasVideo)
                        R.string.camera_turned_on else R.string.camera_turned_off))
                }
                if (info.hasAudio != localStream!!.hasAudio) {
                    baseManagerEventListener?.onMediaMsgUpdate(context.getString(if (info.hasAudio)
                        R.string.microphone_turned_on else R.string.microphone_turned_off))
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

interface BaseManagerEventListener {
    fun onMediaMsgUpdate(msg: String)
}