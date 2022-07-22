package io.agora.agoraeduuikit.impl.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeduuikit.impl.video.OptionItemType.Audio
import io.agora.agoraeduuikit.impl.video.OptionItemType.Video
import io.agora.agoraeduuikit.impl.video.OptionItemType.Cohost
import io.agora.agoraeduuikit.impl.video.OptionItemType.Reward
import io.agora.agoraeduuikit.impl.video.OptionItemType.Grant
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState.Error
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState.Close
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType

@SuppressLint("InflateParams")
class AgoraEduFloatingControlWindow(
    info: AgoraUIUserDetailInfo,
    context: Context,
    private val optionListener: IAgoraOptionListener,
    private val eduContext: EduContextPool?
) : Dialog(context, R.style.agora_dialog) {
    private val tag = "AgoraEduFloatingControlWindow"
    private val elevation = 10
    private var curInfo = info
    private val timerLimit = 5000L
    private val timerInterval = 1000L
    private var dialogCountDownTimer: CountDownTimer? = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            dismiss()
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }
    private val optionItemProviderFactory = OptionItemProviderFactory(eduContext)

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
            R.layout.agora_teacher_option_dialog_layout, null, false
        )
        setContentView(layout)

        val recycler = layout.findViewById<RecyclerView>(R.id.agora_teacher_option_dialog_recycler)
        recycler.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        val iconsRes = optionItemProviderFactory.getOptionItems(curInfo.userUuid, curInfo.role)
        recycler.adapter = OptionAdapter(iconsRes)

        val popupLayout = layout.findViewById<FrameLayout>(R.id.agora_tool_popup_layout)
        popupLayout.clipToOutline = true
        popupLayout.elevation = elevation.toFloat()
        val popupVerticalMargin = popupLayout.marginTop + popupLayout.marginBottom
        val popupHorizontalMargin = popupLayout.marginStart + popupLayout.marginEnd
        val popupVerticalPadding = popupLayout.paddingTop + popupLayout.paddingBottom
        val popupHorizontalPadding = popupLayout.paddingStart + popupLayout.paddingEnd
        val itemSize = context.resources.getDimensionPixelSize(R.dimen.agora_floating_window_option_ic_size)
        val height = popupVerticalMargin + popupVerticalPadding + itemSize
        val itemPaddingHorizontal = context.resources.getDimensionPixelSize(R.dimen.padding_smaller) * 2
        val width = popupHorizontalMargin + popupHorizontalPadding + (itemSize + itemPaddingHorizontal) * iconsRes.size
        anchor?.let {
            // window will show on the top of anchor.bottom when roomType is oneToOne
            if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.ONE_ON_ONE) {
                OptionDialogUtil.adjustPosition(this.window!!, it, width.toInt(), height.toInt(), true)
            } else {
                OptionDialogUtil.adjustPosition(this.window!!, it, width.toInt(), height.toInt())
            }
        }
    }

    private class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_style_item_icon)
    }

    private inner class OptionAdapter(val iconsRes: Array<OptionItem>) : RecyclerView.Adapter<OptionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
            return OptionViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_teacher_option_item_layout, parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            val pos = holder.absoluteAdapterPosition
            val item = iconsRes[pos]

            when (item.type) {
                Audio -> {
                    eduContext?.userContext()?.getLocalUserInfo()?.userUuid?.let {
                        if (curInfo.userUuid == it) {
                            holder.icon.isEnabled = curInfo.audioSourceState != Error
                            holder.icon.isSelected = curInfo.audioSourceState == Close
                        } else {
                            holder.icon.isEnabled =
                                curInfo.audioSourceState != Error && curInfo.audioSourceState != Close
                            holder.icon.isSelected = !curInfo.hasAudio
                        }
                    }
                }
                Video -> {
                    eduContext?.userContext()?.getLocalUserInfo()?.userUuid?.let {
                        if (curInfo.userUuid == it) {
                            holder.icon.isEnabled = curInfo.videoSourceState != Error
                            holder.icon.isSelected = curInfo.videoSourceState == Close
                        } else {
                            holder.icon.isEnabled =
                                curInfo.videoSourceState != Error && curInfo.videoSourceState != Close
                            holder.icon.isSelected = !curInfo.hasVideo
                        }
                    }
                }
                Grant -> {
                    holder.icon.isSelected = !curInfo.whiteBoardGranted
                }
                else -> {
                }
            }
            holder.icon.setImageResource(item.res)
            holder.itemView.setOnClickListener {
                when (item.type) {
                    Audio -> {
                        if (holder.icon.isEnabled) {
                            optionListener.onAudioUpdated(curInfo, holder.icon.isSelected)
                            holder.icon.isSelected = !holder.icon.isSelected
                        }
                    }
                    Video -> {
                        if (holder.icon.isEnabled) {
                            optionListener.onVideoUpdated(curInfo, holder.icon.isSelected)
                            holder.icon.isSelected = !holder.icon.isSelected
                        }
                    }
                    Cohost -> {
                        optionListener.onCohostUpdated(curInfo, false)
                        dismiss()
                    }
                    Grant -> {
                        optionListener.onGrantUpdated(curInfo, holder.icon.isSelected)
                        holder.icon.isSelected = !holder.icon.isSelected
                    }
                    Reward -> {
                        optionListener.onRewardUpdated(curInfo, 1)
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

    override fun onStop() {
        super.onStop()
        dialogCountDownTimer?.cancel()
        dialogCountDownTimer = null
    }
}

private object OptionDialogUtil {
    // window：指的就是这个dialog
    // anchor：指的就是点击的view
    fun adjustPosition(window: Window, anchor: View, width: Int, height: Int, top: Boolean = false) {
        val params = window.attributes
        hideStatusBar(window)

        params.width = width
        params.height = height
        params.gravity = Gravity.TOP or Gravity.START

        val locationsOnScreen = IntArray(2)
        anchor.getLocationOnScreen(locationsOnScreen)
        params.x = locationsOnScreen[0] + anchor.width / 2 - width / 2
        if (!top) {
            params.y = locationsOnScreen[1] + anchor.height
        } else {
            params.y = locationsOnScreen[1] + anchor.height - height
        }
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

private data class OptionItem(val res: Int, val type: OptionItemType)

private enum class OptionItemType(val value: Short) {
    Audio(0),
    Video(1),
    Cohost(2),
    Reward(3),
    Grant(4);
}

private abstract class OptionItemProvider(val eduContext: EduContextPool?) {
    abstract fun getOptionItems(userUuid: String, role: AgoraEduContextUserRole): Array<OptionItem>
}

private class OptionItemProviderFactory(eduContext: EduContextPool?) : OptionItemProvider(eduContext) {
    private val provideStrategy = buildProvider(eduContext)

    override fun getOptionItems(userUuid: String, role: AgoraEduContextUserRole): Array<OptionItem> {
        return provideStrategy.getOptionItems(userUuid, role)
    }

    private fun buildProvider(eduContext: EduContextPool?): OptionItemProvider {
        return when (eduContext?.roomContext()?.getRoomInfo()?.roomType) {
            RoomType.ONE_ON_ONE -> { OptionItemProvideStrategyByOneToOne(eduContext) }
            RoomType.SMALL_CLASS -> { OptionItemProvideStrategyBySmall(eduContext) }
            RoomType.GROUPING_CLASS -> { OptionItemProvideStrategyBySmall(eduContext) }
            RoomType.LARGE_CLASS -> { OptionItemProvideStrategyByLecture(eduContext) }
            else -> { OptionItemProvideStrategyByLecture(eduContext) }
        }
    }

    private class OptionItemProvideStrategyByOneToOne(eduContext: EduContextPool?) : OptionItemProvider(eduContext) {
        override fun getOptionItems(userUuid: String, role: AgoraEduContextUserRole): Array<OptionItem> {
            val localUser = eduContext?.userContext()?.getLocalUserInfo()
            return when {
                //老师角色点击老师窗口
                localUser?.userUuid == userUuid && role == Teacher -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio)
                    )
                }
                //本地角色是老师，操作学生窗口
                localUser?.role == Teacher && role == Student -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio),
                        OptionItem(R.drawable.agora_floating_window_ic_reward_on, Reward),
                        OptionItem(R.drawable.agora_floating_window_ic_grant, Grant)
                    )
                }
                else -> {
                    arrayOf()
                }
            }
        }
    }

    private class OptionItemProvideStrategyBySmall(eduContext: EduContextPool?) : OptionItemProvider(eduContext) {
        override fun getOptionItems(userUuid: String, role: AgoraEduContextUserRole): Array<OptionItem> {
            val localUser = eduContext?.userContext()?.getLocalUserInfo()
            return when {
                //老师角色点击老师窗口
                localUser?.userUuid == userUuid && role == Teacher -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio),
                        OptionItem(R.drawable.agora_floating_window_ic_cohost_on, Cohost)
                    )
                }
                //本地角色是老师，操作学生窗口
                localUser?.role == Teacher && role == Student -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio),
                        OptionItem(R.drawable.agora_floating_window_ic_reward_on, Reward),
                        OptionItem(R.drawable.agora_floating_window_ic_grant, Grant),
                        OptionItem(R.drawable.agora_floating_window_ic_cohost_on, Cohost),
                    )
                }
                else -> {
                    arrayOf()
                }
            }
        }
    }

    private class OptionItemProvideStrategyByLecture(eduContext: EduContextPool?) : OptionItemProvider(eduContext) {
        override fun getOptionItems(userUuid: String, role: AgoraEduContextUserRole): Array<OptionItem> {
            val localUser = eduContext?.userContext()?.getLocalUserInfo()
            return when {
                //老师角色点击老师窗口
                localUser?.userUuid == userUuid && role == Teacher -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio),
                        OptionItem(R.drawable.agora_floating_window_ic_cohost_on, Cohost)
                    )
                }
                //本地角色是老师，操作学生窗口
                localUser?.role == Teacher && role == Student -> {
                    arrayOf(
                        OptionItem(R.drawable.agora_floating_window_ic_video, Video),
                        OptionItem(R.drawable.agora_floating_window_ic_audio, Audio),
                        OptionItem(R.drawable.agora_floating_window_ic_cohost_on, Cohost)
                    )
                }
                else -> {
                    arrayOf()
                }
            }
        }
    }
}