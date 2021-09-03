package io.agora.edu.classroom

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.R
import io.agora.edu.classroom.bean.PropertyData
import io.agora.edu.classroom.bean.PropertyData.FLEX
import io.agora.edu.common.api.RoomPre
import io.agora.edu.common.bean.request.DeviceStateUpdateReq
import io.agora.edu.common.bean.roompre.DeviceStateBean
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
import io.agora.educontext.*
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rte.RteEngineImpl
import io.agora.rte.data.RteLocalAudioState
import io.agora.rte.data.RteLocalVideoState
import io.agora.rte.data.RteRemoteAudioState
import io.agora.rte.data.RteRemoteVideoState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

open class BaseManager(
        protected val context: Context,
        protected val launchConfig: AgoraEduLaunchConfig,
        protected var eduRoom: EduRoom?,
        protected var eduUser: EduUser
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

    protected var baseManagerEventListener: BaseManagerEventListener? = null

    private val renderStateMachine = RenderStateMachine(object : RenderStateListener {
        override fun onCreateSurface(parent: ViewGroup, streamId: String?): SurfaceView {
            Log.d(tag, "onCreateSurface, $streamId")
            val surface = RtcEngine.CreateRendererView(parent.context.applicationContext)
            streamId?.let {
                surface.tag = it
                surface.setZOrderMediaOverlay(true)
                surface.layoutParams = ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT)
                parent.addView(surface)
                var uid: Int = (it.toLong() and 0xffffffffL).toInt()
                if (it == eduUser.userInfo.streamUuid) uid = 0
                val videoCanvas = VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, launchConfig.roomUuid, uid)

                if (uid == 0) {
                    RteEngineImpl.setupLocalVideo(videoCanvas)
                } else {
                    RteEngineImpl.setupRemoteVideo(videoCanvas)
                }
            }

            return surface
        }

        override fun onRemoveSurface(parent: ViewGroup, surfaceView: SurfaceView, streamId: String?) {
            Log.d(tag, "onRemoveSurface, $streamId")
            streamId?.let {
                parent.removeView(surfaceView)
                var uid: Int = (it.toLong() and 0xffffffffL).toInt()
                if (it == eduUser.userInfo.streamUuid) uid = 0
                val videoCanvas = VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, launchConfig.roomUuid, uid)
                if (uid == 0) {
                    RteEngineImpl.setupLocalVideo(videoCanvas)
                } else {
                    RteEngineImpl.setupRemoteVideo(videoCanvas)
                }
            }
        }
    })

    init {
        roomPre = RoomPreImpl(launchConfig.appId, launchConfig.roomUuid)
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

    fun getAgoraCustomProps(userUuid: String): MutableMap<String, String>? {
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

    fun startPreview(container: ViewGroup) {
        val streamId = eduUser.userInfo.streamUuid
        val option = LocalStreamInitOptions(streamId, true, false)
        val renderState = renderStateMachine.currentState(container, streamId)
        if (renderState == RenderState.Idle) {
            eduUser.initOrUpdateLocalStream(option, object : EduCallback<EduStreamInfo> {
                override fun onSuccess(res: EduStreamInfo?) {
                    renderStateMachine.startPreview(container, streamId)
                }

                override fun onFailure(error: EduError) {

                }
            })
        } else {
            Log.d(tag, "startPreview ${eduUser.userInfo.streamUuid}, invalid " +
                    "render state: ${renderState.name}")
        }
    }

    fun stopPreview(container: ViewGroup) {
        val option = LocalStreamInitOptions(eduUser.userInfo.streamUuid, false, false)
        val renderState = renderStateMachine.currentState(container, eduUser.userInfo.streamUuid)
        if (renderState == RenderState.Previewing) {
            eduUser.initOrUpdateLocalStream(option, object : EduCallback<EduStreamInfo> {
                override fun onSuccess(res: EduStreamInfo?) {
                    renderStateMachine.stopPreview(container)
                }

                override fun onFailure(error: EduError) {

                }
            })
        } else {
            Log.d(tag, "stopPreview ${eduUser.userInfo.streamUuid}, invalid " +
                    "render state: ${renderState.name}")
        }
    }

    fun publishLocalStream(parent: ViewGroup, hasAudio: Boolean, hasVideo: Boolean) {
        buildLocalStream()
        localStream?.let { streamInfo ->
            streamInfo.hasAudio = hasAudio
            streamInfo.hasVideo = hasVideo

            val renderState = renderStateMachine.currentState(parent, streamInfo.streamUuid)
            if (renderState == RenderState.Previewing) {
                eduUser.publishStream(streamInfo, object : EduCallback<Boolean> {
                    override fun onSuccess(res: Boolean?) {
                        res?.let {
                            Log.d(tag, "publish local stream ${if (res) "success" else "fail"}")
                            if (res) {
                                renderStateMachine.publish(parent, streamInfo.streamUuid)
                            }
                        }
                    }

                    override fun onFailure(error: EduError) {
                        Log.d(tag, "publish local stream fail: code ${error.msg}, msg ${error.msg}")
                    }
                })
            } else {
                Log.d(tag, "publishLocalStream ${eduUser.userInfo.streamUuid}, invalid " +
                        "render state: ${renderState.name}")
            }
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
                    openVideo, openAudio, it.hasVideo, it.hasAudio),
                    object : EduCallback<EduStreamInfo> {
                        override fun onSuccess(res: EduStreamInfo?) {
                            res!!.hasVideo = openVideo
                            res!!.hasAudio = openAudio
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
                roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(camera = value))
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
                roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(camera = value))
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
                        if (it.userUuid == launchConfig.userUuid) {
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
                roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(mic = value))
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
                roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(mic = value))
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
                    res?.find { it.userUuid == launchConfig.userUuid }?.let {
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
        if (info.user.userUuid == launchConfig.userUuid) {
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

class RenderStateMachine(private val listener: RenderStateListener) {

    companion object {
        const val tag = "RenderStateMachine"
    }

    // Which parents have their rendering surface views.
    // If a parent view does have a surface view, it must be
    // previewing. Otherwise, the parent view isn't rendering
    // anything, and
    private val parentMap = mutableMapOf<ViewGroup, SurfaceView>()

    // Which streams have surface to render.
    // If a stream has a surface here, it means it is publishing
    // a video stream while previewing the video content on a surface.
    // Otherwise, it it not publishing video streams, but it may
    // or may not preview the content.
    private val streamIdMap = mutableMapOf<String, SurfaceView>()

    // If the content of a surface belongs to a video stream
    // If a surface (e.g., found in parent map) does no belong
    // to any video stream, we can say it is previewing the content
    // but not publishing video streams.
    private val surfaceMap = mutableMapOf<SurfaceView, String>()

    /**
     * Only works for local video rendering
     */
    @Synchronized
    fun startPreview(parent: ViewGroup, streamId: String? = null) {
        if (currentState(parent, streamId) == RenderState.Idle) {
            parentMap[parent] = listener.onCreateSurface(parent, streamId)
        }
    }

    /**
     * Only works for local video rendering
     */
    @Synchronized
    fun stopPreview(parent: ViewGroup, streamId: String? = null) {
        if (currentState(parent, streamId) == RenderState.Previewing) {
            val surface = parentMap[parent]
            surfaceMap.remove(surface)
            parentMap.remove(parent)

            surface?.let {
                listener.onRemoveSurface(parent, it, streamId)
            }
        }
    }

    /**
     * Assume that only a previewing video can be published to
     * remote users.
     * Strictly restrict the behavior of publish to only publish
     * a previewing state video surface; otherwise calling this
     * method does nothing.
     */
    @Synchronized
    fun publish(parent: ViewGroup, streamId: String) {
        val state = currentState(parent, streamId)
        if (state == RenderState.Previewing) {
            parentMap[parent]?.let {
                streamIdMap[streamId] = it
                surfaceMap[it] = streamId
            }
        } else {
            Log.d(tag, "Illegal state when publishing: ${state.name}, " +
                    "a stream can only be published when previewing")
        }
    }

    /**
     * Stop publishing video streams to remote users, but local
     * capture and previewing continues.
     * Only call this method when it is publishing video;
     * otherwise nothing will be done.
     */
    @Synchronized
    fun unPublish(parent: ViewGroup, streamId: String) {
        val state = currentState(parent, streamId)
        if (state == RenderState.Published) {
            parentMap[parent]?.let {
                streamIdMap.remove(streamId)
                surfaceMap.remove(it)
            }
        } else {
            Log.d(tag, "Illegal state when unPublishing: ${state.name}, " +
                    "a stream can only be unPublished when it is already published")
        }
    }

    /**
     * Entirely stops video capturing, previewing and publishing.
     * Takes effect only when it is publishing video streams.
     * After calling this method successfully, the state will
     * transform from "published" to "idle".
     */
    @Synchronized
    fun mute(parent: ViewGroup, streamId: String) {
        val state = currentState(parent, streamId)
        if (state == RenderState.Published) {
            streamIdMap.remove(streamId)
            parentMap.remove(parent)?.let {
                surfaceMap.remove(it)
                listener.onRemoveSurface(parent, it, streamId)
            }
        } else {
            Log.d(tag, "Illegal state when mute $streamId, state ${state.name}, " +
                    " a stream can be muted when idle")
        }
    }

    /**
     * Starts capturing, previewing and publishing a stream.
     * Takes effect only when it is in idle state.
     * Eventually the state will transform from "idle" to
     * "published" if successfully.
     */
    @Synchronized
    fun unmute(parent: ViewGroup, streamId: String) {
        val state = currentState(parent, streamId)
        if (state == RenderState.Idle) {
            listener.onCreateSurface(parent, streamId).let {
                parentMap[parent] = it
                streamIdMap[streamId] = it
                surfaceMap[it] = streamId
            }
        } else {
            Log.d(tag, "Illegal state when unmute $streamId, state ${state.name}, " +
                    "a stream can be unmuted only when already published")
        }
    }

    /**
     * @param parent the parent view of previewing surface
     * @param streamId if not null, find a if a previewing
     *  video is publishing streams; otherwise, do not care
     *  about whether it is publishing streams, only check
     *  for previewing behavior
     */
    @Synchronized
    fun currentState(parent: ViewGroup, streamId: String?): RenderState {
        if (!parentMap.containsKey(parent)) {
            streamId?.let { stream ->
                streamIdMap[stream]?.let { surface ->
                    Log.d(tag, "Surface $surface instance exists but its parent " +
                            "does not preview stream $streamId any more, clear now")
                    surfaceMap.remove(surface)
                    streamIdMap.remove(streamId)
                }
            }

            return RenderState.Idle
        }

        val prevSurface = parentMap[parent]
        if (prevSurface == null) {
            Log.d(tag, "It should be previewing but surface view lost")
            return RenderState.Unknown
        }

        // Cases when the current surface corresponds to
        // no published streams ever.
        val publishedStreamId = surfaceMap[prevSurface]
        if (publishedStreamId != null) {
            if (publishedStreamId == streamId) {
                Log.d(tag, "Surface is publishing the video to the same stream $publishedStreamId")
            } else if (streamId == null) {
                Log.d(tag, "Surface is actually publishing the video, but the stream" +
                        " id is not passed as the parameter")
            } else {
                Log.d(tag, "Surface is publishing the video to a different stream " +
                        "$publishedStreamId, different to requested id $streamId")
            }
            return RenderState.Published
        }

        // Cases when the current surface already has a
        // stream to publish
        val publishedSurface = streamIdMap[streamId]
        if (publishedSurface != null) {
            return if (publishedSurface == prevSurface) {
                Log.d(tag, "Surface is publishing to stream $streamId")
                RenderState.Published
            } else {
                Log.d(tag, "Illegal surface and stream states")
                RenderState.Unknown
            }
        }

        return RenderState.Previewing
    }

    @Synchronized
    fun findPreviewParent(streamId: String): ViewGroup? {
        parentMap.iterator().forEach {
            if (it.value.tag.toString() == streamId) {
                return it.key
            }
        }
        return null
    }

    @Synchronized
    fun currentState(streamId: String): RenderState {
        // If the stream does not have a surface, it must
        // not be performed any operation, so it is in idle state
        val parent = findPreviewParent(streamId) ?: return RenderState.Idle
        val surface = parentMap[parent]
        val surface1 = streamIdMap[streamId]

        if (surface == surface1 && surfaceMap[surface] == streamId) {
            return RenderState.Published
        }

        return RenderState.Previewing
    }

    fun reset() {
        parentMap.clear()
        streamIdMap.clear()
        surfaceMap.clear()
    }
}

enum class RenderState {
    Unknown,
    Idle,
    Previewing,
    Published,
}

interface RenderStateListener {
    fun onCreateSurface(parent: ViewGroup, streamId: String? = null): SurfaceView

    fun onRemoveSurface(parent: ViewGroup, surfaceView: SurfaceView, streamId: String?)
}