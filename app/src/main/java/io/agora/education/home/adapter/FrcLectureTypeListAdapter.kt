package io.agora.education.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.education.R
import io.agora.education.databinding.ItemLectureRoomTypeBinding
import io.agora.education.home.bean.FrcLectureTypeInfo

/**
 * author : felix
 * date : 2022/9/6
 * description : 选择直播类型，如 rtc 极速直播 cdn
 */
class FrcLectureTypeListAdapter : RecyclerView.Adapter<FrcLectureTypeListAdapter.FrcLectureTypeHolder>() {
    val roomList = ArrayList<FrcLectureTypeInfo>()
    var selectPosition = 0 //当前选择了第几个
    private var myClickListener: OnItemClickListener? = null

    init {
        roomList.add(
            FrcLectureTypeInfo(
                R.string.fcr_create_label_servicetype_RTC,
                R.string.fcr_create_label_latency_RTC,
                R.drawable.fcr_quick1
            )
        )

        roomList.add(
            FrcLectureTypeInfo(
                R.string.fcr_create_label_servicetype_Standard,
                R.string.fcr_create_label_latency_Standard,
                R.drawable.fcr_quick2
            )
        )

        roomList.add(
            FrcLectureTypeInfo(
                R.string.fcr_create_label_servicetype_CDN,
                R.string.fcr_create_label_latency_CDN,
                R.drawable.fcr_quick3
            )
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrcLectureTypeHolder {
        val binding = ItemLectureRoomTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FrcLectureTypeHolder(binding)
    }

    override fun onBindViewHolder(holder: FrcLectureTypeHolder, position: Int) {
        holder.bindView(roomList[position], selectPosition == position, (itemCount - 1) != position)
        holder.itemView.setOnClickListener {
            selectPosition = position
            //当传入的myClickListener不为空就执行其中的方法
            myClickListener?.onClick(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return roomList.size
    }

    fun setOnItemClickListener(myClickListener: OnItemClickListener?) {
        this.myClickListener = myClickListener
    }

    interface OnItemClickListener {
        fun onClick(position: Int)
    }

    class FrcLectureTypeHolder(var binding: ItemLectureRoomTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bindView(info: FrcLectureTypeInfo, isSelect: Boolean, isShowLine: Boolean) {
            binding.ivLectureIcon.setImageResource(info.iconRes)
            binding.tvLectureTypeName.setText(info.roomTypeNameRes)
            binding.tvLectureDesc.setText(info.roomDescRes)
            binding.checkLecture.isSelected = isSelect
            binding.viewLine.visibility = if (isShowLine) View.VISIBLE else View.GONE
        }
    }
}

