package io.agora.uikit.impl.setting

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.forEach
import io.agora.educontext.EduContextCameraFacing
import io.agora.educontext.EduContextDeviceConfig
import io.agora.educontext.EduContextPool
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.DeViceHandler
import io.agora.uikit.util.AppUtil

class AgoraUIDeviceSettingPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraUIDeviceSettingDia"

    private val clickInterval = 500L

    private var parent: ViewGroup? = null
    private var contentWidth = 0
    private var contentHeight = 0
    private var rightMargin = 0
    private var bottomMargin = 0

    @SuppressLint("InflateParams")
    private val layout = LayoutInflater.from(context).inflate(
        R.layout.agora_status_bar_setting_dialog_layout, null, false)

    private val cameraSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_camera_switch)
    private val micSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_mic_switch)
    private val speakerSwitch: AppCompatImageView = layout.findViewById(R.id.agora_setting_loudspeaker_switch)
    private val facingFront: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_front)
    private val facingBack: AppCompatTextView = layout.findViewById(R.id.agora_setting_camera_back)

    private var eduContext: EduContextPool? = null

    var leaveRoomRunnable: Runnable? = null

    private val eventHandler = object : DeViceHandler() {
        override fun onCameraDeviceEnableChanged(enabled: Boolean) {
            super.onCameraDeviceEnableChanged(enabled)
            cameraSwitch.isActivated = enabled
        }

        override fun onCameraFacingChanged(facing: EduContextCameraFacing) {
            super.onCameraFacingChanged(facing)
            facingFront.isActivated = facing == EduContextCameraFacing.Front
            facingBack.isActivated = facing == EduContextCameraFacing.Back
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
        val content = layout.findViewById<LinearLayout>(R.id.agora_setting_dialog_layout)
        content.elevation = 10f
        content.clipToOutline = true

        val config = eduContext?.deviceContext()?.getDeviceConfig() ?: EduContextDeviceConfig()
        layout.findViewById<AppCompatTextView>(R.id.agora_setting_exit_button).setOnClickListener {
            dismiss()
            leaveRoomRunnable?.run()
        }

        cameraSwitch.isActivated = config.cameraEnabled
        cameraSwitch.setOnClickListener {
            if (AppUtil.isFastClick(clickInterval)) {
                return@setOnClickListener
            }
            eduContext?.deviceContext()?.setCameraDeviceEnable(!cameraSwitch.isActivated)
        }

        facingFront.isActivated = config.cameraFacing == EduContextCameraFacing.Front
        facingBack.isActivated = config.cameraFacing == EduContextCameraFacing.Back

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
    }

    fun initView(parent: ViewGroup, width: Int, height: Int, right: Int, bottom: Int) {
        this.parent = parent
        contentWidth = width
        contentHeight = height
        rightMargin = right
        bottomMargin = bottom
    }

    fun setEduContextPool(eduContext: EduContextPool?) {
        this.eduContext = eduContext
        resetDeviceStateButtons()
        eduContext?.deviceContext()?.addHandler(eventHandler)
    }

    private fun resetDeviceStateButtons() {
        val config = eduContext?.deviceContext()?.getDeviceConfig()
        config?.let { it ->
            cameraSwitch.isActivated = it.cameraEnabled
            facingFront.isActivated = it.cameraFacing == EduContextCameraFacing.Front
            facingBack.isActivated = it.cameraFacing == EduContextCameraFacing.Back
            micSwitch.isActivated = it.micEnabled
            speakerSwitch.isActivated = it.speakerEnabled
        }
    }

    fun show() {
        parent?.let { parent ->
            addView(layout, contentWidth, contentHeight)
            parent.addView(this, contentWidth, contentHeight)
            val param = this.layoutParams as MarginLayoutParams
            param.width = contentWidth
            param.height = contentHeight
            param.rightMargin = rightMargin
            param.bottomMargin = bottomMargin
            param.leftMargin = parent.width - rightMargin - contentWidth
            param.topMargin = parent.height - bottomMargin - contentHeight
            this.layoutParams = param
        }
    }

    fun dismiss() {
        this.parent?.let {
            var contains = false
            it.forEach { child ->
                if (child == this) contains = true
            }

            if (contains) {
                it.removeView(this)
            }
        }
    }
}