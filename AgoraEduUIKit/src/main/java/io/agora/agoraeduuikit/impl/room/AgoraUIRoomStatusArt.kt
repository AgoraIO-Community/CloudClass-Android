package io.agora.agoraeduuikit.impl.room

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.EduContextNetworkQuality.Bad
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialog
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.AbsComponent

@SuppressLint("InflateParams")
class AgoraUIRoomStatusArt(parent: ViewGroup,
                           private val eduContext: EduContextPool?,
                           width: Int,
                           height: Int,
                           left: Int,
                           top: Int) : AbsComponent() {
    private val tag = "AgoraUIRoomStatusArt"

    private val contentView: View = LayoutInflater.from(parent.context).inflate(
            R.layout.agora_status_bar_layout_art, parent, false)
    private val networkImage: AppCompatImageView
    private val className: AppCompatTextView
    private val classStateText: AppCompatTextView
    private val classTimeText: AppCompatTextView
    private var destroyClassDialog: AgoraUIDialog? = null
    private var curNetworkState = EduContextNetworkQuality.Unknown
    private var badCount = 0
    private val badNetworkThreshold = 3

    //    private var classInfo: AgoraEduContextClassInfo? = null
    private val Class_Will_End_Warn_Time_Min = (5 * 60).toLong()
    private val Class_Will_End_Warn_Time_Max = (5 * 60 + 1).toLong()
    private val Class_Force_Leave_Warn_Time_Min = (1 * 60).toLong()
    private val Class_Force_Leave_Warn_Time_Max = (1 * 60 + 1).toLong()
    private var lastLogTime = 0L
    private var handler: Handler? = Handler(parent.context.mainLooper)
    private val positiveRunnable: Runnable = object : Runnable {
        override fun run() {
            val classInfo = eduContext?.roomContext()?.getClassInfo() ?: AgoraEduContextClassInfo()
            val startedTime = (TimeUtil.currentTimeMillis() - classInfo.startTime) / 1000
            val minutes0 = startedTime / 60
            val seconds0 = startedTime % 60
            val timeStr = buildTime(minutes0, seconds0)
            setClassTime(timeStr)
            val a = classInfo.duration - startedTime
            val b = classInfo.closeDelay + classInfo.duration - startedTime
            var tips = -1
            var minutes1: Long = 0
            var seconds1: Long = 0

            if (a in Class_Will_End_Warn_Time_Min until Class_Will_End_Warn_Time_Max) {
                minutes1 = 5
                tips = R.string.toast_classtime_until_class_end
            } else if (a > -1 && a < 1) {
                minutes1 = classInfo.closeDelay / 60
                seconds1 = classInfo.closeDelay % 60
                tips = R.string.toast_classtime_until_class_close_0
            } else if (a <= -classInfo.closeDelay) {
                roomHandler.onRoomClosed()
            } else if (b in Class_Force_Leave_Warn_Time_Min until Class_Force_Leave_Warn_Time_Max) {
                minutes1 = 1
                tips = R.string.toast_classtime_until_class_close_1
            }
            if (isLogTime(startedTime)) {
                eduContext?.monitorContext()?.uploadLog()
            }
            if (tips != -1) {
                val countdownTips = buildCountdownTips(tips, minutes1, seconds1)
                AgoraUIToast.warn(context = contentView.context, text = countdownTips.toString())
            }
            handler!!.postDelayed(this, 1000)
        }
    }

    private fun buildCountdownTips(tips: Int, minutes: Long, seconds: Long): SpannableString {
        var time: String? = null
        var content: SpannableString? = null
        if (tips == R.string.toast_classtime_until_class_close_0) {
            if (seconds != 0L) {
                time = String.format(contentView.context.resources.getString(R.string.toast_classtime_until_class_close_0_args),
                        minutes, seconds)
                time = String.format(contentView.context.resources.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contentView.context.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val secondsStart = time.lastIndexOf(seconds.toString())
                content.setSpan(ForegroundColorSpan(contentView.context.resources.getColor(R.color.toast_classtime_countdown_time)),
                        secondsStart, secondsStart + seconds.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                time = String.format(contentView.context.resources.getString(R.string.toast_classtime_until_class_close_0_arg),
                        minutes)
                time = String.format(contentView.context.resources.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contentView.context.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            time = String.format(contentView.context.resources.getString(R.string.toast_classtime_until_class_close_0_arg), minutes)
            time = String.format(contentView.context.resources.getString(tips), time)
            content = SpannableString(time.toString())
            val start = time.indexOf(minutes.toString())
            content.setSpan(ForegroundColorSpan(contentView.context.resources.getColor(R.color.toast_classtime_countdown_time)),
                    start, start + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return content
    }

    private fun isLogTime(startedTime: Long): Boolean {
        // uploadLog every five minutes
        val item = 60L * 10
        val b = (startedTime - lastLogTime) >= item
        if (b) {
            lastLogTime = startedTime
            return true
        }
        return false
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            setClassroomName(roomInfo.roomName)
        }

        override fun onRoomClosed() {
            super.onRoomClosed()
            destroyClassDialog()
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            setClassState(state)
        }
    }

    private val monitorHandler = object : MonitorHandler() {
        override fun onLocalNetworkQualityUpdated(quality: EduContextNetworkQuality) {
            super.onLocalNetworkQualityUpdated(quality)
            updateNetworkState(quality)
        }

        override fun onLocalConnectionUpdated(state: EduContextConnectionState) {
            super.onLocalConnectionUpdated(state)
            updateConnectionState(state)
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

        updateNetworkState(EduContextNetworkQuality.Unknown)
        eduContext?.roomContext()?.addHandler(roomHandler)
        eduContext?.monitorContext()?.addHandler(monitorHandler)
    }

    fun showLeaveDialog() {
        ContextCompat.getMainExecutor(contentView.context).execute {
            getContainer()?.getActivity()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    AgoraUIDialogBuilder(className.context)
                            .title(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_title))
                            .message(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_message))
                            .negativeText(className.context.resources.getString(R.string.cancel))
                            .positiveText(className.context.resources.getString(R.string.confirm))
                            .positiveClick {
                                eduContext?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                                    override fun onSuccess(target: Unit?) {
                                        finish(activity)
                                    }

                                    override fun onFailure(error: EduContextError?) {
                                        error?.let {
                                            getContainer()?.showError(it)
                                        }
                                    }
                                })
                            }
                            .build()
                            .show()
                }
            }
        }
    }

    private fun finish(activity: Activity) {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        activity.finish()
    }

    private fun destroyClassDialog() {
        if (destroyClassDialog != null && destroyClassDialog!!.isShowing) {
            return
        }
        ContextCompat.getMainExecutor(contentView.context).execute {
            getContainer()?.getActivity()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    destroyClassDialog = AgoraUIDialogBuilder(activity)
                            .title(className.context.resources.getString(R.string.agora_dialog_class_destroy_title))
                            .message(className.context.resources.getString(R.string.agora_dialog_class_destroy))
                            .positiveText(className.context.resources.getString(R.string.confirm))
                            .positiveClick {
                                finish(activity)
                            }
                            .build()
                    destroyClassDialog?.show()
                    eduContext?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                        override fun onSuccess(target: Unit?) {
                        }

                        override fun onFailure(error: EduContextError?) {
                            error?.let {
                                getContainer()?.showError(it)
                            }
                        }
                    })
                }
            }
        }
    }

    fun kickOut() {
        ContextCompat.getMainExecutor(contentView.context).execute {
            getContainer()?.getActivity()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    AgoraUIDialogBuilder(className.context)
                            .title(className.context.resources.getString(R.string.agora_dialog_kicked_title))
                            .message(className.context.resources.getString(R.string.agora_dialog_kicked_message))
                            .positiveText(className.context.resources.getString(R.string.confirm))
                            .positiveClick {
                                activity.finish()
                            }
                            .build()
                            .show()
                    eduContext?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                        override fun onSuccess(target: Unit?) {
                        }

                        override fun onFailure(error: EduContextError?) {
                            error?.let {
                                getContainer()?.showError(it)
                            }
                        }
                    })
                }
            }
        }
    }

    fun setClassroomName(name: String) {
        className.post { className.text = name }
    }

    fun setClassState(state: AgoraEduContextClassState) {
        if (state == AgoraEduContextClassState.Before) {
        } else if (state == AgoraEduContextClassState.During) {
            positiveRunnable.run()
        } else if (state == AgoraEduContextClassState.After || state == AgoraEduContextClassState.RoomClosed) {
            positiveRunnable.run()
        }
        classStateText.post {
            classStateText.setText(
                    when (state) {
                        AgoraEduContextClassState.Before -> R.string.agora_room_state_not_started
                        AgoraEduContextClassState.During -> R.string.agora_room_state_started
                        AgoraEduContextClassState.After -> R.string.agora_room_state_end
                        else -> {
                            R.string.agora_room_state_end
                        }
                    })
            if (state == AgoraEduContextClassState.After) {
                classStateText.setTextColor(classStateText.context.resources.getColor(R.color.theme_text_color_orange_red))
                classTimeText.setTextColor(classStateText.context.resources.getColor(R.color.theme_text_color_orange_red))
            }
        }
    }

    private fun setClassTime(time: String) {
        classTimeText.post { classTimeText.text = time }
    }

    private fun buildTime(minutes: Long, seconds: Long): String {
        return if (minutes > 59) {
            val hours = minutes / 60
            val m = minutes % 60
            String.format(contentView.context.resources.getString(R.string.reward_window_classtime1),
                    hours, m, seconds)
        } else {
            String.format(contentView.context.resources.getString(R.string.reward_window_classtime0),
                    minutes, seconds)
        }
    }

    private fun updateNetworkState(state: EduContextNetworkQuality) {
        networkImage.post {
            networkImage.setImageResource(getNetworkStateIcon(state))
        }
        if (state == Bad && badCount == badNetworkThreshold) {
            AgoraUIToast.warn(context = contentView.context, text = contentView.context.resources
                .getString(R.string.toast_classroom_network_bad))
            badCount = 0
        } else if (state == Bad && badCount != badNetworkThreshold) {
            badCount++
        } else if (state != Bad) {
            badCount = 0
        }
        curNetworkState = state
    }

    private fun updateConnectionState(connectionState: EduContextConnectionState) {
        if (connectionState == EduContextConnectionState.Aborted) {
            AgoraUIToast.warn(context = contentView.context, text = contentView.context.resources
                .getString(R.string.remoteloginerror))
            eduContext?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                override fun onSuccess(target: Unit?) {
                    getContainer()?.getActivity()?.let { activity ->
                        finish(activity)
                    }
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
        }
    }

    private fun getNetworkStateIcon(state: EduContextNetworkQuality): Int {
        return when (state) {
            EduContextNetworkQuality.Good -> R.drawable.agora_tool_icon_signal_good
            Bad -> R.drawable.agora_tool_icon_signal_bad
            EduContextNetworkQuality.Down -> R.drawable.agora_tool_icon_signal_down
            else -> R.drawable.agora_tool_icon_signal_unknown
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