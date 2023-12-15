package io.agora.online.component

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
import androidx.core.view.isVisible
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.AgoraEduContextSubRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.online.R
import io.agora.online.component.common.AbsAgoraEduConfigComponent
import io.agora.online.component.common.UIUtils
import io.agora.online.component.toast.AgoraUIToast
import io.agora.online.config.component.FcrStateBarUIConfig
import io.agora.online.databinding.FcrOnlineEduHeadComponentBinding


/**
 * author : felix
 * date : 2022/1/24
 * description : 通用 header
 */
class AgoraEduHeadComponent : AbsAgoraEduConfigComponent<FcrStateBarUIConfig> {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineEduHeadComponentBinding =
        FcrOnlineEduHeadComponentBinding.inflate(LayoutInflater.from(context), this, true)

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
    var onExitListener: (() -> Unit)? = null // 退出

    private val positiveRunnable: Runnable = object : Runnable {
        override fun run() {
            //val classInfo = eduContext?.roomContext()?.getClassInfo() ?: AgoraEduContextClassInfo()
            val classInfo = getAgoraEduContextClassInfo()
            if (classInfo.startTime == 0L) {
                return
            }
            var startedTime = (TimeUtil.currentTimeMillis() - classInfo.startTime) / 1000
            if (startedTime < 0) {
                startedTime = -startedTime
            }
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
                AgoraUIToast.warn(context.applicationContext, text = countdownTips.toString())
            }
            handler?.postDelayed(this, 1000)
        }
    }

    fun getAgoraEduContextClassInfo(): AgoraEduContextClassInfo {
        var info = eduContext?.roomContext()?.getClassInfo()
        // 如果是分组小班课，用大教室的时间，分组没有时间
        if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
            FCRGroupClassUtils.mainLaunchConfig?.apply {
                val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
                info = eduContextPool?.roomContext()?.getClassInfo()
            }
        }
        return info ?: AgoraEduContextClassInfo()
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
            time = String.format(
                context.resources.getString(R.string.fcr_toast_classtime_until_class_close_0_arg),
                minutes
            )
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

    val groupHandler = object : FCRGroupHandler() {
        override fun onSubRoomListUpdated(subRoomList: List<AgoraEduContextSubRoomInfo>) {
            super.onSubRoomListUpdated(subRoomList)
            subRoomList.forEach {
                val roomUuid = eduContext?.roomContext()?.getRoomInfo()?.roomUuid
                if (roomUuid == it.subRoomUuid) {
                    // 当前分组名称更改了
                    setClassroomName(it.subRoomName)
                }
            }
        }
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            setClassroomName(roomInfo.roomName)

            val state = eduContext?.roomContext()?.getRecordingState()
            if (state == FcrRecordingState.STARTED) {
                setShowRecordView(1)
            } else {
                setShowRecordView(0)
            }
        }

        override fun onRecordingStateUpdated(state: FcrRecordingState) {
            super.onRecordingStateUpdated(state)
            if (state == FcrRecordingState.STARTED) {
                setShowRecordView(1)
            } else {
                setShowRecordView(0)
            }
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

    var isEnableRecordView = true

    fun setShowRecordView(state: Int) {
        if (isEnableRecordView) {
            ContextCompat.getMainExecutor(context).execute {
                if (state == 1) {
                    binding.agoraRecordStatus.visibility = View.VISIBLE
                } else {
                    binding.agoraRecordStatus.visibility = View.GONE
                }
            }
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
        //eduContext?.groupContext()?.addHandler(groupHandler)

        if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
            FCRGroupClassUtils.mainLaunchConfig?.apply {
                val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
                eduContextPool?.groupContext()?.addHandler(groupHandler)
                eduContextPool?.roomContext()?.addHandler(roomHandler)
                AgoraEduCoreManager.getEduCore(roomUuid)?.updateClassState()
            }
        }
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

    fun setTitleToMiddle() {
        binding.agoraStatusBarSettingIcon.visibility = View.GONE
        (binding.agoraStatusBarCenter.layoutParams as? RelativeLayout.LayoutParams)?.let { params ->
            params.removeRule(RelativeLayout.ALIGN_PARENT_END)
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            binding.agoraStatusBarCenter.layoutParams = params
        }
    }

    fun showSettingIcon(showed: Boolean) {
        binding.agoraStatusBarSettingIcon.isVisible = showed
    }

    fun showStandaloneExit(showed: Boolean, listener: OnClickListener? = null) {
        binding.agoraStatusBarExitIcon.isVisible = showed
        binding.agoraStatusBarExitIcon.setOnClickListener(listener)
    }

    private fun showDeviceSettingPopUp() {
        if (popupView == null) {
            popupView = AgoraEduSettingPopupComponent(context)
            popupView?.initView(agoraUIProvider)
            popupView?.onExitListener = {
                onExitListener?.invoke()
            }
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
            binding.agoraStatusBarSettingIcon.isActivated = false
        }
    }

    fun setClassroomName(name: String) {
        binding.agoraStatusBarClassroomName.post {
            binding.agoraStatusBarClassroomName.text = name
        }
    }

    fun setClassState(state: AgoraEduContextClassState) {
        if (getUIConfig().scheduleTime?.isVisible != true) {
            return
        }
        if (state == AgoraEduContextClassState.Before) {
            positiveRunnable.run()
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
            val classInfo = getAgoraEduContextClassInfo()
            if (classInfo.startTime == 0L) {
                binding.agoraStatusBarClassStartedText.text = ""
            }

            if (state == AgoraEduContextClassState.After) {
                binding.agoraStatusBarClassStartedText.setTextColor(
                    binding.agoraStatusBarClassStartedText.context.resources.getColor(
                        R.color.fcr_system_error_color
                    )
                )
                binding.agoraStatusBarClassTimeText.setTextColor(
                    binding.agoraStatusBarClassStartedText.context.resources.getColor(
                        R.color.fcr_system_error_color
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
                context.applicationContext,
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

    override fun release() {
        super.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
        eduContext?.monitorContext()?.removeHandler(monitorHandler)

        if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
            FCRGroupClassUtils.mainLaunchConfig?.apply {
                val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
                eduContextPool?.groupContext()?.removeHandler(groupHandler)
                eduContextPool?.roomContext()?.removeHandler(roomHandler)
            }
        }
    }

    override fun updateUIForConfig(config: FcrStateBarUIConfig) {
        UIUtils.setViewVisible(binding.agoraStatusBarNetworkStateIcon, config.networkState.isVisible)
        UIUtils.setViewVisible(binding.agoraStatusBarClassroomName, config.roomName.isVisible)
        UIUtils.setViewVisible(binding.agoraStatusBarClassStartedText, config.scheduleTime.isVisible)
        UIUtils.setViewVisible(binding.agoraStatusBarClassTimeText, config.scheduleTime.isVisible)
    }

    override fun getUIConfig(): FcrStateBarUIConfig {
        return getTemplateUIConfig().stateBar
    }
}