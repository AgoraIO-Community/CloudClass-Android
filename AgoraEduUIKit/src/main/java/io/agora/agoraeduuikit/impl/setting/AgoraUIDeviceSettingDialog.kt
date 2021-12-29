package io.agora.agoraeduuikit.impl.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler3
import io.agora.agoraeduuikit.util.AppUtil
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceType.Camera
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceType.Mic
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceType.Speaker

@SuppressLint("InflateParams")
class AgoraUIDeviceSettingDialog(context: Context, anchor: View,
                                 exitClickHandler: Runnable,
                                 private val eduContext: EduContextPool?) : Dialog(context, R.style.agora_dialog) {
    private val tag = "AgoraUIDeviceSettingDia"

    private val clickInterval = 500L
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_status_bar_setting_dialog_layout,
            null, false)
    private val cameraSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_camera_switch)
    private val micSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_mic_switch)
    private val speakerSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_loudspeaker_switch)
    private val facingFront: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_front)
    private val facingBack: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_back)

    private var cameras: MutableList<AgoraEduContextDeviceInfo>? = null
    private lateinit var curCameraInfo: AgoraEduContextDeviceInfo
    private lateinit var curMicInfo: AgoraEduContextDeviceInfo
    private lateinit var curSpeakerInfo: AgoraEduContextDeviceInfo

    private val eventHandler = object : MediaHandler3() {
        override fun onLocalDeviceStateUpdated(deviceInfo: AgoraEduContextDeviceInfo, state: AgoraEduContextDeviceState2) {
            super.onLocalDeviceStateUpdated(deviceInfo, state)
            when (deviceInfo.deviceType) {
                AgoraEduContextDeviceType.Camera -> {
                    cameraSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(state)
                    facingFront.isActivated = deviceInfo.isFrontCamera()
                    facingBack.isActivated = !deviceInfo.isFrontCamera()
                }
                AgoraEduContextDeviceType.Mic -> {
                    micSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(state)
                }
                AgoraEduContextDeviceType.Speaker -> {
                    speakerSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(state)
                }
            }
        }
    }

    init {
        setContentView(layout)
        val content = layout.findViewById<LinearLayout>(R.id.agora_setting_dialog_layout)
        content.elevation = 10f
        content.clipToOutline = true

        hideStatusBar(window!!)
        adjustPosition(anchor,
                content.resources.getDimensionPixelSize(R.dimen.agora_setting_dialog_width),
                content.resources.getDimensionPixelSize(R.dimen.agora_setting_dialog_height))

        cameras = eduContext?.mediaContext()?.getLocalDevices(Camera)?.toMutableList()
        val mics = eduContext?.mediaContext()?.getLocalDevices(Mic)
        val speakers = eduContext?.mediaContext()?.getLocalDevices(Speaker)
        cameras?.find { it.isFrontCamera() }?.let {
            curCameraInfo = it
            eduContext?.mediaContext()?.getLocalDeviceState(it, object : EduContextCallback<AgoraEduContextDeviceState2> {
                override fun onSuccess(target: AgoraEduContextDeviceState2?) {
                    target?.let {
                        cameraSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(it)
                    }
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
            facingFront.isActivated = it.isFrontCamera()
            facingBack.isActivated = !it.isFrontCamera()
        }
        if (!mics.isNullOrEmpty()) {
            curMicInfo = mics[0]
            eduContext?.mediaContext()?.getLocalDeviceState(curMicInfo, object : EduContextCallback<AgoraEduContextDeviceState2> {
                override fun onSuccess(target: AgoraEduContextDeviceState2?) {
                    target?.let {
                        micSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(it)
                    }
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
        }
        if (!speakers.isNullOrEmpty()) {
            curSpeakerInfo = speakers[0]
            eduContext?.mediaContext()?.getLocalDeviceState(curSpeakerInfo, object : EduContextCallback<AgoraEduContextDeviceState2> {
                override fun onSuccess(target: AgoraEduContextDeviceState2?) {
                    target?.let {
                        speakerSwitch.isActivated = AgoraEduContextDeviceState2.isDeviceOpen(it)
                    }
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
        }

        cameraSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            if (cameraSwitch.isActivated) {
                eduContext?.mediaContext()?.closeLocalDevice(curCameraInfo)
            } else {
                eduContext?.mediaContext()?.openLocalDevice(curCameraInfo)
            }
        }

        facingFront.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.mediaContext()?.closeLocalDevice(curCameraInfo)
            cameras?.find { it.isFrontCamera() }?.let {
                curCameraInfo = it
                eduContext?.mediaContext()?.openLocalDevice(curCameraInfo)
            }
        }

        facingBack.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            // default is front, so close front first and open back.
            eduContext?.mediaContext()?.closeLocalDevice(curCameraInfo)
            cameras?.find { !it.isFrontCamera() }?.let {
                curCameraInfo = it
                eduContext?.mediaContext()?.openLocalDevice(curCameraInfo)
            }
        }

        micSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            if (micSwitch.isActivated) {
                eduContext?.mediaContext()?.closeLocalDevice(curMicInfo)
            } else {
                eduContext?.mediaContext()?.openLocalDevice(curMicInfo)
            }
        }

        speakerSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            if (speakerSwitch.isActivated) {
                eduContext?.mediaContext()?.closeLocalDevice(curSpeakerInfo)
            } else {
                eduContext?.mediaContext()?.openLocalDevice(curSpeakerInfo)
            }
        }

        eduContext?.mediaContext()?.addHandler(eventHandler)
    }

    override fun show() {
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        super.show()
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun adjustPosition(anchor: View, width: Int, height: Int) {
        val window = window
        val params = window!!.attributes
        hideStatusBar(window)

        params.width = width
        params.height = height
        params.gravity = Gravity.TOP or Gravity.END

        val locationsOnScreen = IntArray(2)
        anchor.getLocationOnScreen(locationsOnScreen)
        params.x = anchor.left - locationsOnScreen[0]
        params.y = locationsOnScreen[1] + anchor.height + context.resources.getDimensionPixelSize(R.dimen.stroke_small) * 2
        window.attributes = params
    }
}