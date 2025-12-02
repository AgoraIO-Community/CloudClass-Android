package io.agora.education.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeducore.core.internal.framework.bean.FcrSceneType
import io.agora.education.utils.AppUtil
import io.agora.education.R
import io.agora.education.databinding.ItemJoinClassStateBinding
import io.agora.education.home.dialog.FcrShareDialog
import io.agora.education.request.bean.FcrRoomDetail

/**
 * author : felix
 * date : 2022/9/5
 * description :
 */
class FrcJoinListAdapter : FcrPreloadAdapter<FrcJoinListAdapter.FrcJoinListHolder>() {
    var list: ArrayList<FcrRoomDetail> = ArrayList()
    var onEnterRoomListener: ((FcrRoomDetail) -> Unit)? = null

    fun setData(listData: List<FcrRoomDetail>, isAppend: Boolean) {
        if (!isAppend) {
            list.clear()
        }
        list.addAll(listData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrcJoinListHolder {
        val binding = ItemJoinClassStateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FrcJoinListHolder(binding)
    }

    override fun onBindViewHolder(holder: FrcJoinListHolder, position: Int) {
        holder.bindView(list[position], onEnterRoomListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class FrcJoinListHolder(var binding: ItemJoinClassStateBinding) : RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bindView(info: FcrRoomDetail, onEnterRoomListener: ((FcrRoomDetail) -> Unit)?) {
            setTitleColor(binding, R.color.fcr_join_text_black)
            setContentColor(binding, R.color.fcr_join_text_color)

            binding.tvJoinClassState.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            binding.tvJoinClassIdContent.text = info.roomId
            binding.ivJoinClassShare.visibility = View.VISIBLE
            binding.tvJoinClassTime.text = AppUtil.getRoomDate2(binding.root.context, info.startTime, info.duration)
            binding.tvJoinClassType.text = ""
            binding.tvJoinClassTips.visibility = View.GONE
            binding.layoutJoinAlphaItem.alpha = 1f
            binding.tvJoinClassEnter.visibility = View.VISIBLE
            binding.tvJoinClassName.text = info.roomName
            binding.tvJoinClassEnter.setBackgroundResource(R.drawable.bg_join_blue)

            binding.tvJoinClassEnter.setOnClickListener {
                onEnterRoomListener?.invoke(info)
            }

            binding.tvJoinClassIdContent.setOnClickListener {
                // copy id
                AppUtil.copyToClipboard(context, info.roomId)
                Toast.makeText(context, context.getString(R.string.fcr_join_copy_success), Toast.LENGTH_SHORT).show()
            }

            binding.ivJoinClassShare.setOnClickListener {
                // 分享链接
                //val userName = AppUserInfoUtils.getUserInfo()?.userName
                FcrShareDialog.newInstance(context).setLinkUrl(info.getShareLink(context, info.roomName)).show()
            }

            when (info.sceneType) {
                FcrSceneType.ONE_ON_ONE.value -> {
                    binding.tvJoinClassType.text = context.getString(R.string.fcr_create_room_one_to_one)
                }

                FcrSceneType.SMALL_CLASS.value -> {
                    binding.tvJoinClassType.text = context.getString(R.string.fcr_create_room_one_to_many)
                }

                FcrSceneType.LARGE_CLASS.value -> {
                    binding.tvJoinClassType.text = context.getString(R.string.fcr_create_room_lecture_hall)
                }

                FcrSceneType.CLOUD_CLASS.value -> {
                    binding.tvJoinClassType.text = context.getString(R.string.fcr_login_free_class_mode_option_cloud_class)
                }
            }

            // 课前 // 课中 // 课后
            if (info.roomState == 0) {
                binding.layoutJoinItem.setBackgroundResource(R.drawable.bg_join_class_state_before)
                binding.tvJoinClassState.text = context.getString(R.string.fcr_join_state_before)
                binding.ivJoinClassShare.setImageResource(R.drawable.fcr_join_share)

                binding.tvJoinClassTime.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_time),
                    null,
                    null,
                    null
                )

                binding.tvJoinClassType.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_label),
                    null,
                    null,
                    null
                )

                binding.tvJoinClassIdContent.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_copy),
                    null
                )
            } else if (info.roomState == 1) {
                setTitleColor(binding, R.color.fcr_join_text_white)
                setContentColor(binding, R.color.fcr_join_text_white)

                binding.layoutJoinItem.setBackgroundResource(R.drawable.bg_join_class_state_join)
                binding.tvJoinClassState.text = context.getString(R.string.fcr_join_state_join)
                binding.tvJoinClassState.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_online),
                    null,
                    null,
                    null
                )
                binding.ivJoinClassShare.setImageResource(R.drawable.fcr_join_share2)
                binding.tvJoinClassTime.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_time2),
                    null,
                    null,
                    null
                )

                binding.tvJoinClassType.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_label2),
                    null,
                    null,
                    null
                )
                binding.tvJoinClassEnter.setBackgroundResource(R.drawable.bg_join_class_black_btn)
                binding.tvJoinClassIdContent.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_copy2),
                    null
                )
            } else if (info.roomState == 2) {
                binding.layoutJoinItem.setBackgroundResource(R.drawable.bg_join_class_state_after)
                binding.tvJoinClassState.text = context.getString(R.string.fcr_join_state_after)
                binding.ivJoinClassShare.visibility = View.GONE
                binding.tvJoinClassTips.visibility = View.VISIBLE
                binding.tvJoinClassEnter.visibility = View.GONE
                binding.layoutJoinAlphaItem.alpha = 0.5f

                binding.tvJoinClassTime.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_time),
                    null,
                    null,
                    null
                )

                binding.tvJoinClassType.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.fcr_join_label),
                    null,
                    null,
                    null
                )
                binding.tvJoinClassIdContent.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
        }

        fun setTitleColor(binding: ItemJoinClassStateBinding, colorResId: Int){
            val color = ContextCompat.getColor(binding.root.context, colorResId)
            binding.tvJoinClassState.setTextColor(color)
            binding.tvJoinClassId.setTextColor(color)
            binding.tvJoinClassIdContent.setTextColor(color)
            binding.tvJoinClassName.setTextColor(color)
            binding.viewJoinRoomLine.setBackgroundColor(color)
        }

        fun setContentColor(binding: ItemJoinClassStateBinding, colorResId: Int){
            val color = ContextCompat.getColor(binding.root.context, colorResId)
            binding.tvJoinClassTime.setTextColor(color)
            binding.tvJoinClassType.setTextColor(color)
            binding.tvJoinClassTips.setTextColor(color)
        }
    }
}

