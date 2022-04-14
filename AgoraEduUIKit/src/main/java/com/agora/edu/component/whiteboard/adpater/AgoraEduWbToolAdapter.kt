package com.agora.edu.component.whiteboard.adpater

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.whiteboard.tool.AgoraEduImageUtils
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduApplianceItemBinding
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig

/**
 * author : hefeng
 * date : 2022/2/16
 * description : 通用
 */
open class AgoraEduToolsAdapter<T : AgoraEduWbToolInfo>(var config: AgoraUIDrawingConfig) :
    RecyclerView.Adapter<AgoraEduWbToolViewHolder<T>>() {

    val list: MutableList<T> = mutableListOf()

    // 对象和位置
    var onClickItemListener: ((Int, T) -> Unit)? = null

    var selectPosition: Int = -1 // 默认是第二个大小

    /**
     * 上一步
     */
    var isCanUndoStepsUpdate: Boolean = false

    /**
     * 下一步
     */
    var isCanRedoStepsUpdate: Boolean = false

    /**
     * 0 : 工具
     * 1 : 教具（笔和文本依据颜色设置）
     * 2 : 笔 - 形状（依据颜色设置）
     * 3 : 笔 - 粗细
     * 4 : 笔 - 颜色，不在这里
     * 5 : T 的大小
     */
    var operationType: Int = 0

    fun setViewData(listData: List<T>) {
        this.list.clear()
        this.list.addAll(listData)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgoraEduWbToolViewHolder<T> {
        val binding = AgoraEduApplianceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgoraEduWbToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgoraEduWbToolViewHolder<T>, position: Int) {
        holder.bindView(list[position])
        holder.itemView.setOnClickListener {
            // 选择了，就不要重复选择
            //if(selectPosition!=holder.absoluteAdapterPosition){
                selectPosition = holder.absoluteAdapterPosition
                onClickItemListener?.invoke(holder.absoluteAdapterPosition, list[selectPosition])
                notifyDataSetChanged()
            //}
        }

        if (selectPosition == position) { // 选择
            when (operationType) {
                1 -> { // 教具
                    val agoraEduApplianceInfo = list[position] as AgoraEduApplianceInfo
                    val type = agoraEduApplianceInfo.activeAppliance
                    val tint = getApplianceTint(holder, type)
                    AgoraEduImageUtils.setImageTintResource(list[position].iconRes, tint, holder.binding.agoraApplianceImageview)
                }

                else -> {
                    AgoraEduImageUtils.setImageTintResource(list[position].iconRes, config.color,
                        holder.binding.agoraApplianceImageview
                    )
                    //holder.binding.agoraApplianceImageview.imageTintList = ColorStateList.valueOf(config.color)
                }
            }
        } else { // 没有选择
            var tint: Int

            if (operationType == 1) { // 教具
                val agoraEduApplianceInfo = list[position] as AgoraEduApplianceInfo
                val type = agoraEduApplianceInfo.activeAppliance

                // 需要设置变灰的上一步，下一步
                if (type == WhiteboardApplianceType.WB_Pre) {
                    tint = getTintColor(holder.binding.root.context, isCanUndoStepsUpdate)
                } else if (type == WhiteboardApplianceType.WB_Next) {
                    tint = getTintColor(holder.binding.root.context, isCanRedoStepsUpdate)
                } else {  // 还原icon颜色
                    tint = ContextCompat.getColor(holder.binding.root.context, R.color.agora_wb_icon_def_color)
                }
            } else { // 还原icon颜色
                var colorDefRes = R.color.agora_wb_icon_def_color
                if (operationType == 3) {
                    colorDefRes = R.color.agora_wb_icon_def_circle_color
                }
                tint = ContextCompat.getColor(holder.binding.root.context, colorDefRes)
            }
            //holder.binding.agoraApplianceImageview.setImageResource(list[position].iconRes)
            //holder.binding.agoraApplianceImageview.imageTintList = ColorStateList.valueOf(tint)
            AgoraEduImageUtils.setImageTintResource(list[position].iconRes, tint, holder.binding.agoraApplianceImageview)
        }
    }

    fun getApplianceTint(holder: AgoraEduWbToolViewHolder<T>, type: WhiteboardApplianceType):Int {
        val tint: Int

        if (type == WhiteboardApplianceType.WB_Clear) {
            tint = ContextCompat.getColor(holder.binding.root.context, R.color.agora_wb_icon_def_color)
        } else if (type == WhiteboardApplianceType.WB_Pre) {
            tint = getTintColor(holder.binding.root.context, isCanUndoStepsUpdate)
        } else if (type == WhiteboardApplianceType.WB_Next) {
            tint = getTintColor(holder.binding.root.context, isCanRedoStepsUpdate)
        } else if (config.activeAppliance == WhiteboardApplianceType.Pen || config.activeAppliance == WhiteboardApplianceType.Text) {
            // 选择的颜色
            tint = config.color
        } else { // 默认选择的蓝色
            tint = ContextCompat.getColor(holder.binding.root.context, R.color.agora_def_color)
        }
        return tint
        //holder.binding.agoraApplianceImageview.imageTintList = ColorStateList.valueOf(tint)
    }

    fun getTintColor(context: Context, isCanUse: Boolean): Int {
        return if (isCanUse) {
            ContextCompat.getColor(context, R.color.agora_wb_icon_def_color)
        } else {
            ContextCompat.getColor(context, R.color.agora_wb_icon_gray_color)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}