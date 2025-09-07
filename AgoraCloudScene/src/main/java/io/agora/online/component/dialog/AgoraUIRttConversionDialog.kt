package io.agora.online.component.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduRttConversionDialogBinding
import io.agora.online.helper.RttLanguageEnum
import io.agora.online.helper.RttRecordItem
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 功能作用：Rtt实时转写显示弹窗
 * 创建人：王亮（Loren）
 * 思路：
 * 方法：
 * 注意：
 * 修改人：
 * 修改时间：
 * 备注：
 *
 * @author 王亮（Loren）
 */
class AgoraUIRttConversionDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {

    private val binding: FcrOnlineEduRttConversionDialogBinding by lazy {
        FcrOnlineEduRttConversionDialogBinding.inflate(layoutInflater, null, false)
    }

    /**
     * 操作回调
     */
    var optionsCallback: ConversionOptionsInterface? = null

    /**
     * 适配器
     */
    private val mAdapter = RecordAdapter(context, arrayListOf())

    init {
        setContentView(binding.root)
        val window = this.window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        val layout = findViewById<ViewGroup>(R.id.agora_dialog_layout)
        layout.elevation = 10f

        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.fcrOnlineEduRttConversionDialogChangeStatus.setOnClickListener {
            if (it.isActivated) {
                closeConversion(true)
            } else {
                openConversion(true)
            }
        }
        binding.fcrOnlineEduRttConversionDialogOptionsSetting.setOnClickListener {
            optionsCallback?.openSetting()
        }
        binding.fcrOnlineEduRttConversionDialogOptionsClose.setOnClickListener { dismiss() }
        binding.fcrOnlineEduRttConversionDialogList.adapter = mAdapter
        binding.fcrOnlineEduRttConversionDialogList.layoutManager = LinearLayoutManager(context)
        binding.fcrOnlineEduRttConversionDialogInput.setOnEditorActionListener { v, actionId, _ ->
            if (EditorInfo.IME_ACTION_SEARCH == actionId) {
                searchData(if (v.text != null) v.text.toString() else "")
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        binding.fcrOnlineEduRttConversionDialogOptionsGoPre.setOnClickListener {
            if (mAdapter.currentSearchResultIndex > 0) {
                mAdapter.currentSearchResultIndex--
                changeShowSearchResult()
            }
        }
        binding.fcrOnlineEduRttConversionDialogOptionsGoNext.setOnClickListener {
            if (mAdapter.currentSearchResultIndex + 1 < mAdapter.getSumResultCount()) {
                mAdapter.currentSearchResultIndex++
                changeShowSearchResult()
            }
        }
        binding.fcrOnlineEduRttConversionDialogSearchClear.setOnClickListener {
            binding.fcrOnlineEduRttConversionDialogInput.text = null
            binding.fcrOnlineEduRttConversionDialogInput.clearFocus()
            searchData(null)
        }
    }

    /**
     * 设置限时体验信息
     */
    fun setExperienceInfo(allowUseConfig: Boolean, rttExperienceReduceTime: Int) {
        binding.fcrOnlineEduRttConversionDialogChangeStatus.isEnabled = allowUseConfig || rttExperienceReduceTime > 0
        if (allowUseConfig) {
            binding.fcrOnlineEduRttConversionDialogTimeLimitHint.visibility = View.GONE
            binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.visibility = View.GONE
        } else {
            binding.fcrOnlineEduRttConversionDialogTimeLimitHint.visibility = View.VISIBLE
            if (rttExperienceReduceTime <= 0) {
                binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setText(R.string.fcr_dialog_rtt_time_limit_end)
                binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setBackgroundResource(R.drawable.agora_options_rtt_time_limit_bg_disable)
                binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.visibility = View.GONE
                closeConversion(true)
            } else {
                binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setText(R.string.fcr_dialog_rtt_time_limit)
                binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setBackgroundResource(R.drawable.agora_options_rtt_time_limit_bg)
                binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.visibility = View.VISIBLE
                binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.text = MessageFormat.format(
                    context.resources.getString(R.string.fcr_dialog_rtt_dialog_time_limit_reduce),
                    if (rttExperienceReduceTime / 60000 > 9) rttExperienceReduceTime / 60000 else "0${rttExperienceReduceTime / 60000}",
                    if (rttExperienceReduceTime % 60000 / 1000 > 9) rttExperienceReduceTime % 60000 / 1000 else "0${rttExperienceReduceTime % 60000 / 1000}",
                )
            }
        }
    }

    /**
     * 刷新记录信息
     */
    fun updateShowList(list: List<RttRecordItem>) {
        binding.root.post {
            if (mAdapter.dataList.size != list.size) {
                mAdapter.dataList.clear()
                mAdapter.dataList.addAll(list)
                mAdapter.notifyItemRangeChanged(0, list.size)
                if (mAdapter.searchText.isNullOrEmpty()) {
                    binding.fcrOnlineEduRttConversionDialogList.scrollToPosition(mAdapter.dataList.size - 1)
                }
            } else {
                if(list.isNotEmpty() && mAdapter.dataList.isNotEmpty()) {
                    mAdapter.dataList[mAdapter.dataList.size - 1] = list[list.size - 1]
                    mAdapter.notifyItemChanged(mAdapter.dataList.size - 1)
                    if (mAdapter.searchText.isNullOrEmpty()) {
                        binding.fcrOnlineEduRttConversionDialogList.scrollToPosition(mAdapter.dataList.size - 1)
                    }
                }
            }
            this.searchData(mAdapter.searchText)
        }
    }

    /**
     * 修改按钮状态
     */
    fun changeShowButton(toState:Boolean){
        if(toState){
            openConversion(false)
        }else{
            closeConversion(false)
        }
    }

    /**
     * 开始转写
     */
    private fun openConversion(sendRequest:Boolean) {
        if (binding.fcrOnlineEduRttConversionDialogChangeStatus.isEnabled) {
            if (sendRequest) {
                optionsCallback?.openConversion()
            }
            binding.fcrOnlineEduRttConversionDialogChangeStatus.isActivated = true
            binding.fcrOnlineEduRttConversionDialogChangeStatus.setText(R.string.fcr_dialog_rtt_dialog_conversion_close)
            binding.fcrOnlineEduRttConversionDialogChangeStatus.setTextColor(ContextCompat.getColor(context, R.color.fcr_text_red))
        }
    }

    /**
     * 关闭转写
     */
    private fun closeConversion(sendRequest:Boolean) {
        if (sendRequest) {
            optionsCallback?.closeConversion()
        }
        binding.fcrOnlineEduRttConversionDialogChangeStatus.isActivated = false
        binding.fcrOnlineEduRttConversionDialogChangeStatus.setText(R.string.fcr_dialog_rtt_dialog_conversion_open)
        binding.fcrOnlineEduRttConversionDialogChangeStatus.setTextColor(ContextCompat.getColor(context, R.color.fcr_white))
    }

    /**
     * 搜索数据
     */
    @SuppressLint("SetTextI18n")
    private fun searchData(text: String?) {
        mAdapter.searchText = text
        if (!text.isNullOrEmpty()) {
            mAdapter.searchResultListIndexMap.clear()
            mAdapter.dataList.forEachIndexed { index, rttRecordItem ->
                val showText = rttRecordItem.getShowText()
                val level1Text = showText[0]
                val level2Text = showText[1]
                findSubstringsIndexes(level1Text, text).apply {
                    addAll(findSubstringsIndexes(if (level2Text.isNullOrEmpty()) "" else level2Text, text).map { return@map it + (level1Text?.length ?: 0) })
                }.let {
                    if (it.isNotEmpty()) {
                        mAdapter.searchResultListIndexMap[index] = it
                    }
                }
            }
            mAdapter.currentSearchResultIndex =
                mAdapter.currentSearchResultIndex.coerceAtMost(mAdapter.itemCount - 1).coerceAtMost(mAdapter.getSumResultCount() - 1).coerceAtLeast(0)
            binding.fcrOnlineEduRttConversionDialogSearchClear.visibility = View.VISIBLE
            binding.fcrOnlineEduRttConversionDialogSearchCount.visibility = View.VISIBLE
            binding.fcrOnlineEduRttConversionDialogOptionsChangeLoc.visibility = View.VISIBLE
            changeShowSearchResult()
        } else {
            mAdapter.searchResultListIndexMap.clear()
            mAdapter.currentSearchResultIndex = 0
            mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount)
            binding.fcrOnlineEduRttConversionDialogSearchClear.visibility = View.GONE
            binding.fcrOnlineEduRttConversionDialogSearchCount.visibility = View.GONE
            binding.fcrOnlineEduRttConversionDialogOptionsChangeLoc.visibility = View.GONE
        }
    }

    /**
     * 显示弹窗
     */
    fun show(list: List<RttRecordItem>) {
        mAdapter.dataList.clear()
        mAdapter.dataList.addAll(list)
        openConversion(true)
        super.show()
    }

    /**
     * 从字符串中查找子字符串位置列表
     */
    private fun findSubstringsIndexes(input: String?, searchString: String): ArrayList<Int> {
        if (input.isNullOrEmpty()) {
            return arrayListOf()
        }
        val indexes = arrayListOf<Int>()
        var index = input.indexOf(searchString, 0)
        while (index >= 0) {
            indexes.add(index)
            index = input.indexOf(searchString, index + 1)
        }
        return indexes
    }

    /**
     * 修改显示的搜索结果
     */
    @SuppressLint("SetTextI18n")
    private fun changeShowSearchResult() {
        binding.fcrOnlineEduRttConversionDialogSearchCount.text = "${if(mAdapter.getSumResultCount() == 0)  "0" else (mAdapter.currentSearchResultIndex + 1)}/${mAdapter.getSumResultCount()}"
        binding.fcrOnlineEduRttConversionDialogOptionsGoPre.isEnabled = mAdapter.currentSearchResultIndex > 0
        binding.fcrOnlineEduRttConversionDialogOptionsGoNext.isEnabled = mAdapter.currentSearchResultIndex < (mAdapter.getSumResultCount() - 1)
        mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount)
        //滑动到指定位置
        var count = 0
        for (entry in mAdapter.searchResultListIndexMap) {
            if (count <= mAdapter.currentSearchResultIndex && mAdapter.currentSearchResultIndex < count + entry.value.size) {
                binding.fcrOnlineEduRttConversionDialogList.scrollToPosition(entry.key)
                break
            }
            count += entry.value.size
        }


    }
}

/**
 * 转写操作接口类
 */
interface ConversionOptionsInterface {
    /**
     * 开启转写
     */
    fun openConversion()

    /**
     * 关闭转写
     */
    fun closeConversion()

    /**
     * 打开设置页面
     */
    fun openSetting()
}

/**
 * 数据记录适配器
 */
private class RecordAdapter(private val context: Context, val dataList: ArrayList<RttRecordItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /**
     * 搜索结果下标
     * key值是列表中位置
     * value是数据元素位置
     */
    var searchResultListIndexMap = HashMap<Int, List<Int>>()

    /**
     * 搜索的文本
     */
    var searchText: String? = null

    /**
     * 当前显示的查询结果位置
     */
    var currentSearchResultIndex = 0

    /**
     * 获取总的查询结果数量
     */
    fun getSumResultCount(): Int {
        var count = 0
        for (entry in searchResultListIndexMap) {
            count += entry.value.size
        }
        return count
    }

    /**
     * 获取当前条目之前的结果数量
     */
    fun getPreItemResultCount(position: Int): Int {
        var count = 0
        for (entry in searchResultListIndexMap) {
            if (entry.key < position) {
                count += entry.value.size
            }
        }
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (dataList[viewType].statusText.isNullOrEmpty()) {
            return object : RecyclerView.ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fcr_online_rtt_conversion_dialog_list_content, parent, false)) {}
        }
        return object :
            RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.fcr_online_rtt_conversion_dialog_list_options, parent, false)) {}
    }

    override fun getItemCount() = dataList.size

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bean = dataList[position]
        if (bean.statusText.isNullOrEmpty()) {
            holder.itemView.let {
                Glide.with(context).load(bean.userHeader).skipMemoryCache(true).placeholder(R.drawable.agora_video_ic_audio_on)
                    .apply(RequestOptions.circleCropTransform()).into(it.findViewById(R.id.agora_fcr_rtt_text_dialog_user_header))
                it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_user_header_text).text =
                    if (bean.userName.isNullOrEmpty()) "" else bean.userName!!.substring(0, 1)
                it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_user_name).text = bean.userName
                it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_time).text =
                    (if (bean.time == null || bean.time == 0L) null else bean.time)?.let { time -> Date(time) }
                        ?.let { date -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(date) }

                val showText = bean.getShowText()
                val level1Text = showText[0]
                val level2Text = showText[1]
                if (!searchResultListIndexMap.containsKey(position)) {
                    it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_text_origin).text = level1Text
                    it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_text_result).text = level2Text
                } else {
                    val resultCount = getPreItemResultCount(position)
                    val sourceSpan = SpannableString(level1Text)
                    val targetTextSpan = if (level2Text.isNullOrEmpty()) null else SpannableString(level2Text)
                    searchResultListIndexMap[position]?.forEachIndexed { index, position ->
                        if (resultCount + index == currentSearchResultIndex) {
                            //当前是定位到的位置
                            if (position < sourceSpan.length) {
                                sourceSpan.setSpan(ForegroundColorSpan(Color.WHITE), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                sourceSpan.setSpan(BackgroundColorSpan(Color.parseColor("#4262FF")), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            } else {
                                targetTextSpan?.setSpan(ForegroundColorSpan(Color.WHITE), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                targetTextSpan?.setSpan(BackgroundColorSpan(Color.parseColor("#4262FF")), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        } else {
                            //非定位到的位置
                            if (position < sourceSpan.length) {
                                sourceSpan.setSpan(BackgroundColorSpan(Color.parseColor("#334262FF")), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            } else {
                                targetTextSpan?.setSpan(BackgroundColorSpan(Color.parseColor("#334262FF")), position, position + searchText!!.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }


                    }
                    it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_text_origin).text = sourceSpan
                    it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_text_result).text = targetTextSpan
                }

                it.findViewById<AppCompatTextView>(R.id.agora_fcr_rtt_text_dialog_text_result).apply {
                    visibility = if (RttLanguageEnum.NONE == bean.currentTargetLan) View.GONE else View.VISIBLE
                }
            }
        } else {
            holder.itemView.findViewById<AppCompatTextView>(R.id.fcr_online_rtt_conversion_dialog_list_options_text).text = bean.statusText
        }
    }
}