package io.agora.edu.core.internal.edu.classroom

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import io.agora.edu.core.AgoraEduCoreConfig
import io.agora.edu.core.context.*
import io.agora.edu.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.edu.core.internal.edu.common.bean.roompre.DeviceStateBean
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.EduRoom
import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.education.api.stream.data.LocalStreamInitOptions
import io.agora.edu.core.internal.framework.EduLocalUser
import io.agora.edu.core.internal.framework.EduUserInfo
import io.agora.edu.core.context.EduContextCameraFacing.Back
import io.agora.edu.core.context.EduContextCameraFacing.Front
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.rte.RteEngineImpl

internal class DeviceManager(
        context: Context,
        config: AgoraEduCoreConfig,
        eduRoom: EduRoom?,
        eduUser: EduLocalUser,
        private val eduContext: EduContextPool
) : BaseManager(context, config, eduRoom, eduUser, eduContext) {
    override var tag = "DeviceManager"

    private val deviceConfig = EduContextDeviceConfig()
    var eventListener: DeviceManagerEventListener? = null
    private var lastCameraDeviceEnableState: Boolean? = null
    private var lastMicDeviceEnableState: Boolean? = null

    fun initDeviceConfig() {
        deviceConfig.speakerEnabled = RteEngineImpl.isSpeakerphoneEnabled()
        val deviceJson: String? = getProperty(eduUser.userInfo.userProperties, DeviceStateBean.DEVICES)
        if (!TextUtils.isEmpty(deviceJson)) {
            val stateBean: DeviceStateBean = Gson().fromJson(deviceJson, DeviceStateBean::class.java)
            AgoraLog.i("$tag:initDeviceConfig:${Gson().toJson(stateBean)}")
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
                res?.find { it.publisher.userUuid == config.userUuid }?.let { info ->
                    val camera = if (!deviceConfig.cameraEnabled) false else info.hasVideo
                    val mic = if (!deviceConfig.micEnabled) false else info.hasAudio
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
        AgoraLog.i("$tag:syncDeviceConfig")
        val req = DeviceStateUpdateReq(if (deviceConfig.cameraEnabled) 1 else 0,
                deviceConfig.cameraFacing.value, if (deviceConfig.micEnabled) 1 else 0,
                if (deviceConfig.speakerEnabled) 1 else 0)
        roomPre.updateDeviceState(config.userUuid, req)
    }

    @Synchronized
    fun getDeviceConfig(): EduContextDeviceConfig {
        return deviceConfig
    }

    private fun setMediaDeviceEnable(runnable: Runnable) {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.publisher.userUuid == config.userUuid }?.let { info ->
                    refreshLocalDevice(info.streamUuid, info.hasVideo, info.hasAudio, runnable)
                    return
                }
                // local stream is null, local user is not staging
                getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                    override fun onSuccess(res: MutableList<EduUserInfo>?) {
                        res?.find { it.userUuid == config.userUuid }?.let { info ->
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
        AgoraLog.i("$tag:setCameraDeviceEnable:$enable")
        deviceConfig.cameraEnabled = enable
        eventListener?.onCameraDeviceEnableChanged(enable)
        setMediaDeviceEnable(Runnable {
            eduContext.deviceContext()?.getHandlers()?.forEach {
                it.onCameraDeviceEnableChanged(enable)
            }
        })
    }

    fun switchCameraFacing() {
        deviceConfig.cameraFacing = if (deviceConfig.cameraFacing == Front) Back else Front
        AgoraLog.i("$tag:switchCameraFacing:${deviceConfig.cameraFacing.name}")
        eduUser.switchCamera()
        val value = deviceConfig.cameraFacing.value
        roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(facing = value))
        eduContext.deviceContext()?.getHandlers()?.forEach {
            val facing = deviceConfig.cameraFacing
            it.onCameraFacingChanged(facing)
        }
    }

    fun setMicDeviceEnable(enable: Boolean) {
        AgoraLog.i("$tag:setMicDeviceEnable:$enable")
        deviceConfig.micEnabled = enable
        eventListener?.onMicDeviceEnabledChanged(enable)
        setMediaDeviceEnable(Runnable {
            eduContext.deviceContext()?.getHandlers()?.forEach {
                it.onMicDeviceEnabledChanged(enable)
            }
        })
    }

    fun setSpeakerEnable(enable: Boolean) {
        AgoraLog.i("$tag:setSpeakerEnable:$enable")
        deviceConfig.speakerEnabled = enable
        RteEngineImpl.setEnableSpeakerphone(enable)
        val value = if (enable) 1 else 0
        roomPre.updateDeviceState(config.userUuid, DeviceStateUpdateReq(speaker = value))
        eduContext.deviceContext()?.getHandlers()?.forEach {
            it.onSpeakerEnabledChanged(enable)
        }
    }

    /**
     * Set the device lifecycle based on the View lifecycle
     * */
    fun setDeviceLifecycle(lifecycle: EduContextDeviceLifecycle) {
        when (lifecycle) {
            EduContextDeviceLifecycle.Stop -> {
                lastMicDeviceEnableState = deviceConfig.micEnabled
                lastCameraDeviceEnableState = deviceConfig.cameraEnabled
                setCameraDeviceEnable(false)
                setMicDeviceEnable(false)
            }
            EduContextDeviceLifecycle.Resume -> {
                lastCameraDeviceEnableState?.let {
                    setCameraDeviceEnable(it)
                }
                lastMicDeviceEnableState?.let {
                    setMicDeviceEnable(it)
                }
            }
        }
    }
}

interface DeviceManagerEventListener {
    fun onCameraDeviceEnableChanged(enabled: Boolean)
    fun onMicDeviceEnabledChanged(enabled: Boolean)
}