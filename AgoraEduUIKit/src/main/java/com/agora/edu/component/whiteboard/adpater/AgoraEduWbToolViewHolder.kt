package com.agora.edu.component.whiteboard.adpater

import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeduuikit.databinding.AgoraEduApplianceItemBinding

/**
 * author : felix
 * date : 2022/2/16
 * description :
 */
open class AgoraEduWbToolViewHolder<T : AgoraEduWbToolInfo>(var binding: AgoraEduApplianceItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindView(info: T) {
        info.itemSize?.let {
            val layoutParams = binding.agoraApplianceItem.layoutParams
            layoutParams.width = it
            layoutParams.height = layoutParams.width
            binding.agoraApplianceItem.layoutParams = layoutParams
        }

        info.iconSize?.let {
            val iconParams = binding.agoraApplianceImageview.layoutParams
            iconParams.width = it
            iconParams.height = it
            binding.agoraApplianceImageview.layoutParams = iconParams
        }
    }
}