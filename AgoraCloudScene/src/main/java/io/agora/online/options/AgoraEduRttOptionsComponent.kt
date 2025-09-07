package io.agora.online.options

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.agora.online.R
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.databinding.FcrOnlineEduRttOptionsComponentBinding
import io.agora.online.helper.RttOptionsManager
import java.text.MessageFormat


class AgoraEduRttOptionsComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineEduRttOptionsComponentBinding =
        FcrOnlineEduRttOptionsComponentBinding.inflate(LayoutInflater.from(context), this, true)

    private var rttOptionsManager: RttOptionsManager? = null

    init {
        binding.root.setOnClickListener {
            rttOptionsManager?.openSetting()
        }
    }

    fun initView(rttOptionsManager: RttOptionsManager) {
        this.rttOptionsManager = rttOptionsManager
        binding.agoraFcrRttTextDialogClose.setOnClickListener {
            rttOptionsManager.closeSubtitles()
        }
    }

    private var touchX: Float = 0F
    private var touchY: Float = 0F
    private var move = false
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                move = false
            }

            MotionEvent.ACTION_MOVE -> {
                move = true
                val params = layoutParams as MarginLayoutParams
                params.leftMargin = Math.max(0, Math.min(params.leftMargin + (event.x - touchX).toInt(), (parent as View).width - width))
                params.bottomMargin = Math.max(0, Math.min(params.bottomMargin - (event.y - touchY).toInt(), (parent as View).height - height))
                setLayoutParams(params)
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (move) {
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * 重置显示位置
     */
    fun resetShowPosition() {
        runOnUIThread {
            if (width == 0) {
                viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        resetShowPosition()
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            } else {
                val params = layoutParams as MarginLayoutParams
                params.leftMargin = ((parent as View).width - width) / 2
                params.bottomMargin = 100
                if (params is ConstraintLayout.LayoutParams) {
                    params.topToTop = -1
                    params.bottomToBottom = (parent as View).id
                }
                setLayoutParams(params)
            }
        }
    }

    /**
     * 显示的时候需要再树布局测绘完成后再显示
     */
    override fun setVisibility(visibility: Int) {
        runOnUIThread {
            resetShowPosition()
            super.setVisibility(visibility)
        }
    }

    /**
     * 设置限时体验信息
     */
    fun setExperienceInfo(allowUseConfig: Boolean, rttExperienceDefaultTime: Int, rttExperienceReduceTime: Int) {
        runOnUIThread {
            if (allowUseConfig) {
                binding.agoraFcrRttTextDialogHintLayout.visibility = View.GONE
            } else {
                binding.agoraFcrRttTextDialogHintLayout.visibility = View.VISIBLE
                if (rttExperienceReduceTime <= 0) {
                    binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setText(R.string.fcr_dialog_rtt_time_limit_end)
                    binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.text =
                        MessageFormat.format(resources.getString(R.string.fcr_dialog_rtt_subtitles_dialog_time_limit_end),
                            rttExperienceDefaultTime / 60000)
                } else {
                    binding.fcrOnlineEduRttConversionDialogTimeLimitHint.setText(R.string.fcr_dialog_rtt_time_limit)
                    binding.fcrOnlineEduRttConversionDialogTimeLimitReduce.text = MessageFormat.format(
                        resources.getString(R.string.fcr_dialog_rtt_subtitles_dialog_time_limit_reduce),
                        rttExperienceDefaultTime / 60000,
                        if (rttExperienceReduceTime / 60000 > 9) rttExperienceReduceTime / 60000 else "0${rttExperienceReduceTime / 60000}",
                        if (rttExperienceReduceTime % 60000 / 1000 > 9) rttExperienceReduceTime % 60000 / 1000 else "0${rttExperienceReduceTime % 60000 / 1000}",
                    )
                }
            }
        }
    }

    /**
     * 设置显示状态信息
     * @param showIcon 是否显示图标
     * @param showProgress 是否显示进度圈
     */
    fun setShowStatusInfo(showProgress: Boolean, showIcon: Boolean, text: String) {
        runOnUIThread {
            binding.agoraFcrRttTextDialogLayoutStatus.visibility = View.VISIBLE
            binding.agoraFcrRttTextDialogLayoutText.visibility = View.GONE
            binding.agoraFcrRttTextDialogStatusText.text = text
            binding.agoraFcrRttTextDialogProgress.visibility = if (showProgress) VISIBLE else GONE
            binding.agoraFcrRttTextDialogIcon.visibility = if (showIcon) VISIBLE else GONE
        }
    }

    /**
     * 设置显示翻译信息
     * @param headImage 头像
     * @param name 用户名称
     * @param originText 翻译原文
     * @param resultText 翻译结果
     */
    fun setShowTranslatorsInfo(headImage: String, name: String, originText: String, resultText: String? = null) {
        runOnUIThread {
            binding.agoraFcrRttTextDialogLayoutStatus.visibility = View.GONE
            binding.agoraFcrRttTextDialogLayoutText.visibility = View.VISIBLE
            Glide.with(this).load(headImage).skipMemoryCache(true).placeholder(R.drawable.agora_video_ic_audio_on)
                .apply(RequestOptions.circleCropTransform()).into(binding.agoraFcrRttTextDialogUserHeader)
            binding.agoraFcrRttTextDialogUserHeaderText.text = if (headImage.isEmpty()) "" else headImage.substring(0, 1)
            binding.agoraFcrRttTextDialogUserName.text = name
            binding.agoraFcrRttTextDialogTextOrigin.text = originText
            binding.agoraFcrRttTextDialogTextResult.text = resultText
            binding.agoraFcrRttTextDialogTextResult.visibility = if (resultText.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

}




















