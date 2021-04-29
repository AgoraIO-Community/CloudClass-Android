package io.agora.uikit.impl.room

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.educontext.EduContextClassState
import io.agora.educontext.EduContextNetworkState
import io.agora.educontext.EduContextPool
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.component.dialog.AgoraUIDialogBuilder
import io.agora.uikit.educontext.handlers.RoomHandler

@SuppressLint("InflateParams")
class AgoraUIRoomStatus(parent: ViewGroup,
                        private val eduContext: EduContextPool?,
                        width: Int,
                        height: Int,
                        left: Int,
                        top: Int) : AbsComponent() {

    private val contentView: View = LayoutInflater.from(parent.context).inflate(
            R.layout.agora_status_bar_layout, parent, false)
    private val networkImage: AppCompatImageView
    private val className: AppCompatTextView
    private val classStateText: AppCompatTextView
    private val classTimeText: AppCompatTextView
    private val exitBtn: AppCompatImageView

    private val eventHandler = object : RoomHandler() {
        override fun onClassroomName(name: String) {
            setClassroomName(name)
        }

        override fun onClassState(state: EduContextClassState) {
            setClassState(state)
        }

        override fun onClassTime(time: String) {
            setClassTime(time)
        }

        override fun onNetworkStateChanged(state: EduContextNetworkState) {
            setNetworkState(state)
        }
    }

    init {
        parent.addView(contentView, width, height)
        val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top
        params.leftMargin = left
        contentView.layoutParams = params

        networkImage = contentView.findViewById(R.id.agora_status_bar_network_state_icon)
        className = contentView.findViewById(R.id.agora_status_bar_classroom_name)
        classStateText = contentView.findViewById(R.id.agora_status_bar_class_started_text)
        classTimeText = contentView.findViewById(R.id.agora_status_bar_class_time_text)
        exitBtn = contentView.findViewById(R.id.agora_status_bar_exit_icon)

        exitBtn.setOnClickListener {
            showLeaveDialog()
        }

        setNetworkState(EduContextNetworkState.Unknown)
        eduContext?.roomContext()?.addHandler(eventHandler)
    }

    fun showLeaveDialog() {
        className.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_message))
                    .negativeText(className.context.resources.getString(R.string.cancel))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { eduContext?.roomContext()?.leave() })
                    .build()
                    .show()
        }
    }

    private fun destroyClassDialog() {
        className.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_class_destroy_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_class_destroy))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { eduContext?.roomContext()?.leave() })
                    .build()
                    .show()
        }
    }

    fun kickOut() {
        className.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_kicked_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_kicked_message))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { eduContext?.roomContext()?.leave() })
                    .build()
                    .show()
        }
    }

    fun setClassroomName(name: String) {
        className.post { className.text = name }
    }

    fun setClassState(state: EduContextClassState) {
        classStateText.post {
            if (state == EduContextClassState.Destroyed) {
                destroyClassDialog()
                return@post
            }
            classStateText.setText(
                    when (state) {
                        EduContextClassState.Init -> R.string.agora_room_state_not_started
                        EduContextClassState.Start -> R.string.agora_room_state_started
                        EduContextClassState.End -> R.string.agora_room_state_end
                        else -> return@post
                    })
            if (state == EduContextClassState.End) {
                classStateText.setTextColor(classStateText.context.resources.getColor(R.color.agora_setting_leave_text_color))
                classTimeText.setTextColor(classStateText.context.resources.getColor(R.color.agora_setting_leave_text_color))
            }
        }
    }

    fun setClassTime(time: String) {
        classTimeText.post { classTimeText.text = time }
    }

    fun setNetworkState(state: EduContextNetworkState) {
        networkImage.post {
            networkImage.setImageResource(getNetworkStateIcon(state))
        }
    }

    private fun getNetworkStateIcon(state: EduContextNetworkState): Int {
        return when (state) {
            EduContextNetworkState.Good -> R.drawable.agora_tool_icon_signal_good
            EduContextNetworkState.Medium -> R.drawable.agora_tool_icon_signal_medium
            EduContextNetworkState.Bad -> R.drawable.agora_tool_icon_signal_bad
            EduContextNetworkState.Unknown -> R.drawable.agora_tool_icon_signal_unknown
        }
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }
}