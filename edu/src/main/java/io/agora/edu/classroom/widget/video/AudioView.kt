package io.agora.edu.classroom.widget.video

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import io.agora.edu.R

class AudioView : LinearLayout {
    private val TAG = "AudioView"
    private lateinit var volumeLayout: LinearLayout
    private lateinit var audioImg: AppCompatImageView
    private val VOLUMEITEM = 45

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    ) {
        initView()
    }

    constructor(
            context: Context?,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.video_window_audio_view_layout, this)
        volumeLayout = findViewById(R.id.volume_Layout)
        audioImg = findViewById(R.id.ic_audio_Img)
    }

    fun updateVolume(volume: Int) {
        volumeLayout.removeAllViews()
        var volumeLevel = 0
        if (volume != 0) {
            volumeLevel = volume / VOLUMEITEM
            if (volumeLevel < 0) {
                volumeLevel = 1
            } else if (volumeLevel > 4) {
                volumeLevel = 4
            }
        }
        for (i in 1..(4 - volumeLevel)) {
            val volumeIc = AppCompatImageView(context)
            volumeIc.setImageResource(R.drawable.video_window_ic_volume_off)
            val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutParams.topMargin = context.resources.getDimensionPixelSize(R.dimen.video_window_audio_volume_img_top_margin)
            volumeIc.layoutParams = layoutParams
            volumeLayout.addView(volumeIc)
        }
        for (i in 1..volumeLevel) {
            val volumeIc = AppCompatImageView(context)
            volumeIc.setImageResource(R.drawable.video_window_ic_volume_on)
            val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutParams.topMargin = context.resources.getDimensionPixelSize(R.dimen.video_window_audio_volume_img_top_margin)
            volumeIc.layoutParams = layoutParams
            volumeLayout.addView(volumeIc)
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        volumeLayout.visibility = if (selected) VISIBLE else GONE
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        audioImg.isClickable = clickable
    }
}