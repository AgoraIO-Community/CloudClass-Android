package io.agora.education.home.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.github.gzuliyujiang.wheelpicker.contract.LinkageProvider
import com.github.gzuliyujiang.wheelview.annotation.CurtainCorner
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.education.R
import io.agora.education.databinding.DialogSelectTimeBinding
import java.text.SimpleDateFormat
import java.util.*


/**
 * author : felix
 * date : 2022/9/6
 * description : select time
 */
class FcrSelectTimeDialog(context: Context) : FcrBaseSheetDialog(context) {
    lateinit var binding: DialogSelectTimeBinding
    var onSelectTimeListener: ((Long) -> Unit)? = null
    var mapDate: Map<String, Long>? = null
    var mapHour: Map<String, Int>? = null
    var mapMinutes: Map<String, Int>? = null
    var isSelectTime = false // 是否选择过时间

    override fun initView() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.tvOk.setOnClickListener {
            val date = mapDate?.get(binding.timeDatePicker.firstWheelView.getCurrentItem<String>())
            val hour = mapHour?.get(binding.timeDatePicker.secondWheelView.getCurrentItem<String>())
            val minutes = mapMinutes?.get(binding.timeDatePicker.thirdWheelView.getCurrentItem<String>())

            val calendar = Calendar.getInstance()
            calendar.time = Date(date!!)
            calendar.set(Calendar.HOUR_OF_DAY, hour!!)
            calendar.set(Calendar.MINUTE, minutes!!)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val currentCalendar = Calendar.getInstance()
            currentCalendar.set(Calendar.SECOND, 0)
            currentCalendar.set(Calendar.MILLISECOND, 0)

            if (currentCalendar.timeInMillis > calendar.timeInMillis) {
                ToastManager.showShort(context.getString(R.string.fcr_create_tips_starttime))
            }else{
                onSelectTimeListener?.invoke(calendar.timeInMillis)
                dismiss()
            }
        }

        binding.timeDatePicker.firstWheelView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT, 2f
        )

        binding.timeDatePicker.firstWheelView.curtainCorner = CurtainCorner.LEFT
        binding.timeDatePicker.secondWheelView.curtainCorner = CurtainCorner.NONE
        binding.timeDatePicker.thirdWheelView.curtainCorner = CurtainCorner.RIGHT

        //binding.timeDatePicker.setDefaultValue("今天","22","15")

        binding.timeDatePicker.setOnLinkageSelectedListener { first, second, third ->
            isSelectTime = true
        }
    }

    override fun show() {
        super.show()

        val time = TimeData(context)
        mapDate = time.mapDate
        mapHour = time.mapHour
        mapMinutes = time.mapMinutes

        binding.timeDatePicker.setData(time)
    }

    override fun getView(): View {
        binding = DialogSelectTimeBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }
}

class TimeData(var context: Context) : LinkageProvider {
    val dateFormat = SimpleDateFormat(context.resources.getString(R.string.fcr_create_picker_time_format))
    val hourFormat = SimpleDateFormat("HH")
    val mintuesFormat = SimpleDateFormat("mm")

    val mapDate = getDateData()
    val mapHour = getHourData()
    val mapMinutes = getMinutesData()

    fun getTodayHour():String {
        val calendar = Calendar.getInstance()
        return hourFormat.format(calendar.timeInMillis)
    }

    fun getTodayMinutes():String {
        val calendar = Calendar.getInstance()
        return mintuesFormat.format(calendar.timeInMillis)
    }

    override fun firstLevelVisible(): Boolean {
        return true
    }

    override fun thirdLevelVisible(): Boolean {
        return true
    }

    override fun provideFirstData(): MutableList<*> {
        return ArrayList<String>(mapDate.keys)
    }

    var secondTodayData = ArrayList<String>()
    var secondData = ArrayList<String>()

    override fun linkageSecondData(firstIndex: Int): MutableList<*> {
        if (firstIndex == 0) { // 今天
            val hour = getTodayHour().toInt()
            secondTodayData = ArrayList<String>()

            mapHour.forEach {
                if (it.value >= hour) {
                    secondTodayData.add(it.key)
                }
            }
            return secondTodayData
        } else {
            secondData = ArrayList<String>(mapHour.keys)
            return secondData
        }
    }

    var thirdTodayData = ArrayList<String>()
    var thirdData = ArrayList<String>()

    override fun linkageThirdData(firstIndex: Int, secondIndex: Int): MutableList<*> {
        if (firstIndex == 0 && secondIndex == 0) { // 今天
            val hour = getTodayMinutes().toInt()
            thirdTodayData = ArrayList<String>()

            mapMinutes.forEach {
                if (it.value >= hour) {
                    thirdTodayData.add(it.key)
                }
            }
            return thirdTodayData
        }

        thirdData = ArrayList<String>(mapMinutes.keys)
        return thirdData
    }

    override fun findFirstIndex(firstValue: Any?): Int {
        return ArrayList<String>(mapDate.keys).indexOf(firstValue)
    }

    override fun findSecondIndex(firstIndex: Int, secondValue: Any?): Int {
        if (firstIndex == 0) { // 今天
            return secondTodayData.indexOf(secondValue)
        }
        return secondData.indexOf(secondValue)
    }

    override fun findThirdIndex(firstIndex: Int, secondIndex: Int, thirdValue: Any?): Int {
        if (firstIndex == 0 && secondIndex == 0) { // 今天
            return thirdTodayData.indexOf(thirdValue)
        }
        return thirdData.indexOf(thirdValue)
    }

    fun getDateData(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        map[context.getString(R.string.fcr_create_label_today)] = calendar.timeInMillis

        for (i in 1..6) {
            calendar.add(Calendar.DAY_OF_MONTH, 1) // 每次+1天
            map[dateFormat.format(calendar.time)] = calendar.timeInMillis
        }
        return map
    }

    fun getHourData(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()

        for (i in 0..24) {
            if (i < 10) {
                map["0$i"] = i
            } else {
                map["$i"] = i
            }
        }
        return map
    }

    fun getMinutesData(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (i in 0..60 step 5) {
            if (i < 10) {
                map["0$i"] = i
            } else {
                map["$i"] = i
            }
        }
        return map
    }
}