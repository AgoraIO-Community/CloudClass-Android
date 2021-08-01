package io.agora.edu.classroom

import android.content.Context
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.R
import io.agora.edu.common.api.FlexProps
import io.agora.edu.common.bean.flexpropes.RoomFlexPropsReq
import io.agora.edu.common.bean.response.RoomPreCheckRes
import io.agora.edu.common.impl.FlexPropsImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.util.TimeUtil
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.statistics.NetworkQuality
import io.agora.educontext.EduContextClassState
import io.agora.educontext.EduContextConnectionState
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextNetworkState
import io.agora.educontext.context.RoomContext

class RoomStateManager(
        context: Context,
        private val roomContext: RoomContext?,
        var launchConfig: AgoraEduLaunchConfig,
        var preCheckData: RoomPreCheckRes,
        var eduRoom: EduRoom?) {

    private val tag = "RoomStatusManager"
    private var contextApp = context.applicationContext
    private var handler: Handler? = Handler(contextApp.mainLooper)
    private var curState = EduRoomState.INIT
    private var startTime: Long = 0L
    private var duration: Long = 0L

    private val reverseRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                var minutes = (startTime - TimeUtil.currentTimeMillis()) / 1000 / 60
                var seconds = (startTime - TimeUtil.currentTimeMillis()) / 1000 % 60
                minutes = if (minutes < 0) 0 else minutes
                seconds = if (seconds < 0) 0 else seconds
                val timeStr = buildTime(minutes, seconds)
                if (TimeUtil.currentTimeMillis() > startTime) {
                    handler!!.removeCallbacks(this)
                } else {
                    handler!!.postDelayed(this, 1000)
                }

                roomContext?.getHandlers()?.forEach { handler ->
                    handler.onClassTime(timeStr)
                }
            }
        }
    }

    private val positiveRunnable: Runnable = object : Runnable {
        override fun run() {
            val startedTime = (TimeUtil.currentTimeMillis() - startTime) / 1000
            val minutes0 = startedTime / 60
            val seconds0 = startedTime % 60
            val timeStr = buildTime(minutes0, seconds0)

            roomContext?.getHandlers()?.forEach { handler ->
                handler.onClassTime(timeStr)
            }

            val a = duration - startedTime
            val b = closeDelay + duration - startedTime
            var tips = -1
            var minutes1: Long = 0
            var seconds1: Long = 0

            if (a in Class_Will_End_Warn_Time_Min until Class_Will_End_Warn_Time_Max) {
                minutes1 = 5
                tips = R.string.toast_classtime_until_class_end
            } else if (a > -1 && a < 1) {
                minutes1 = closeDelay / 60
                seconds1 = closeDelay % 60
                tips = R.string.toast_classtime_until_class_close_0
//            } else if (a > -closeDelay - 1 && a <= -closeDelay) {
            } else if (a <= -closeDelay) {
                roomContext?.getHandlers()?.forEach { handler ->
                    handler.onClassState(EduContextClassState.Destroyed)
                }
            } else if (b in Class_Force_Leave_Warn_Time_Min until Class_Force_Leave_Warn_Time_Max) {
                minutes1 = 1
                tips = R.string.toast_classtime_until_class_close_1
            }
            if (tips != -1) {
                val countdownTips = buildCountdownTips(tips, minutes1, seconds1)
                roomContext?.getHandlers()?.forEach { handler ->
                    handler.onClassTip(countdownTips.toString())
                }
            }
            handler!!.postDelayed(this, 1000)
        }
    }

    private val Class_Will_End_Warn_Time_Min = (5 * 60).toLong()
    private val Class_Will_End_Warn_Time_Max = (5 * 60 + 1).toLong()
    private var closeDelay: Long = 0L
    private val Class_Force_Leave_Warn_Time_Min = (1 * 60).toLong()
    private val Class_Force_Leave_Warn_Time_Max = (1 * 60 + 1).toLong()

    private var curNetworkState = EduContextNetworkState.Unknown
    private var curConnectionState = EduContextConnectionState.Disconnected

    private val flexProps: FlexProps

    private val scheduleKey = "schedule"
    private val startTimeKey = "startTime"

    init {
        flexProps = FlexPropsImpl(launchConfig.appId, launchConfig.roomUuid)
    }

    fun dispose() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        eduRoom = null
    }

    fun initClassState() {
        eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
            override fun onSuccess(res: EduRoomStatus?) {
                res?.let {
                    if (it.courseState != EduRoomState.INIT) {
                        refreshStartTime()?.let {
                            launchConfig.startTime = it
                        }
                    }
                    setClassState(
                            it.courseState,
                            preCheckData.startTime,
                            preCheckData.duration,
                            preCheckData.closeDelay)
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun buildTime(minutes: Long, seconds: Long): String {
        if (minutes > 59) {
            val hours = minutes / 60
            val m = minutes % 60
            val timeStr = String.format(contextApp.getString(R.string.reward_window_classtime1), hours, m, seconds)
            return timeStr
        } else {
            val timeStr = String.format(contextApp.getString(R.string.reward_window_classtime0), minutes, seconds)
            return timeStr
        }
    }

    fun setClassName(name: String) {
        roomContext?.getHandlers()?.forEach { handler ->
            handler.onClassroomName(name)
        }
    }

    private fun getProperty(properties: Map<String, Any>?, key: String): String? {
        if (properties != null) {
            for ((key1, value) in properties) {
                if (key1 == key) {
                    return Gson().toJson(value)
                }
            }
        }
        return null
    }

    private fun refreshStartTime(): Long? {
        val scheduleJson = getProperty(eduRoom!!.roomProperties, scheduleKey)
        val scheduleMap: MutableMap<String, Any>? = Gson().fromJson(scheduleJson,
                object : TypeToken<MutableMap<String, Any>>() {}.type)
        return getProperty(scheduleMap, startTimeKey)?.toDouble()?.toLong()
    }

    fun updateClassState(event: EduRoomChangeType) {
        if (event == EduRoomChangeType.CourseState) {
            eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
                override fun onSuccess(res: EduRoomStatus?) {
                    res?.let {
                        if (res.courseState == EduRoomState.START) {
                            refreshStartTime()?.let {
                                launchConfig.startTime = it
                            }
                            setClassState(EduRoomState.START,
                                    preCheckData.startTime,
                                    preCheckData.duration,
                                    preCheckData.closeDelay)
                        } else if (res.courseState == EduRoomState.END) {
                            setClassEnd()
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        }
    }

    fun setClassState(state: EduRoomState, startTime: Long, duration: Long, closeDelay: Long) {
        this.curState = state
        this.startTime = startTime
        this.duration = duration
        this.closeDelay = closeDelay

        if (this.curState == EduRoomState.INIT) {
            roomContext?.getHandlers()?.forEach { handler ->
                handler.onClassState(EduContextClassState.Init)
            }
            reverseRunnable.run()
        } else if (this.curState == EduRoomState.START) {
            handler!!.removeCallbacks(reverseRunnable)
            roomContext?.getHandlers()?.forEach { handler ->
                handler.onClassState(EduContextClassState.Start)
            }
            positiveRunnable.run()
        } else if (this.curState == EduRoomState.END) {
            handler!!.removeCallbacks(reverseRunnable)
            roomContext?.getHandlers()?.forEach { handler ->
                handler.onClassState(EduContextClassState.End)
            }
            positiveRunnable.run()
        }
    }

    private fun setClassEnd() {
        roomContext?.getHandlers()?.forEach { handler ->
            handler.onClassState(EduContextClassState.End)
        }
    }

    fun setUploadedLogMsg(logData: String) {
        roomContext?.getHandlers()?.forEach { handler ->
            handler.onLogUploaded(logData)
        }
    }

    private fun buildCountdownTips(tips: Int, minutes: Long, seconds: Long): SpannableString {
        var time: String? = null
        var content: SpannableString? = null
        if (tips == R.string.toast_classtime_until_class_close_0) {
            if (seconds != 0L) {
                time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_args),
                        minutes, seconds)
                time = String.format(contextApp.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val secondsStart = time.lastIndexOf(seconds.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        secondsStart, secondsStart + seconds.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_arg),
                        minutes)
                time = String.format(contextApp.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_arg), minutes)
            time = String.format(contextApp.getString(tips), time)
            content = SpannableString(time.toString())
            val start = time.indexOf(minutes.toString())
            content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                    start, start + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return content
    }

    fun updateNetworkState(quality: NetworkQuality) {
        var state = EduContextNetworkState.Unknown
        when (quality) {
            NetworkQuality.UNKNOWN -> state = EduContextNetworkState.Unknown
            NetworkQuality.GOOD -> state = EduContextNetworkState.Good
            NetworkQuality.POOR -> state = EduContextNetworkState.Medium
            NetworkQuality.BAD -> {
                state = EduContextNetworkState.Bad
                if (curNetworkState != EduContextNetworkState.Bad) {
                    roomContext?.getHandlers()?.forEach { handler ->
                        handler.onClassTip(contextApp.getString(R.string.toast_classroom_network_bad))
                    }
                }
            }
        }

        curNetworkState = state
        roomContext?.getHandlers()?.forEach { handler ->
            handler.onNetworkStateChanged(curNetworkState)
        }
    }

    fun updateConnectionState(connectionState: EduContextConnectionState) {
        if (connectionState == EduContextConnectionState.Aborted) {
            roomContext?.getHandlers()?.forEach { handler ->
                handler.onClassTip(contextApp.getString(R.string.remoteloginerror))
            }
        }

        roomContext?.getHandlers()?.forEach { handler ->
            handler.onConnectionStateChanged(connectionState)
        }
    }

    fun isReconnected(connectionState: EduContextConnectionState): Boolean {
        val reconnected = curConnectionState == EduContextConnectionState.Reconnecting &&
                connectionState == EduContextConnectionState.Connected
        curConnectionState = connectionState
        return reconnected
    }

    fun updateFlexProps(properties: MutableMap<String, String>, cause: MutableMap<String, String>?) {
        val req = RoomFlexPropsReq(properties, cause)
        flexProps.updateFlexRoomProperties(req, object : EduCallback<Boolean> {
            override fun onSuccess(res: Boolean?) {
            }

            override fun onFailure(error: EduError) {
                roomContext?.getHandlers()?.forEach {
                    it.onError(EduContextError(error.type, error.msg))
                }
            }
        })
    }
}