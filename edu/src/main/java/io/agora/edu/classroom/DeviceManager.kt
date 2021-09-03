package io.agora.edu.classroom

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import io.agora.edu.common.bean.request.DeviceStateUpdateReq
import io.agora.edu.common.bean.roompre.DeviceStateBean
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.LocalStreamInitOptions
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.educontext.*
import io.agora.educontext.EduContextCameraFacing.Back
import io.agora.educontext.EduContextCameraFacing.Front
import io.agora.rte.RteEngineImpl

class DeviceManager(
        context: Context,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser,
        private val eduContext: EduContextPool??
) : BaseManager(context, launchConfig, eduRoom, eduUser) {
    override var tag = "DeviceManager"

    private val deviceConfig = EduContextDeviceConfig()
    var eventListener: DeviceManagerEventListener? = null

    fun initDeviceConfig() {
        deviceConfig.speakerEnabled = RteEngineImpl.isSpeakerphoneEnabled()
        val deviceJson: String? = getProperty(eduUser.userInfo.userProperties, DeviceStateBean.DEVICES)
        if (!TextUtils.isEmpty(deviceJson)) {
            val stateBean: DeviceStateBean = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
            deviceConfig.cameraEnabled = stateBean.camera != EduContextDeviceState.Closed.value
            deviceConfig.cameraFacing = if (stateBean.facing == Back.value) Back else Front
            if (deviceConfig.cameraFacing == Back) {
                eduUser.switchCamera()
            }
            deviceConfig.micEnabled = stateBean.mic != EduContextDeviceState.Closed.value
            checkDeviceConfig()
            deviceConfig.speakerEnabled = stateBean.speaker != State.NO.value
            RteEngineImpl.setEnableSpeakerphone(deviceConfig.speakerEnabled)
        }
    }

    fun checkDeviceConfig() {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.publisher.userUuid == launchConfig.userUuid }?.let { info ->
                    var camera = if (!deviceConfig.cameraEnabled) false else info.hasVideo
                    var mic = if (!deviceConfig.micEnabled) false else info.hasAudio
                    val options = LocalStreamInitOptions(info.streamUuid, camera, mic, info.hasVideo,
                            info.hasAudio)
                    eduUser.initOrUpdateLocalStream(options, object : EduCallback<EduStreamInfo> {
                        override fun onSuccess(res: EduStreamInfo?) {
                        }

                        override fun onFailure(error: EduError) {
                        }
                    })
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun syncDeviceConfig() {
        val req = DeviceStateUpdateReq(if (deviceConfig.cameraEnabled) 1 else 0,
                deviceConfig.cameraFacing.value, if (deviceConfig.micEnabled) 1 else 0,
                if (deviceConfig.speakerEnabled) 1 else 0)
        roomPre.updateDeviceState(launchConfig.userUuid, req)
    }

    @Synchronized
    fun getDeviceConfig(): EduContextDeviceConfig {
        return deviceConfig
    }

    private fun setMediaDeviceEnable(runnable: Runnable) {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.publisher.userUuid == launchConfig.userUuid }?.let { info ->
                    refreshLocalDevice(info.streamUuid, info.hasVideo, info.hasAudio, runnable)
                    return
                }
                // local stream is null, local user is not staging
                getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                    override fun onSuccess(res: MutableList<EduUserInfo>?) {
                        res?.find { it.userUuid == launchConfig.userUuid }?.let { info ->
                            // local user is not Staging, default unMute
                            refreshLocalDevice(info.streamUuid, hasVideo = true, hasAudio = true,
                                    runnable = runnable)
                        }
                    }

                    override fun onFailure(error: EduError) {
                    }
                })
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun refreshLocalDevice(streamUuid: String, hasVideo: Boolean, hasAudio: Boolean,
                                   runnable: Runnable) {
        val options = LocalStreamInitOptions(streamUuid, deviceConfig.cameraEnabled,
                deviceConfig.micEnabled, hasVideo, hasAudio)
        eduUser.initOrUpdateLocalStream(options, object : EduCallback<EduStreamInfo> {
            override fun onSuccess(res: EduStreamInfo?) {
                runnable.run()
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun setCameraDeviceEnable(enable: Boolean) {
        deviceConfig.cameraEnabled = enable
        eventListener?.onCameraDeviceEnableChanged(enable)
        setMediaDeviceEnable(Runnable {
            eduContext?.deviceContext()?.getHandlers()?.forEach {
                it.onCameraDeviceEnableChanged(enable)
            }
        })
    }

    fun switchCameraFacing() {
        deviceConfig.cameraFacing = if (deviceConfig.cameraFacing == Front) Back else Front
        val code = eduUser.switchCamera()
        val value = deviceConfig.cameraFacing.value
        roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(facing = value))
        eduContext?.deviceContext()?.getHandlers()?.forEach {
            val facing = deviceConfig.cameraFacing
            it.onCameraFacingChanged(facing)
        }
    }

    fun setMicDeviceEnable(enable: Boolean) {
        deviceConfig.micEnabled = enable
        eventListener?.onMicDeviceEnabledChanged(enable)
        setMediaDeviceEnable(Runnable {
            eduContext?.deviceContext()?.getHandlers()?.forEach {
                it.onMicDeviceEnabledChanged(enable)
            }
        })
    }

    fun setSpeakerEnable(enable: Boolean) {
        deviceConfig.speakerEnabled = enable
        val code = RteEngineImpl.setEnableSpeakerphone(enable)
        val value = if (enable) 1 else 0
        roomPre.updateDeviceState(launchConfig.userUuid, DeviceStateUpdateReq(speaker = value))
        eduContext?.deviceContext()?.getHandlers()?.forEach {
            it.onSpeakerEnabledChanged(enable)
        }
    }
}

interface DeviceManagerEventListener {
    fun onCameraDeviceEnableChanged(enabled: Boolean)
    fun onMicDeviceEnabledChanged(enabled: Boolean)
}