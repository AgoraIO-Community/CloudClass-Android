package com.hyphenate.easeimgroup.modules.utils

import android.annotation.SuppressLint
import android.content.Context
import io.agora.util.TimeInfo
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object EaseDateUtils {
    private const val INTERVAL_IN_MILLISECONDS = (30 * 1000).toLong()

    fun getTimestampString(context: Context?, messageDate: Date): String {
        var format: String? = null
        val language = Locale.getDefault().language
        val isZh = language.startsWith("zh")
        val messageTime = messageDate.time
        format = if (isSameDay(messageTime)) {
            "HH:mm:ss"
        } else if (isYesterday(messageTime)) {
            return SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(messageDate)
        } else {
            "MMM dd HH:mm:ss"
        }

        return if (isZh) {
            SimpleDateFormat(format, Locale.CHINESE).format(messageDate)
        } else {
            SimpleDateFormat(format, Locale.ENGLISH).format(messageDate)
        }
    }

    fun isCloseEnough(time1: Long, time2: Long): Boolean {
        var delta = time1 - time2
        if (delta < 0) {
            delta = -delta
        }
        return delta < INTERVAL_IN_MILLISECONDS
    }

    private fun isSameDay(inputTime: Long): Boolean {
        val tStartAndEndTime = todayStartAndEndTime
        return inputTime > tStartAndEndTime.startTime && inputTime < tStartAndEndTime.endTime
    }

    private fun isYesterday(inputTime: Long): Boolean {
        val yStartAndEndTime = yesterdayStartAndEndTime
        return inputTime > yStartAndEndTime.startTime && inputTime < yStartAndEndTime.endTime
    }

    @SuppressLint("SimpleDateFormat")
    fun StringToDate(dateStr: String?, formatStr: String?): Date? {
        val format: DateFormat = SimpleDateFormat(formatStr)
        var date: Date? = null
        try {
            date = format.parse(dateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    /**
     *
     * @param timeLength Millisecond
     * @return
     */
    @SuppressLint("DefaultLocale")
    fun toTime(timeLength: Int): String {
        var timeLength = timeLength
        timeLength /= 1000
        var minute = timeLength / 60
        var hour = 0
        if (minute >= 60) {
            hour = minute / 60
            minute = minute % 60
        }
        val second = timeLength % 60
        return String.format("%02d:%02d", minute, second)
    }

    /**
     *
     * @param timeLength second
     * @return
     */
    @SuppressLint("DefaultLocale")
    fun toTimeBySecond(timeLength: Int): String {
        var minute = timeLength / 60
        var hour = 0
        if (minute >= 60) {
            hour = minute / 60
            minute = minute % 60
        }
        val second = timeLength % 60
        // return String.format("%02d:%02d:%02d", hour, minute, second);
        return String.format("%02d:%02d", minute, second)
    }

    val yesterdayStartAndEndTime: TimeInfo
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1.add(Calendar.DATE, -1)
            calendar1[Calendar.HOUR_OF_DAY] = 0
            calendar1[Calendar.MINUTE] = 0
            calendar1[Calendar.SECOND] = 0
            calendar1[Calendar.MILLISECOND] = 0
            val startDate = calendar1.time
            val startTime = startDate.time
            val calendar2 = Calendar.getInstance()
            calendar2.add(Calendar.DATE, -1)
            calendar2[Calendar.HOUR_OF_DAY] = 23
            calendar2[Calendar.MINUTE] = 59
            calendar2[Calendar.SECOND] = 59
            calendar2[Calendar.MILLISECOND] = 999
            val endDate = calendar2.time
            val endTime = endDate.time
            val info = TimeInfo()
            info.startTime = startTime
            info.endTime = endTime
            return info
        }

    val todayStartAndEndTime: TimeInfo
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1[Calendar.HOUR_OF_DAY] = 0
            calendar1[Calendar.MINUTE] = 0
            calendar1[Calendar.SECOND] = 0
            calendar1[Calendar.MILLISECOND] = 0
            val startDate = calendar1.time
            val startTime = startDate.time
            val calendar2 = Calendar.getInstance()
            calendar2[Calendar.HOUR_OF_DAY] = 23
            calendar2[Calendar.MINUTE] = 59
            calendar2[Calendar.SECOND] = 59
            calendar2[Calendar.MILLISECOND] = 999
            val endDate = calendar2.time
            val endTime = endDate.time
            val info = TimeInfo()
            info.startTime = startTime
            info.endTime = endTime
            return info
        }

    val beforeYesterdayStartAndEndTime: TimeInfo
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1.add(Calendar.DATE, -2)
            calendar1[Calendar.HOUR_OF_DAY] = 0
            calendar1[Calendar.MINUTE] = 0
            calendar1[Calendar.SECOND] = 0
            calendar1[Calendar.MILLISECOND] = 0
            val startDate = calendar1.time
            val startTime = startDate.time
            val calendar2 = Calendar.getInstance()
            calendar2.add(Calendar.DATE, -2)
            calendar2[Calendar.HOUR_OF_DAY] = 23
            calendar2[Calendar.MINUTE] = 59
            calendar2[Calendar.SECOND] = 59
            calendar2[Calendar.MILLISECOND] = 999
            val endDate = calendar2.time
            val endTime = endDate.time
            val info = TimeInfo()
            info.startTime = startTime
            info.endTime = endTime
            return info
        }

    /**
     * endtime为今天
     * @return
     */
    val currentMonthStartAndEndTime: TimeInfo
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1[Calendar.DATE] = 1
            calendar1[Calendar.HOUR_OF_DAY] = 0
            calendar1[Calendar.MINUTE] = 0
            calendar1[Calendar.SECOND] = 0
            calendar1[Calendar.MILLISECOND] = 0
            val startDate = calendar1.time
            val startTime = startDate.time
            val calendar2 = Calendar.getInstance()
            val endDate = calendar2.time
            val endTime = endDate.time
            val info = TimeInfo()
            info.startTime = startTime
            info.endTime = endTime
            return info
        }

    val lastMonthStartAndEndTime: TimeInfo
        get() {
            val calendar1 = Calendar.getInstance()
            calendar1.add(Calendar.MONTH, -1)
            calendar1[Calendar.DATE] = 1
            calendar1[Calendar.HOUR_OF_DAY] = 0
            calendar1[Calendar.MINUTE] = 0
            calendar1[Calendar.SECOND] = 0
            calendar1[Calendar.MILLISECOND] = 0
            val startDate = calendar1.time
            val startTime = startDate.time
            val calendar2 = Calendar.getInstance()
            calendar2.add(Calendar.MONTH, -1)
            calendar2[Calendar.DATE] = 1
            calendar2[Calendar.HOUR_OF_DAY] = 23
            calendar2[Calendar.MINUTE] = 59
            calendar2[Calendar.SECOND] = 59
            calendar2[Calendar.MILLISECOND] = 999
            calendar2.roll(Calendar.DATE, -1)
            val endDate = calendar2.time
            val endTime = endDate.time
            val info = TimeInfo()
            info.startTime = startTime
            info.endTime = endTime
            return info
        }

    val timestampStr: String
        get() = java.lang.Long.toString(System.currentTimeMillis())

    /**
     * 判断是否是24小时制
     * @param context
     * @return
     */
    fun is24HourFormat(context: Context?): Boolean {
        return android.text.format.DateFormat.is24HourFormat(context)
    }
}