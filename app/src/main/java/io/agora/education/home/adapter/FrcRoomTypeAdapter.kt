package io.agora.education.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.education.R
import io.agora.education.databinding.ItemCreateRoomClassTypeBinding
import io.agora.education.home.bean.FrcCreateRoomInfo

/**
 * author : felix
 * date : 2022/9/6
 * description : 房间类型列表
 */
class FrcRoomTypeAdapter : RecyclerView.Adapter<FrcRoomTypeAdapter.FrcCreateHolder>() {
    val roomList = ArrayList<FrcCreateRoomInfo>()
    var selectPosition = 0
    private var myClickListener: OnItemClickListener? = null

    init {
        roomList.add(
            FrcCreateRoomInfo(
                R.string.fcr_create_room_one_to_one,
                R.string.fcr_create_room_one_to_one_desc,
                R.drawable.fcr_join_class_type1
            )
        )

        roomList.add(
            FrcCreateRoomInfo(
                R.string.fcr_create_room_lecture_hall,
                R.string.fcr_create_room_lecture_hall_desc,
                R.drawable.fcr_join_class_type2
            )
        )

        roomList.add(
            FrcCreateRoomInfo(
                R.string.fcr_create_room_one_to_many,
                R.string.fcr_create_room_one_to_many_desc,
                R.drawable.fcr_join_class_type3
            )
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrcCreateHolder {
        val binding = ItemCreateRoomClassTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FrcCreateHolder(binding)
    }

    override fun onBindViewHolder(holder: FrcCreateHolder, position: Int) {
        holder.bindView(roomList[position], selectPosition == position)
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

    class FrcCreateHolder(var binding: ItemCreateRoomClassTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bindView(info: FrcCreateRoomInfo, isSelect: Boolean) {
            binding.layoutRoomItem.setBackgroundResource(info.roomBgRes)
            binding.tvRoomType.setText(info.roomTypeRes)
            binding.tvRoomDesc.setText(info.roomDescRes)
            binding.ivCreateSelect.visibility = if (isSelect) View.VISIBLE else View.GONE
            binding.ivCreateSelectReact.visibility = if (isSelect) View.VISIBLE else View.GONE
        }
    }
}

