package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.forEach
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceState2.Companion.isDeviceOpen
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceType.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler
import io.agora.agoraeducore.core.internal.framework.proxy.MediaProxy
import io.agora.agoraeduuikit.impl.container.AgoraUIDeviceSetting
import io.agora.agoraeduuikit.util.AppUtil

class AgoraUIDeviceSettingPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var leaveRoomRunnable: Runnable? = null

    private val tag = "AgoraUIDeviceSettingDia"

    private val clickInterval = 500L

    @SuppressLint("InflateParams")
    private val layout = LayoutInflater.from(context).inflate(
        R.layout.agora_option_setting_popup_layout, null, false)

    // content layout is the area of the layout excluding the shadow
    // area of the .9 png background resource
    private val content = layout.findViewById(R.id.agora_setting_dialog_layout) as ViewGroup
    private val cameraSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_popup_camera_switch)
    private val micSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_popup_mic_switch)
    private val speakerSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_popup_volume_switch)
    private val facingFront: AppCompatTextView = layout.findViewById(R.id.agora_setting_popup_camera_front)
    private val facingBack: AppCompatTextView = layout.findViewById(R.id.agora_setting_popup_camera_back)

    private var eduContext: EduContextPool? = null
    private var parent: ViewGroup? = null

    private val eventHandler = object : MediaHandler() {
        override fun onLocalDeviceStateUpdated(deviceInfo: AgoraEduContextDeviceInfo,
                                               state: AgoraEduContextDeviceState2) {
            AgoraEduContextSysDeviceId.getSystemDevice(deviceInfo.deviceId)?.let { device ->
                layout.post {
                    when (device) {
                        AgoraEduContextSystemDevice.CameraFront,
                        AgoraEduContextSystemDevice.CameraBack -> {
                            cameraSwitch.isActivated = isDeviceOpen(state)
                            facingFront.isActivated = deviceInfo.isFrontCamera()
                            facingBack.isActivated = !deviceInfo.isFrontCamera()
                        }
                        AgoraEduContextSystemDevice.Microphone -> {
                            micSwitch.isActivated = isDeviceOpen(state)
                        }
                        AgoraEduContextSystemDevice.Speaker -> {
                            speakerSwitch.isActivated = isDeviceOpen(state)
                        }
                    }
                }
            }
        }
    }

    init {
        content.clipToOutline = true
        layout.findViewById<AppCompatTextView>(R.id.agora_setting_exit_button).setOnClickListener {
            dismiss()
            leaveRoomRunnable?.run()
        }

        setButtonClickListeners()
    }

    private fun setButtonClickListeners() {
        setFastClickAvoidanceListener(cameraSwitch) { activated ->
            val device = if (AgoraUIDeviceSetting.isFrontCamera()) {
                AgoraEduContextSystemDevice.CameraFront
            } else {
                AgoraEduContextSystemDevice.CameraBack
            }

            if (activated) {
                eduContext?.mediaContext()?.closeSystemDevice(device)
            } else {
                eduContext?.mediaContext()?.openSystemDevice(device)
            }
        }

        setFastClickAvoidanceListener(facingFront) { activated ->
            if (!AgoraUIDeviceSetting.isFrontCamera() &&
                !activated && cameraSwitch.isActivated) {
                eduContext?.mediaContext()?.openSystemDevice(
                    AgoraEduContextSystemDevice.CameraFront,
                    DeviceOperationCallback {
                        AgoraUIDeviceSetting.setFrontCamera(true)
                    })
            }
        }

        setFastClickAvoidanceListener(facingBack) { activated ->
            if (AgoraUIDeviceSetting.isFrontCamera() &&
                !activated && cameraSwitch.isActivated) {
                eduContext?.mediaContext()?.openSystemDevice(
                    AgoraEduContextSystemDevice.CameraBack,
                    DeviceOperationCallback {
                        AgoraUIDeviceSetting.setFrontCamera(false)
                    })
            }
        }

        setFastClickAvoidanceListener(micSwitch) { activated ->
            if (activated) {
                eduContext?.mediaContext()?.closeSystemDevice(
                    AgoraEduContextSystemDevice.Microphone)
            } else {
                eduContext?.mediaContext()?.openSystemDevice(
                    AgoraEduContextSystemDevice.Microphone)
            }
        }

        setFastClickAvoidanceListener(speakerSwitch) { activated ->
            if (activated) {
                eduContext?.mediaContext()?.closeSystemDevice(
                    AgoraEduContextSystemDevice.Speaker)
            } else {
                eduContext?.mediaContext()?.openSystemDevice(
                    AgoraEduContextSystemDevice.Speaker)
            }
        }
    }

    private fun setFastClickAvoidanceListener(view: View, worker: ((Boolean) -> Unit)?) {
        view.setOnClickListener {
            if (!AppUtil.isFastClick(clickInterval)) {
                worker?.invoke(view.isActivated)
            }
        }
    }

    fun initView(parent: ViewGroup, width: Int, height: Int) {
        this.parent = parent
        addView(layout, width, height)

        val param = layout.layoutParams as MarginLayoutParams
        param.width = width
        param.height = height
        layout.layoutParams = param
    }

    fun setEduContextPool(eduContext: EduContextPool?) {
        this.eduContext = eduContext
        resetDeviceStateButtons()
        eduContext?.mediaContext()?.addHandler(eventHandler)
    }

    private fun resetDeviceStateButtons() {
        facingFront.isActivated = AgoraUIDeviceSetting.isFrontCamera()
        facingBack.isActivated = !facingFront.isActivated

        cameraSwitch.isActivated = false
        eduContext?.mediaContext()?.let { mediaContext ->
            getSystemDeviceInfo(AgoraEduContextSystemDevice.CameraFront)?.let { deviceInfo ->
                mediaContext.getLocalDeviceState(deviceInfo, CameraStateCallback {
                    if (isDeviceOpen(it)) cameraSwitch.isActivated = true
                })
            }

            getSystemDeviceInfo(AgoraEduContextSystemDevice.CameraBack)?.let { deviceInfo ->
                mediaContext.getLocalDeviceState(deviceInfo, CameraStateCallback {
                    if (isDeviceOpen(it)) cameraSwitch.isActivated = true
                })
            }

            getSystemDeviceInfo(AgoraEduContextSystemDevice.Microphone)?.let { deviceInfo ->
                mediaContext.getLocalDeviceState(deviceInfo, CameraStateCallback {
                    micSwitch.isActivated = isDeviceOpen(it)
                })
            }

            getSystemDeviceInfo(AgoraEduContextSystemDevice.Speaker)?.let { deviceInfo ->
                mediaContext.getLocalDeviceState(deviceInfo, CameraStateCallback {
                    speakerSwitch.isActivated = isDeviceOpen(it)
                })
            }
        }
    }

    private fun getSystemDeviceInfo(device: AgoraEduContextSystemDevice): AgoraEduContextDeviceInfo? {
        val id = AgoraEduContextSystemDevice.getDeviceId(device)
        val info = MediaProxy.getSystemDeviceMap()[id]
        return info?.let { AgoraEduContextDeviceInfo(it.id, it.name, toEduContextDeviceType(it.type)) }
    }

    private fun toEduContextDeviceType(type: MediaProxy.DeviceType): AgoraEduContextDeviceType {
        return when (type) {
            MediaProxy.DeviceType.Camera -> Camera
            MediaProxy.DeviceType.Mic -> Mic
            MediaProxy.DeviceType.Speaker -> Speaker
        }
    }

    fun dismiss() {
        this.parent?.let {
            var contains = false
            it.forEach check@{ child ->
                if (child == this) {
                    contains = true
                    return@check
                }
            }

            if (contains) {
                it.removeView(this)
            }
        }
    }
}

internal class CameraStateCallback(private val success: ((AgoraEduContextDeviceState2) -> Unit)?)
    : EduContextCallback<AgoraEduContextDeviceState2> {
    override fun onSuccess(target: AgoraEduContextDeviceState2?) {
        target?.let { success?.invoke(it) }
    }

    override fun onFailure(error: EduContextError?) {

    }
}

internal class DeviceOperationCallback(private val success: (() -> Unit)?) : EduContextCallback<Unit> {
    override fun onSuccess(target: Unit?) {
        success?.invoke()
    }

    override fun onFailure(error: EduContextError?) {

    }
}