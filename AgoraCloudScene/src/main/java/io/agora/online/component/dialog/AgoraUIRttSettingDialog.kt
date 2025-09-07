package io.agora.online.component.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduRttSettingDialogBinding
import io.agora.online.helper.RttSettingInfo


/**
 * 功能作用：字幕、转写设置弹窗
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
class AgoraUIRttSettingDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {

    private val binding: FcrOnlineEduRttSettingDialogBinding by lazy { FcrOnlineEduRttSettingDialogBinding.inflate(layoutInflater, null, false) }

    /**
     * 内容适配器
     */
    private val adapter = ContentAdapter(context)

    init {
        setContentView(binding.root)
        val window = this.window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        val layout = findViewById<ViewGroup>(R.id.agora_dialog_layout)
        layout.elevation = 10f
        initView()
    }

    private fun initView() {
        binding.agoraFcrRttSettingDialogList.adapter = adapter
        binding.agoraFcrRttSettingDialogList.layoutManager = LinearLayoutManager(context)
        binding.agoraFcrRttSettingDialogClose.setOnClickListener { dismiss() }
    }

    /**
     * 显示设置弹窗
     */
    fun show(currentSettingInfo: RttSettingInfo) {
        adapter.setConfigInfo(currentSettingInfo)
        super.show()
    }

    /**
     * 设置监听
     */
    fun setListener(listener: AgoraUIRttSettingDialogListener) {
        adapter.setListener(listener)
    }

}

/**
 * 设置弹窗监听
 */
interface AgoraUIRttSettingDialogListener {
    /**
     * 修改双语显示
     */
    fun changeDoubleShow(showDouble: Boolean)

    /**
     * 设置目标语言
     */
    fun setTargetLan(code: String)

    /**
     * 设置声源语言
     */
    fun setSourceLan(code: String)
}

/**
 * 语言选择列表适配器
 */
private class SelectListAdapter(var context: Context, var dataList: MutableList<SelectItem>) : RecyclerView.Adapter<ViewHolder>() {
    private var currentSelect: SelectItem? = null
    private var lastSelect: SelectItem? = null
    var onSelectChangedListener: OnSelectChangedListener? = null

    init {
        currentSelect = dataList.find { it.select }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.fcr_online_rtt_setting_dialog_list_select, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataList[position]
        viewHolder.iconCheck.visibility = if (item.select) View.VISIBLE else View.GONE
        viewHolder.iconDefault.visibility = if (!item.select) View.VISIBLE else View.GONE
        viewHolder.itemView.setOnClickListener {
            if (viewHolder.iconDefault.visibility == View.VISIBLE && item.allowSelect) {
                lastSelect = if (lastSelect == null) {
                    dataList.find { it.select }
                } else {
                    currentSelect
                }
                dataList.forEach { it.select = false }
//                dataList.find { it.code == lastSelect!!.code }?.select = false
                item.select = true
                currentSelect = item
                onSelectChangedListener?.onChanged(currentSelect!!)
                notifyItemRangeChanged(0, itemCount)
            }
        }
        viewHolder.title.text = dataList[position].text
    }

    /**
     * 重置到上一次
     */
    fun resetUseLast() {
        if (lastSelect != null) {
            currentSelect?.select = false
            currentSelect = lastSelect
            dataList.find { it.code == lastSelect!!.code }?.select = true
            lastSelect = null
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun getItemCount() = dataList.size
}

private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var iconCheck: AppCompatImageView = view.findViewById(R.id.dialog_rtt_select_item_icon_checked)
    var iconDefault: AppCompatImageView = view.findViewById(R.id.dialog_rtt_select_item_icon_default)
    var title: AppCompatTextView = view.findViewById(R.id.dialog_rtt_select_item_title)
}

/**
 * 语音列表选择改变监听
 */
private interface OnSelectChangedListener {
    fun onChanged(select: SelectItem)
}

/**
 * 内容显示适配器
 */
private class ContentAdapter(var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mFromAdapter by lazy { SelectListAdapter(context, mutableListOf()) }
    private val mToAdapter by lazy { SelectListAdapter(context, mutableListOf()) }

    /**
     * 配置信息
     */
    private var currentSettingInfo: RttSettingInfo? = null

    /**
     * 监听
     */
    private var listener: AgoraUIRttSettingDialogListener? = null

    /**
     * 内容的元素列表
     */
    private val contentConfigList = FcrRttSettingContentItemEnum.getShowConfigList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(getLayout(viewType), parent, false)) {}
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val itemEnum = contentConfigList[position]
        when(itemEnum){
            FcrRttSettingContentItemEnum.SOURCE_LAN_TITLE -> {
                (viewHolder.itemView as AppCompatTextView).setText(R.string.fcr_dialog_rtt_setting_dialog_title_origin)
            }
            FcrRttSettingContentItemEnum.SOURCE_LAN_LIST -> {
                (viewHolder.itemView as RecyclerView).apply {
                    adapter = mFromAdapter
                    mFromAdapter.onSelectChangedListener = object : OnSelectChangedListener {
                        override fun onChanged(select: SelectItem) {
                            val useText = "“${select.text}”"//使用的变色文本
                            val content = SpannableString(
                                String.format(resources.getString(R.string.fcr_dialog_rtt_setting_dialog_change_content), useText))
                            content.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.fcr_blue_357BF6)),
                                content.indexOf(useText), content.indexOf(useText) + useText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                            AgoraUIDialogBuilder(context).title(resources.getString(R.string.fcr_dialog_rtt_setting_dialog_change_title))
                                .message(content).messagePaddingHorizontal(resources.getDimensionPixelOffset(R.dimen.dp_10))
                                .negativeText(context.resources.getString(R.string.fcr_user_kick_out_cancel)).negativeClick {
                                    mFromAdapter.resetUseLast()
                                }.positiveText(context.resources.getString(R.string.fcr_dialog_rtt_setting_dialog_change_confirm)).positiveClick {
                                    listener?.setSourceLan(select.code)

                                }.build().show()
                        }
                    }
                    layoutManager = LinearLayoutManager(context)
                    for (i in 0 until itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }
                    addItemDecoration(ListSelectItemDecoration(this))
                }
            }
            FcrRttSettingContentItemEnum.TARGET_LAN_TITLE -> {
                (viewHolder.itemView as AppCompatTextView).setText(R.string.fcr_dialog_rtt_setting_dialog_title_result)
            }
            FcrRttSettingContentItemEnum.TARGET_LAN_LIST -> {
                (viewHolder.itemView as RecyclerView).apply {
                    adapter = mToAdapter
                    mToAdapter.onSelectChangedListener = object : OnSelectChangedListener {
                        override fun onChanged(select: SelectItem) {
                            listener?.setTargetLan(select.code)
                        }
                    }
                    layoutManager = LinearLayoutManager(context)
                    for (i in 0 until itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }
                    addItemDecoration(ListSelectItemDecoration(this))
                }
            }
            FcrRttSettingContentItemEnum.DOUBLE_LAN_SHOW_SWITCH -> {
                viewHolder.itemView.apply {
                    findViewById<AppCompatTextView>(R.id.fcr_online_rtt_setting_dialog_content_switch_title).setText(
                        R.string.fcr_dialog_rtt_setting_dialog_title_switch)
                    findViewById<AppCompatImageView>(R.id.fcr_online_rtt_setting_dialog_content_switch_icon).isActivated =
                        this@ContentAdapter.currentSettingInfo?.isShowDoubleLan() ?: false
                    setOnClickListener {
                        if (false == this@ContentAdapter.currentSettingInfo?.isShowDoubleLan()) {
                            findViewById<AppCompatImageView>(R.id.fcr_online_rtt_setting_dialog_content_switch_icon).isActivated = true
                            currentSettingInfo?.setShowDoubleLan(true)
                            listener?.changeDoubleShow(true)
                        } else {
                            findViewById<AppCompatImageView>(R.id.fcr_online_rtt_setting_dialog_content_switch_icon).isActivated = false
                            currentSettingInfo?.setShowDoubleLan(false)
                            listener?.changeDoubleShow(false)
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取布局类型
     */
    private fun getLayout(position: Int): Int {
        return contentConfigList[position].layoutResId
    }

    override fun getItemCount() = contentConfigList.size

    override fun getItemViewType(position: Int) = position

    /**
     * 设置配置信息
     */
    fun setConfigInfo(currentSettingInfo: RttSettingInfo) {
        this.currentSettingInfo = currentSettingInfo
        mFromAdapter.dataList.clear()
        for (item in currentSettingInfo.sourceListLan) {
            mFromAdapter.dataList.add(
                SelectItem(context.getString(item.textRes), item.value, true, item.value === currentSettingInfo.getSourceLan().value))
        }
        mToAdapter.dataList.clear()
        for (item in currentSettingInfo.targetListLan) {
            mToAdapter.dataList.add(
                SelectItem(context.getString(item.textRes), item.value, true, item.value == currentSettingInfo.getTargetLan().value ))
        }
        mFromAdapter.notifyItemRangeChanged(0, mFromAdapter.itemCount)
        mToAdapter.notifyItemRangeChanged(0, mToAdapter.itemCount)
        notifyItemRangeChanged(0, itemCount)
    }

    /**
     * 设置监听
     */
    fun setListener(listener: AgoraUIRttSettingDialogListener) {
        this.listener = listener
    }
}

/**
 * 选项列表分割线
 */
private class ListSelectItemDecoration(private val recyclerView: RecyclerView) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        this.isAntiAlias = true
        this.color = ContextCompat.getColor(recyclerView.context, R.color.fcr_im_input_bg_line)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) { // childCount - 1 to avoid drawing a divider after the last item
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            c.drawRect(recyclerView.paddingLeft.toFloat(), top.toFloat(), recyclerView.right.toFloat(),
                (top + recyclerView.resources.getDimensionPixelSize(R.dimen.dp_1)).toFloat(), paint)
        }
    }
}

/**
 * 选项
 */
private class SelectItem(val text: String, val code: String, val allowSelect: Boolean, var select: Boolean)

/**
 * Rtt设置内容弹窗的item枚举
 */
private enum class FcrRttSettingContentItemEnum(@LayoutRes val layoutResId: Int) {
    /**
     * 声源语言标题
     */
    SOURCE_LAN_TITLE(R.layout.fcr_online_rtt_setting_dialog_content_title),

    /**
     * 声源语言列表
     */
    SOURCE_LAN_LIST(R.layout.fcr_online_rtt_setting_dialog_content_list),

    /**
     * 翻译语言标题
     */
    TARGET_LAN_TITLE(R.layout.fcr_online_rtt_setting_dialog_content_title),

    /**
     * 翻译语言列表
     */
    TARGET_LAN_LIST(R.layout.fcr_online_rtt_setting_dialog_content_list),

    /**
     * 是否显示双语
     */
    DOUBLE_LAN_SHOW_SWITCH(R.layout.fcr_online_rtt_setting_dialog_content_switch);

    companion object {
        fun getShowConfigList(): List<FcrRttSettingContentItemEnum> {
            return listOf(SOURCE_LAN_TITLE, SOURCE_LAN_LIST, TARGET_LAN_TITLE, TARGET_LAN_LIST, DOUBLE_LAN_SHOW_SWITCH)
        }
    }
}
