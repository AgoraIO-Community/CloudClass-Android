package io.agora.edu.classroom.widget.video

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.edu.R
import io.agora.edu.classroom.widget.window.AbstractWindow
import io.agora.edu.classroom.widget.window.IMinimizable

class VideoWindow : AbstractWindow {
    private val TAG = "VideoView"
    lateinit var textureContainer: FrameLayout
    lateinit var optionsLayout: ConstraintLayout
    private lateinit var videoImg: AppCompatImageView
    private lateinit var audioView: AudioView
    private lateinit var trophyNumText: AppCompatTextView
    private lateinit var userNameText: AppCompatTextView
    private lateinit var foldImg: AppCompatImageView
    private lateinit var videoOffLayout: LinearLayout
    private lateinit var rootLayout: RelativeLayout
    private lateinit var unfoldLayout: RelativeLayout
    private lateinit var foldLayout: RelativeLayout
    private lateinit var userNameTextFold: AppCompatTextView
    private lateinit var unfoldImg: AppCompatImageView

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

    private fun initView() {
        inflate(context, R.layout.video_window_layout, this)
        (context as Activity).window.setBackgroundDrawableResource(android.R.color.transparent)
        optionsLayout = findViewById(R.id.options_Layout)
        optionsLayout.z = 100f
        textureContainer = findViewById(R.id.textureContainer)
        textureContainer.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.let {
                    if (child is TextureView) {
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
        foldImg = findViewById(R.id.ic_fold)
        videoOffLayout = findViewById(R.id.videoOff_Layout)
        rootLayout = findViewById(R.id.root_Layout)
        unfoldLayout = findViewById(R.id.unfold_Layout)
        foldLayout = findViewById(R.id.fold_Layout)
        foldLayout.visibility = GONE
        userNameTextFold = findViewById(R.id.userNameText_Fold)
        unfoldImg = findViewById(R.id.unfold_Img)
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = rootLayout.right - rootLayout.left
                val layoutParams = foldLayout.layoutParams
                layoutParams.width = width
                foldLayout.layoutParams = layoutParams
            }
        })
        /**设置弹开和折叠的布局*/
        setLayouts(unfoldLayout, foldLayout)
        /**设置动画方向*/
        setMinimizeDirection(IMinimizable.Direction.bottom)
        foldImg.setOnClickListener {
            startMinimize()
        }
        unfoldImg.setOnClickListener {
            Log.e(TAG, "膨胀")
            restoreMinimize()
        }
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
                if (trophy > 99) {
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