package com.agora.edu.component

import android.app.Activity
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.databinding.AgoraEduHeadComponentBinding


/**
 * author : hefeng
 * date : 2022/1/24
 * description : 通用 header
 */
class AgoraEduHeadComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraEduHeadComponentBinding =
        AgoraEduHeadComponentBinding.inflate(LayoutInflater.from(context), this, true)

    private var curNetworkState = EduContextNetworkQuality.Unknown
    private var badCount = 0
    private val badNetworkThreshold = 3
    private val Class_Will_End_Warn_Time_Min = (5 * 60).toLong()
    private val Class_Will_End_Warn_Time_Max = (5 * 60 + 1).toLong()
    private val Class_Force_Leave_Warn_Time_Min = (1 * 60).toLong()
    private val Class_Force_Leave_Warn_Time_Max = (1 * 60 + 1).toLong()
    private var lastLogTime = 0L
    private var popupView: AgoraEduSettingPopupComponent? = null
    private var isPopUpShowing = false

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
                tips = R.string.fcr_room_class_end_warning
            } else if (a > -1 && a < 1) {
                minutes1 = classInfo.closeDelay / 60
                seconds1 = classInfo.closeDelay % 60
                tips = R.string.fcr_room_close_warning
            } else if (a <= -classInfo.closeDelay) {
                roomHandler.onRoomClosed()
            } else if (b in Class_Force_Leave_Warn_Time_Min until Class_Force_Leave_Warn_Time_Max) {
                minutes1 = 1
                tips = R.string.fcr_room_close_warning
            }
            if (isLogTime(startedTime)) {
                eduContext?.monitorContext()?.uploadLog()
            }
            if (tips != -1) {
                val countdownTips = buildCountdownTips(tips, minutes1, seconds1)
                AgoraUIToast.warn(context, text = countdownTips.toString())
            }
            handler?.postDelayed(this, 1000)
        }
    }

    private fun buildCountdownTips(tips: Int, minutes: Long, seconds: Long): SpannableString {
        var time: String? = null
        var content: SpannableString? = null
        if (tips == R.string.fcr_room_close_warning) {
            if (seconds != 0L) {
                time = String.format(
                    context.resources.getString(R.string.fcr_toast_classtime_until_class_close_0_args),
                    minutes, seconds
                )
                time = String.format(context.resources.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(
                    ForegroundColorSpan(context.resources.getColor(R.color.toast_classtime_countdown_time)),
                    minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val secondsStart = time.lastIndexOf(seconds.toString())
                content.setSpan(
                    ForegroundColorSpan(context.resources.getColor(R.color.toast_classtime_countdown_time)),
                    secondsStart, secondsStart + seconds.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                time = String.format(
                    context.resources.getString(R.string.fcr_toast_classtime_until_class_close_0_arg),
                    minutes
                )
                time = String.format(context.resources.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(
                    ForegroundColorSpan(context.resources.getColor(R.color.toast_classtime_countdown_time)),
                    minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            time = String.format(context.resources.getString(R.string.fcr_toast_classtime_until_class_close_0_arg), minutes)
            time = String.format(context.resources.getString(tips), time)
            content = SpannableString(time.toString())
            val start = time.indexOf(minutes.toString())
            content.setSpan(
                ForegroundColorSpan(context.resources.getColor(R.color.toast_classtime_countdown_time)),
                start, start + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.agoraStatusBarSettingIcon.setOnClickListener {
            if (!isPopUpShowing) {
                showDeviceSettingPopUp()
                isPopUpShowing = true
                binding.agoraStatusBarSettingIcon.isActivated = true
            } else {
                popupView?.dismiss()
                isPopUpShowing = false
                binding.agoraStatusBarSettingIcon.isActivated = false
            }
        }
        updateNetworkState(EduContextNetworkQuality.Unknown)
        eduContext?.roomContext()?.addHandler(roomHandler)
        eduContext?.monitorContext()?.addHandler(monitorHandler)
    }

//    fun setOnSettingClickListener(listener: OnClickListener) {
//        binding.agoraStatusBarSettingIcon.setOnClickListener(listener)
//    }

    /**
     * 设置标题在右边
     */
    fun setTitleToRight() {
        binding.agoraStatusBarSettingIcon.visibility = View.GONE
        val params: RelativeLayout.LayoutParams =
            binding.agoraStatusBarCenter.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_END)
        binding.agoraStatusBarCenter.layoutParams = params
    }

    private fun showDeviceSettingPopUp() {
        if (popupView == null) {
            popupView = AgoraEduSettingPopupComponent(context)
            popupView?.initView(agoraUIProvider)
        }

        popupView!!.popupContainerView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset: Int = -(popupView!!.popupContainerView?.getMeasuredWidth() ?: 0)
        // 右边对齐
        popupView!!.popupWindow?.showAsDropDown(
            binding.agoraStatusBarSettingIcon,
            xOffset,
            0,
            Gravity.BOTTOM or Gravity.RIGHT
        )

        // 左边顶部对齐
        //popupView!!.popupWindow?.showAsDropDown(btn, xOffset, -btn.height,Gravity.TOP or Gravity.LEFT)

        popupView?.agroSettingWidget?.leaveRoomRunnable = Runnable {
            // TODO 退出
            binding.agoraStatusBarSettingIcon.isActivated = false
        }
    }

    fun showLeaveDialog() {
        ContextCompat.getMainExecutor(context).execute {
            var activity: Activity = context as Activity
            if (!activity.isFinishing && !activity.isDestroyed) {
                AgoraUIDialogBuilder(activity)
                    .title(context.resources.getString(R.string.fcr_room_class_leave_class_title))
                    .message(context.resources.getString(R.string.fcr_room_exit_warning))
                    .negativeText(context.resources.getString(R.string.fcr_user_kick_out_cancel))
                    .positiveText(context.resources.getString(R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        eduContext?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                            override fun onSuccess(target: Unit?) {
                                activity.finish()
                            }

                            override fun onFailure(error: EduContextError?) {
                                error?.let {
                                    AgoraUIToast.error(context, text = error.msg)
                                }
                            }
                        })
                    }
                    .build()
                    .show()
            }
        }
    }


    fun setClassroomName(name: String) {
        binding.agoraStatusBarClassroomName.post {
            binding.agoraStatusBarClassroomName.text = name
        }
    }

    fun setClassState(state: AgoraEduContextClassState) {
        if (state == AgoraEduContextClassState.Before) {
        } else if (state == AgoraEduContextClassState.During) {
            positiveRunnable.run()
        } else if (state == AgoraEduContextClassState.After || state == AgoraEduContextClassState.RoomClosed) {
            positiveRunnable.run()
        }
        binding.agoraStatusBarClassStartedText.post {
            binding.agoraStatusBarClassStartedText.setText(
                when (state) {
                    AgoraEduContextClassState.Before -> R.string.fcr_room_class_time_away
                    AgoraEduContextClassState.During -> R.string.fcr_room_class_started
                    AgoraEduContextClassState.After -> R.string.fcr_room_class_over
                    else -> {
                        R.string.fcr_room_class_over
                    }
                }
            )
            if (state == AgoraEduContextClassState.After) {
                binding.agoraStatusBarClassStartedText.setTextColor(
                    binding.agoraStatusBarClassStartedText.context.resources.getColor(
                        R.color.theme_text_color_orange_red
                    )
                )
                binding.agoraStatusBarClassTimeText.setTextColor(
                    binding.agoraStatusBarClassStartedText.context.resources.getColor(
                        R.color.theme_text_color_orange_red
                    )
                )
            }
        }
    }

    private fun setClassTime(time: String) {
        binding.agoraStatusBarClassTimeText.post { binding.agoraStatusBarClassTimeText.text = time }
    }

    private fun buildTime(minutes: Long, seconds: Long): String {
        return if (minutes > 59) {
            val hours = minutes / 60
            val m = minutes % 60
            String.format(context.resources.getString(R.string.fcr_reward_window_classtime1), hours, m, seconds)
        } else {
            String.format(context.resources.getString(R.string.fcr_reward_window_classtime0), minutes, seconds)
        }
    }

    private fun updateNetworkState(state: EduContextNetworkQuality) {
        binding.agoraStatusBarNetworkStateIcon.post {
            binding.agoraStatusBarNetworkStateIcon.setImageResource(getNetworkStateIcon(state))
        }
        if (state == EduContextNetworkQuality.Bad && badCount == badNetworkThreshold) {
            AgoraUIToast.warn(
                context = context,
                text = context.resources.getString(R.string.fcr_monitor_network_poor)
            )
            badCount = 0
        } else if (state == EduContextNetworkQuality.Bad && badCount != badNetworkThreshold) {
            badCount++
        } else if (state != EduContextNetworkQuality.Bad) {
            badCount = 0
        }
        curNetworkState = state
    }


    private fun getNetworkStateIcon(state: EduContextNetworkQuality): Int {
        return when (state) {
            EduContextNetworkQuality.Good -> R.drawable.agora_tool_icon_signal_good
            EduContextNetworkQuality.Bad -> R.drawable.agora_tool_icon_signal_bad
            EduContextNetworkQuality.Down -> R.drawable.agora_tool_icon_signal_down
            else -> R.drawable.agora_tool_icon_signal_unknown
        }
    }
}