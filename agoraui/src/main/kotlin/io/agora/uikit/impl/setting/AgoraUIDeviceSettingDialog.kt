package io.agora.uikit.impl.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.uikit.R
import io.agora.uikit.impl.container.AbsUIContainer
import io.agora.uikit.interfaces.listeners.IAgoraUIDeviceListener

@SuppressLint("InflateParams")
class AgoraUIDeviceSettingDialog(context: Context, anchor: View,
                                 private val dismissListener: DialogInterface.OnDismissListener,
                                 exitClickHandler: Runnable,
                                 container: AbsUIContainer?,
                                 private val deviceListener: IAgoraUIDeviceListener?) : Dialog(context, R.style.agora_dialog) {
    private val facingFront: AppCompatTextView
    private val facingBack: AppCompatTextView

    init {
        val layout = LayoutInflater.from(context).inflate(
                R.layout.agora_status_bar_setting_dialog_layout, null, false)
        setContentView(layout)
        val content = layout.findViewById<LinearLayout>(R.id.agora_setting_dialog_layout)
        content.elevation = 10f
        content.clipToOutline = true

        hideStatusBar(window!!)
        adjustPosition(anchor,
            content.resources.getDimensionPixelSize(R.dimen.agora_setting_dialog_width),
            content.resources.getDimensionPixelSize(R.dimen.agora_setting_dialog_height))

        val config = container?.deviceConfig
        layout.findViewById<AppCompatTextView>(R.id.agora_setting_exit_button).setOnClickListener {
            exitClickHandler.run()
        }

        val cameraSwitch = layout.findViewById<AppCompatImageView>(R.id.agora_setting_camera_switch)
        cameraSwitch.isActivated = config?.cameraEnabled ?: false
        cameraSwitch.setOnClickListener {
            it.isActivated = !it.isActivated
            deviceListener?.onCameraEnabled(it.isActivated)
        }

        facingFront = layout.findViewById(R.id.agora_setting_camera_front)
        facingBack = layout.findViewById(R.id.agora_setting_camera_back)
        facingFront.isActivated = config?.cameraFront ?: false
        facingBack.isActivated = !(config?.cameraFront ?: true)

        facingFront.setOnClickListener {
            if (config?.cameraFront == false) {
                config.cameraFront = true
                deviceListener?.onCameraFacingChanged(config.cameraFront)
            }

            facingFront.isActivated = config?.cameraFront ?: false
            facingBack.isActivated = !(config?.cameraFront ?: true)
        }

        facingBack.setOnClickListener {
            if (config?.cameraFront == true) {
                config.cameraFront = false
                deviceListener?.onCameraFacingChanged(config.cameraFront)
            }

            facingFront.isActivated = config?.cameraFront ?: false
            facingBack.isActivated = !(config?.cameraFront ?: true)
        }

        val micSwitch = layout.findViewById<AppCompatImageView>(R.id.agora_setting_mic_switch)
        micSwitch.isActivated = config?.micEnabled ?: false
        micSwitch.setOnClickListener {
            it.isActivated = !it.isActivated
            deviceListener?.onMicEnabled(it.isActivated)
        }

        val speakerSwitch = layout.findViewById<AppCompatImageView>(R.id.agora_setting_loudspeaker_switch)
        speakerSwitch.isActivated = config?.speakerEnabled ?: false
        speakerSwitch.setOnClickListener {
            it.isActivated = !it.isActivated
            deviceListener?.onSpeakerEnabled(it.isActivated)
        }
    }

    override fun show() {
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setOnDismissListener(dismissListener)

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
        params.x = 10
        params.y = locationsOnScreen[1] + anchor.height
        window.attributes = params
    }
}