package com.agora.edu.component.whiteboard.color

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.whiteboard.adpater.AgoraEduWbToolInfo
import com.agora.edu.component.whiteboard.tool.AgoraEduImageUtils
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduWbColorItemBinding
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig


/**
 * author : felix
 * date : 2022/2/16
 * description : 画笔颜色
 */
open class AgoraEduWbColorAdapter<T : AgoraEduWbToolInfo>(var config: AgoraUIDrawingConfig) :
    RecyclerView.Adapter<AgoraEduWbColorViewHolder<T>>() {

    val list: MutableList<T> = mutableListOf()
    var onClickItemListener: ((Int, T) -> Unit)? = null
    var selectPosition: Int = -1

    fun setViewData(listData: List<T>) {
        this.list.clear()
        this.list.addAll(listData)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgoraEduWbColorViewHolder<T> {
        val binding = AgoraEduWbColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgoraEduWbColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgoraEduWbColorViewHolder<T>, position: Int) {
        val info = list[position]

        val isSelectColor = (config.color == info.iconRes)  // 选择的颜色

        holder.itemView.setOnClickListener {
            config.color = info.iconRes
            if (holder.absoluteAdapterPosition < list.size) {
                onClickItemListener?.invoke(holder.absoluteAdapterPosition, list[holder.absoluteAdapterPosition])
            }
            notifyDataSetChanged()
        }
        if (isSelectColor) {
            setItemSize(holder, R.dimen.agora_edu_pen_color_size)

//            if (position == 0) {// 白色的
//                holder.binding.agoraApplianceImageview.setImageResource(R.drawable.agora_icon_color_white_select)
//            } else {
                AgoraEduImageUtils.setImageTintResource(
                    R.drawable.agora_icon_color_base_select,
                    info.iconRes,
                    holder.binding.agoraApplianceImageview
                )
//            }
        } else {
            setItemSize(holder, R.dimen.agora_edu_pen_color_select_size)

//            if (position == 0) {// 白色的
//                holder.binding.agoraApplianceImageview.setImageResource(R.drawable.agora_icon_color_white)
//            } else {
                AgoraEduImageUtils.setImageTintResource(R.drawable.agora_icon_color_base,
                    info.iconRes,
                    holder.binding.agoraApplianceImageview
                )
//            }
        }
    }

    fun setItemSize(holder: AgoraEduWbColorViewHolder<T>, itemSizeResId: Int) {
        val p = holder.binding.agoraApplianceImageview.layoutParams
        p.width = holder.binding.root.context.resources.getDimensionPixelSize(itemSizeResId)
        p.height = p.width
        holder.binding.agoraApplianceImageview.layoutParams = p
    }

    fun createColorDrawable(context: Context, fillColor: Int, strokeColor: Int, isSelect: Boolean): GradientDrawable {
        // val strokeColor: Int = Color.parseColor("#2E3135")
        // val fillColor: Int = Color.parseColor("#DFDFE0")
        val strokeWidth = context.resources.getDimensionPixelOffset(R.dimen.agora_edu_wb_open_color_stroke)
        val roundRadius = context.resources.getDimensionPixelOffset(R.dimen.agora_edu_wb_open_color_radius)

        val gd = GradientDrawable()
        gd.shape = GradientDrawable.RECTANGLE
        gd.cornerRadius = roundRadius.toFloat()
        gd.setColor(fillColor)
        if (isSelect) {
            gd.setStroke(strokeWidth, strokeColor)
        }
        return gd
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class AgoraEduWbColorViewHolder<T : AgoraEduWbToolInfo>(var binding: AgoraEduWbColorItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
}