package io.agora.education.join

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.easeim.modules.view.ui.widget.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.education.R
import io.agora.education.databinding.DialogSelectClassTypeBinding
import io.agora.education.databinding.ItemSelectClassTypeBinding
import io.agora.education.home.dialog.FcrBaseSheetDialog
import io.agora.education.join.presenter.FcrFcrRoomTypeHelper


/**
 * author : felix
 * date : 2023/08/02
 * description :
 */
class FcrSelectRoomTypeDialog(context: Context, var roomType: FcrRoomType) : FcrBaseSheetDialog(context) {
    lateinit var binding: DialogSelectClassTypeBinding
    lateinit var roomTypeAdapter: FcrSelectRoomTypeAdapter
    var onSelectRoomType: ((FcrRoomType) -> Unit)? = null
        set(value) {
            roomTypeAdapter.onSelectRoomType = value
            field = value
        }

    companion object {
        fun newInstance(context: Context, roomType: FcrRoomType): FcrSelectRoomTypeDialog {
            return FcrSelectRoomTypeDialog(context, roomType).initViewData()
        }
    }

    override fun getView(): View {
        binding = DialogSelectClassTypeBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun initView() {
        binding.fcrRoomTypeList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.fcrRoomTypeList.setHasFixedSize(true)
    }

    fun initViewData(): FcrSelectRoomTypeDialog {
        roomTypeAdapter = FcrSelectRoomTypeAdapter(context, roomType)
        binding.fcrRoomTypeList.adapter = roomTypeAdapter
        return this
    }


    class FcrSelectRoomTypeAdapter(var context: Context, var roomType: FcrRoomType) : RecyclerView.Adapter<FcrSelectRoomTypeHolder>() {
        var onSelectRoomType: ((FcrRoomType) -> Unit)? = null
        val list = FcrFcrRoomTypeHelper.getRoomTypeList(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FcrSelectRoomTypeHolder {
            val binding = ItemSelectClassTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FcrSelectRoomTypeHolder(binding)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: FcrSelectRoomTypeHolder, position: Int) {
            holder.bindView(roomType.type.value, list[position])
            holder.itemView.setOnClickListener {
                roomType = list[position]
                onSelectRoomType?.invoke(roomType)
                notifyDataSetChanged()
            }
        }
    }

    class FcrSelectRoomTypeHolder(var binding: ItemSelectClassTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(roomType: Int, fcrRoomType: FcrRoomType) {

            binding.fcrTextClassType.text = fcrRoomType.name

            if (roomType == fcrRoomType.type.value) {
                binding.fcrClassItem.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_item_rect_1)
                binding.fcrClassCheck.isSelected = true
                binding.fcrTextClassType.setTextColor(ContextCompat.getColor(itemView.context, R.color.fcr_white))
            } else {
                binding.fcrClassItem.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_item_rect_2)
                binding.fcrClassCheck.isSelected = false
                binding.fcrTextClassType.setTextColor(ContextCompat.getColor(itemView.context, R.color.fcr_black))
            }
        }
    }

    data class FcrRoomType(
        val type: RoomType,
        val name: String,
    )

}