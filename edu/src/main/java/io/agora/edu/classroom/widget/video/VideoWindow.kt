package io.agora.apaaspoc.video

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.edu.R
import io.agora.edu.classroom.widget.video.VideoViewTextureOutlineProvider

class VideoWindow : RelativeLayout {
    private val TAG = "VideoView"
    lateinit var textureContainer: FrameLayout
    lateinit var optionsLayout: ConstraintLayout
    private lateinit var videoImg: AppCompatImageView
    private lateinit var audioView: AudioView
    private lateinit var trophyNumText: AppCompatTextView
    private lateinit var userNameText: AppCompatTextView
    private lateinit var packupImg: AppCompatImageView
    private lateinit var videoOffLayout: LinearLayout

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.video_window_layout, this)
        (context as Activity).window.setBackgroundDrawableResource(android.R.color.transparent)
        optionsLayout = findViewById(R.id.options_Layout)
        optionsLayout.z = 100f
        textureContainer = findViewById(R.id.textureContainer)
        textureContainer.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.let {
                    if(child is TextureView) {
                        setTextureViewRound(child)
                    }
                }
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
            }
        })
        videoImg = findViewById(R.id.ic_video)
        audioView = findViewById(R.id.ic_audio)
        trophyNumText = findViewById(R.id.trophyNum_Text)
        userNameText = findViewById(R.id.userName)
        packupImg = findViewById(R.id.ic_packup)
        videoOffLayout = findViewById(R.id.videoOff_Layout)
    }

    private fun setTextureViewRound(textureView: TextureView) {
        var radius: Float = context.resources.getDimensionPixelSize(R.dimen.video_window_texture_radius).toFloat()
        val textureOutlineProvider = VideoViewTextureOutlineProvider(radius)
        textureView.outlineProvider = textureOutlineProvider
        textureView.clipToOutline = true
    }

    fun updateTrophy(trophy: Int) {
        trophyNumText?.let {
            post {
                if(trophy > 99) {
                    it.text = "x99+"
                } else {
                    it.text = "x".plus(trophy)
                }
            }
        }
    }

    fun updateAudioVolume(volume: Int) {
        audioView?.let {
            post {
                audioView.updateVolume(volume)
            }
        }
    }
}