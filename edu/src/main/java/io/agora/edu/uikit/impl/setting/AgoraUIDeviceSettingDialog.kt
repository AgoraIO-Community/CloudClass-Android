package io.agora.edu.uikit.impl.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.R
import io.agora.edu.uikit.handlers.DeViceHandler
import io.agora.edu.uikit.util.AppUtil

@SuppressLint("InflateParams")
class AgoraUIDeviceSettingDialog(context: Context, anchor: View,
                                 exitClickHandler: Runnable,
                                 private val eduContext: io.agora.edu.core.context.EduContextPool?) : Dialog(context, R.style.agora_dialog) {
    private val TAG = "AgoraUIDeviceSettingDia"

    private val clickInterval = 500L
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_status_bar_setting_dialog_layout,
            null, false)
    private val cameraSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_camera_switch)
    private val micSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_mic_switch)
    private val speakerSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_loudspeaker_switch)
    private val facingFront: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_front)
    private val facingBack: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_back)

    private val eventHandler = object : DeViceHandler() {
        override fun onCameraDeviceEnableChanged(enabled: Boolean) {
            super.onCameraDeviceEnableChanged(enabled)
            cameraSwitch.isActivated = enabled
        }

        override fun onCameraFacingChanged(facing: io.agora.edu.core.context.EduContextCameraFacing) {
            super.onCameraFacingChanged(facing)
            facingFront.isActivated = facing == io.agora.edu.core.context.EduContextCameraFacing.Front
            facingBack.isActivated = facing == io.agora.edu.core.context.EduContextCameraFacing.Back
        }

        override fun onMicDeviceEnabledChanged(enabled: Boolean) {
            super.onMicDeviceEnabledChanged(enabled)
            micSwitch.isActivated = enabled
        }

        override fun onSpeakerEnabledChanged(enabled: Boolean) {
            super.onSpeakerEnabledChanged(enabled)
            speakerSwitch.isActivated = enabled
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

        val config = eduContext?.deviceContext()?.getDeviceConfig() ?: io.agora.edu.core.context.EduContextDeviceConfig()
        layout.findViewById<AppCompatTextView>(R.id.agora_setting_exit_button).setOnClickListener {
            exitClickHandler.run()
            dismiss()
        }

        cameraSwitch.isActivated = config.cameraEnabled ?: false
        cameraSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.setCameraDeviceEnable(!cameraSwitch.isActivated)
        }

        facingFront.isActivated = config.cameraFacing == io.agora.edu.core.context.EduContextCameraFacing.Front
        facingBack.isActivated = config.cameraFacing == io.agora.edu.core.context.EduContextCameraFacing.Back

        facingFront.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.switchCameraFacing()
        }

        facingBack.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.switchCameraFacing()
        }

        micSwitch.isActivated = config.micEnabled ?: false
        micSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.setMicDeviceEnable(!micSwitch.isActivated)
        }

        speakerSwitch.isActivated = config.speakerEnabled ?: false
        speakerSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.setSpeakerEnable(!speakerSwitch.isActivated)
        }

        eduContext?.deviceContext()?.addHandler(eventHandler)
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