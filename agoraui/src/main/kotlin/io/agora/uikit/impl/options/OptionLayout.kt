package io.agora.uikit.impl.options

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import io.agora.educontext.EduContextPool
import io.agora.uikit.R
import io.agora.uikit.component.dialog.AgoraUIDialogBuilder
import io.agora.uikit.educontext.handlers.RoomHandler

@SuppressLint("InflateParams")
class OptionLayout(private val parent: ViewGroup,
                   private val right: Int,
                   private val bottom: Int,
                   private val windowListener: OptionWindowListener?,
                   private val eduContext: EduContextPool?) : View.OnClickListener {

    private val tag = "OptionLayout"
    private val layout: View = LayoutInflater.from(parent.context)
        .inflate(R.layout.agora_options_layout, parent, false)
    private val setting: AppCompatImageView
    private val upload: AppCompatImageView
    private val roster: AppCompatImageView
    private val chat: AppCompatImageView
    private val handsUp: AppCompatImageView

    private lateinit var handsUpWrapper: AgoraUIHandsUpWrapper

    private val logUploadHandler = object : RoomHandler() {
        override fun onLogUploaded(logData: String) {
            setUploadLogDialog(logData)
        }
    }

    fun setUploadLogDialog(logData: String) {
        upload.post {
            AgoraUIDialogBuilder(upload.context)
                .title(upload.context.resources.getString(R.string.agora_dialog_sent_log_success))
                .message(logData)
                .positiveText(upload.context.resources.getString(R.string.confirm))
                .build()
                .show()
        }
    }

    init {
        setting = layout.findViewById(R.id.options_setting)
        upload = layout.findViewById(R.id.options_upload)
        roster = layout.findViewById(R.id.options_roster)
        chat = layout.findViewById(R.id.options_chat)
        handsUp = layout.findViewById(R.id.options_handsup)

        setting.setOnClickListener(this)
        upload.setOnClickListener(this)
        roster.setOnClickListener(this)
        chat.setOnClickListener(this)

        parent.addView(layout)

        layout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (layout.width > 0 && layout.height > 0) {
                    layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val param = layout.layoutParams as ViewGroup.MarginLayoutParams
                    param.bottomMargin = bottom
                    param.rightMargin = right
                    param.leftMargin = parent.width - right - layout.width
                    param.topMargin = parent.height - bottom - layout.height
                    layout.layoutParams = param

                    val timerW = (handsUp.width * 2 / 3f).toInt()
                    val timerH = (timerW * 23 / 21f).toInt()
                    val timerRight = right + (handsUp.width - timerW) / 2
                    handsUpWrapper = AgoraUIHandsUpWrapper(parent, eduContext,
                        handsUp, timerW, timerH, timerRight, bottom + handsUp.height)
                }
            }
        })

        eduContext?.roomContext()?.addHandler(logUploadHandler)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            setting.id -> {
                setActivated(OptionItem.Roster, false)
                setActivated(OptionItem.Chat, false)
                windowListener?.onWindowDismiss(OptionItem.Roster)
                windowListener?.onWindowDismiss(OptionItem.Chat)
                if (isActivated(OptionItem.Setting)) {
                    windowListener?.onWindowDismiss(OptionItem.Setting)
                    setActivated(OptionItem.Setting, false)
                } else {
                    windowListener?.onWindowShow(OptionItem.Setting)
                    setActivated(OptionItem.Setting)
                }
            }
            roster.id -> {
                setActivated(OptionItem.Setting, false)
                setActivated(OptionItem.Chat, false)
                windowListener?.onWindowDismiss(OptionItem.Setting)
                windowListener?.onWindowDismiss(OptionItem.Chat)
                if (isActivated(OptionItem.Roster)) {
                    windowListener?.onWindowDismiss(OptionItem.Roster)
                    setActivated(OptionItem.Roster, false)
                } else {
                    windowListener?.onWindowShow(OptionItem.Roster)
                    setActivated(OptionItem.Roster)
                }
            }
            chat.id -> {
                setActivated(OptionItem.Setting, false)
                setActivated(OptionItem.Roster, false)
                windowListener?.onWindowDismiss(OptionItem.Setting)
                windowListener?.onWindowDismiss(OptionItem.Roster)
                if (isActivated(OptionItem.Chat)) {
                    windowListener?.onWindowDismiss(OptionItem.Chat)
                    setActivated(OptionItem.Chat, false)
                } else {
                    windowListener?.onWindowShow(OptionItem.Chat)
                    setActivated(OptionItem.Chat)
                }
            }
            upload.id -> {
                eduContext?.roomContext()?.uploadLog()
            }
        }
    }

    fun setActivated(item: OptionItem, isActivated: Boolean = true) {
        when (item) {
            OptionItem.Setting -> {
                setting.isActivated = isActivated
            }
            OptionItem.Roster -> {
                roster.isActivated = isActivated
            }
            OptionItem.Chat -> {
                chat.isActivated = isActivated
            }
            else -> {

            }
        }
    }

    private fun isActivated(item: OptionItem): Boolean {
        return when (item) {
            OptionItem.Setting -> setting.isActivated
            OptionItem.Roster -> roster.isActivated
            OptionItem.Chat -> chat.isActivated
            else -> false
        }
    }

    fun setIconSize(size: Int) {
        setIconSizeView(setting, size)
        setIconSizeView(upload, size)
        setIconSizeView(roster, size)
        setIconSizeView(chat, size)
        setIconSizeView(handsUp, size)
    }

    private fun setIconSizeView(view: View, size: Int) {
        val param: LinearLayout.LayoutParams =
            view.layoutParams as LinearLayout.LayoutParams
        param.width = size
        param.height = size
        setting.layoutParams = param
    }
}

interface OptionWindowListener {
    fun onWindowShow(item: OptionItem)

    fun onWindowDismiss(item: OptionItem)
}

enum class OptionItem {
    Setting, Upload, Roster, Chat, HandsUp
}