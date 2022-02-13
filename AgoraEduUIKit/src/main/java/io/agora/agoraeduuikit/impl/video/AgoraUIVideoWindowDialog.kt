package io.agora.agoraeduuikit.impl.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

@SuppressLint("InflateParams")
class AgoraUIVideoWindowDialog(info: AgoraUIUserDetailInfo, context: Context, private val optionListener: IAgoraOptionListener2, private val eduContext: EduContextPool?) : Dialog(context, R.style.agora_dialog) {
    private val tag = "AgoraUIOptionDialog"

    private val width = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_layout_width)
    private val elevation = 10

    private var roleInfo = info
    private val timerLimit = 5000L
    private val timerInterval = 1000L
    private var dialogCountDownTimer: CountDownTimer? = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            dismiss()
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }

    fun show(anchor: View?) {
        init(anchor)
        dialogCountDownTimer?.start()
        super.show()
    }

    private fun init(anchor: View?) {
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        initStyleDialog(anchor)
    }

    private fun initStyleDialog(anchor: View?) {
        val layout = LayoutInflater.from(context).inflate(
            R.layout.agora_teacher_option_dialog_layout, null, false)
        setContentView(layout)

        val dialog = layout.findViewById<RelativeLayout>(R.id.agora_tool_popup_layout)
        dialog.clipToOutline = true
        dialog.elevation = elevation.toFloat()
        var params = dialog.layoutParams
        params.width = width - 50
        dialog.layoutParams = params

        val recycler = layout.findViewById<RecyclerView>(R.id.agora_teacher_option_dialog_recycler)

        if (eduContext?.userContext()?.getLocalUserInfo()?.userUuid == roleInfo.userUuid
            && roleInfo.role == AgoraEduContextUserRole.Teacher) { //本地老师，点了老师窗口
            if (AgoraUIConfig.isLargeScreen) {
                dialog.layoutParams.width = width - 50
            } else {
                dialog.layoutParams.width = width - 200
            }
            recycler.layoutManager = GridLayoutManager(context, 3)
        } else if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher
            && roleInfo.role == AgoraEduContextUserRole.Student) {//本地老师，点了学生窗口
            if (AgoraUIConfig.isLargeScreen) {
                dialog.layoutParams.width = width
            } else {
                dialog.layoutParams.width = width - 50
            }
            recycler.layoutManager = GridLayoutManager(context, 5)

        } else if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student
            && roleInfo.userUuid == eduContext?.userContext()?.getLocalUserInfo()?.userUuid) {

            if (AgoraUIConfig.isLargeScreen) {
                dialog.layoutParams.width = width - 120
            } else {
                dialog.layoutParams.width = width - 250
            }
            recycler.layoutManager = GridLayoutManager(context, 2)
        }
        recycler.adapter = OptionAdapter(recycler)

        val height = context.resources.getDimensionPixelSize(R.dimen.agora_status_bar_height)
        anchor?.let { OptionDialogUtil.adjustPosition(this.window!!, it, width, height) }
    }

    private class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_style_item_icon)
    }

    private inner class OptionAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<OptionViewHolder>() {

        var iconsRes: Array<OptionItem> = when {
            eduContext?.userContext()?.getLocalUserInfo()?.userUuid == roleInfo.userUuid
                && roleInfo.role == AgoraEduContextUserRole.Teacher -> {
                arrayOf(
                    OptionItem(R.drawable.agora_option_icon_audio, "voice"),
                    OptionItem(R.drawable.agora_option_icon_video, "video"),
                    OptionItem(R.drawable.agora_option_icon_cohost, "cohost"))
            }
            eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher
                && roleInfo.role == AgoraEduContextUserRole.Student -> {
                arrayOf(
                    OptionItem(R.drawable.agora_option_icon_audio, "voice"),
                    OptionItem(R.drawable.agora_option_icon_video, "video"),
                    OptionItem(R.drawable.agora_option_icon_cohost, "cohost"),
                    OptionItem(R.drawable.agora_option_icon_grant, "grant"),
                    OptionItem(R.drawable.agora_option_icon_reward, "reward"))
            }
            eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student
                && roleInfo.userUuid == eduContext?.userContext()?.getLocalUserInfo()?.userUuid -> {
                arrayOf(
                    OptionItem(R.drawable.agora_option_icon_audio, "voice"),
                    OptionItem(R.drawable.agora_option_icon_video, "video")
                )
            }
            else -> {
                arrayOf()
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {

            return OptionViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.agora_teacher_option_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {

            val itemWidth = if (eduContext?.userContext()?.getLocalUserInfo()?.userUuid == roleInfo.userUuid
                && roleInfo.role == AgoraEduContextUserRole.Teacher) {
                recyclerView.width / 3
            } else if (roleInfo.role == AgoraEduContextUserRole.Student
                && eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
                recyclerView.width / 5
            } else {
                recyclerView.width / 2
            }
            var params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
            params.width = itemWidth
            params.height = itemWidth
            val margin = (recyclerView.height - params.height) / 2
            params.topMargin = margin
            params.bottomMargin = margin
            holder.itemView.layoutParams = params

            params = holder.icon.layoutParams as RelativeLayout.LayoutParams
            params.height = (recyclerView.height * 2f / 3).toInt()
            params.width = params.height
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = params


            val pos = holder.absoluteAdapterPosition
            val item = iconsRes[pos]

            when (item.itemName) {
                "voice" -> {
                    if (roleInfo.audioSourceState == AgoraEduContextMediaSourceState.Error
                        || roleInfo.audioSourceState == AgoraEduContextMediaSourceState.Close) {
                        holder.icon.isEnabled = false
                        holder.icon.isSelected = false
                    } else {
                        holder.icon.isEnabled = true
                        holder.icon.isSelected = roleInfo.hasAudio
                    }
                }
                "video" -> {
                    if (roleInfo.videoSourceState == AgoraEduContextMediaSourceState.Error
                        || roleInfo.videoSourceState == AgoraEduContextMediaSourceState.Close) {
                        holder.icon.isEnabled = false
                        holder.icon.isSelected = false
                    } else {
                        holder.icon.isEnabled = true
                        holder.icon.isSelected = roleInfo.hasVideo
                    }

                }
                "cohost" -> {
                    holder.icon.isActivated = !roleInfo.isCoHost
                }
                "grant" -> {
                    holder.icon.isActivated = !roleInfo.whiteBoardGranted

                }
            }
            holder.icon.setImageResource(item.res)
            holder.itemView.setOnClickListener {
                when (item.itemName) {
                    "voice" -> {
                        if (holder.icon.isEnabled) {
//                            optionListener.onAudioUpdated(roleInfo, !holder.icon.isSelected)//todo
                            holder.icon.isSelected = !holder.icon.isSelected
                        }
                    }
                    "video" -> {
                        if (holder.icon.isEnabled) {
//                            optionListener.onVideoUpdated(roleInfo, !holder.icon.isSelected)
                            holder.icon.isSelected = !holder.icon.isSelected
                        }
                    }
                    "cohost" -> {
                        if (roleInfo.role != AgoraEduContextUserRole.Teacher) {
//                            optionListener.onCohostUpdated(roleInfo, holder.icon.isActivated)
                            holder.icon.isActivated = !holder.icon.isActivated
                        }
                    }
                    "grant" -> {
//                        optionListener.onGrantUpdated(roleInfo, holder.icon.isActivated)
                        holder.icon.isActivated = !holder.icon.isActivated

                    }
                    "reward" -> {
//                        optionListener.onRewardUpdated(roleInfo, 1)
                    }
                }
                dialogCountDownTimer?.cancel()
                dialogCountDownTimer?.start()

            }
        }

        override fun getItemCount(): Int {
            return iconsRes.size
        }
    }

    data class OptionItem(val res: Int, val itemName: String)

    override fun onStop() {
        super.onStop()
        dialogCountDownTimer?.cancel()
        dialogCountDownTimer = null
    }
}

private object OptionDialogUtil {
    // window：指的就是这个dialog
    // anchor：指的就是点击的view
    fun adjustPosition(window: Window, anchor: View, width: Int, height: Int) {
        val params = window.attributes
        hideStatusBar(window)

        params.width = width
        params.height = height
        params.gravity = Gravity.TOP or Gravity.START

        val locationsOnScreen = IntArray(2)
        anchor.getLocationOnScreen(locationsOnScreen)
        params.x = locationsOnScreen[0] + anchor.width / 2 - width / 2
        params.y = locationsOnScreen[1] + anchor.height + 2
        window.attributes = params
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}